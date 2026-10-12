# 4. 스레드 제어와 생명 주기 2

## 목차
- [인터럽트 - 시작 1 (runFlag 방식)](#인터럽트---시작-1-runflag-방식)
- [인터럽트 - 시작 2 (interrupt 방식)](#인터럽트---시작-2-interrupt-방식)
- [인터럽트 - 시작 3, 4 (인터럽트 상태 직접 확인)](#인터럽트---시작-3-4-인터럽트-상태-직접-확인)
- [프린터 예제 (인터럽트 실전 적용)](#프린터-예제-인터럽트-실전-적용)
- [yield - 양보하기](#yield---양보하기)
- [복습 정리](#복습-정리)

---

## 인터럽트 - 시작 1 (runFlag 방식)
- 실행 중인 스레드에게 "이제 그만 멈춰"라고 알리는 가장 단순한 방법은 **공유 변수(flag)** 를 두는 것이다.
- 예제 : `thread.control.interrupt.ThreadStopMainV1`

```java
static class MyTask implements Runnable {
    volatile boolean runFlag = true;

    @Override
    public void run() {
        while (runFlag) {      // 매 반복마다 flag 확인
            log("작업 중");
            sleep(3000);       // 작업에 3초가 걸린다고 가정
        }
        log("자원 정리");
        log("자원 종료");
    }
}
```

[흐름]
- `main` 스레드는 `work` 스레드를 시작하고 4초 후 `runFlag = false`로 작업 중단을 지시한다.
- `work` 스레드는 `while (runFlag)` 조건에서 `false`를 확인하면 반복문을 빠져나와 자원을 정리하고 종료한다.

[보충] `volatile`
- 여러 스레드가 함께 읽고 쓰는 값에 붙이는 키워드. `main`이 바꾼 값을 `work`가 바로 볼 수 있게 해준다.
- 자세한 원리(메모리 가시성)는 이후 챕터에서 다룬다. 지금은 "공유 flag에는 `volatile`" 정도로 기억.

[결과] 중단 지시 후 약 **2초 뒤**에야 종료된다
```text
14:58:27.520 [     work] 작업 중                      // 0초
14:58:30.525 [     work] 작업 중                      // 3초
14:58:31.510 [     main] 작업 중단 지시 runFlag=false  // 4초
14:58:33.532 [     work] 자원 정리                     // 6초 ← 2초 지연
14:58:33.533 [     work] 자원 종료
```

[문제점] 반응이 느리다 — 원인은 `sleep()`
```text
시간   0s ─────────── 3s ─────────── 4s ─────── 6s
work   [  sleep 3초  ] [      sleep 3초       ] → while(runFlag) 확인 → 종료
main                                  ↑ runFlag=false
```
- `work` 스레드는 `sleep(3000)`으로 3초간 잠들어 있는 동안 flag가 바뀐 것을 **알 방법이 없다**.
- 잠에서 깨어나 `while (runFlag)`를 다시 실행해야 비로소 `false`를 확인하고 작업을 중단한다.
- 왜 하필 2초일까?
  - `work`는 3초마다 깨어나 flag를 확인하는데, `main`은 4초에 flag를 바꿨다.
  - `work` 입장에서는 두 번째 `sleep()`에 들어간 지 1초 만에 flag가 바뀐 것이고, 깨어나려면 아직 2초가 남아 있다.
- 만약 sleep이 3초가 아니라 1시간이었다면? 중단 지시 후 최대 1시간을 기다려야 한다.

> 핵심 질문 : `sleep()`처럼 **대기 중인 스레드를 바로 깨워서** 종료시킬 수는 없을까? → **인터럽트**

---

## 인터럽트 - 시작 2 (interrupt 방식)
- `thread.interrupt()`를 호출하면 `WAITING`, `TIMED_WAITING` 같은 **대기 상태의 스레드를 직접 깨워** `RUNNABLE` 상태로 만들 수 있다.
- 예제 : `thread.control.interrupt.ThreadStopMainV2`

```java
// main
thread.interrupt();
log("work 스레드 인터럽트 상태1 = " + thread.isInterrupted());

// work
try {
    while (true) {
        log("작업 중");
        Thread.sleep(3000);   // 인터럽트를 감지하는 지점
    }
} catch (InterruptedException e) {
    log("work 스레드 인터럽트 상태2 = " + Thread.currentThread().isInterrupted());
    log("interrupt message = " + e.getMessage());
    log("state = " + Thread.currentThread().getState());
}
log("자원 정리");
log("자원 종료");
```

[보충] 왜 `ThreadUtils.sleep()` 대신 `Thread.sleep()`을 썼나?
- 직접 만든 `ThreadUtils.sleep()`은 내부에서 `InterruptedException`을 잡아 `RuntimeException`으로 바꿔 던진다.
- 인터럽트를 `catch`해서 **정상 흐름(자원 정리)으로 이어가려면** 체크 예외인 `InterruptedException`을 직접 받아야 한다.

[결과] 중단 지시 후 **거의 즉시** 종료된다
```text
[     main] 작업 중단 지시 thread.interrupt()
[     main] work 스레드 인터럽트 상태1 = true
[     work] work 스레드 인터럽트 상태2 = false
[     work] interrupt message = sleep interrupted
[     work] state = RUNNABLE
[     work] 자원 정리
[     work] 자원 종료
```

[동작 과정]
1. `main`이 `thread.interrupt()` 호출 → `work` 스레드의 **인터럽트 상태가 `true`** 가 된다. (상태1 = `true`)
2. `work`는 `Thread.sleep()`으로 `TIMED_WAITING` 중이었으므로, 인터럽트를 받고 깨어나 `RUNNABLE`이 된다.
3. 동시에 `sleep()`에서 `InterruptedException`이 발생 → `catch` 블록으로 이동하며 반복문을 탈출한다.
4. 예외가 던져지는 순간 **인터럽트 상태는 다시 `false`로 초기화**된다. (상태2 = `false`)
5. `catch` 이후 자원 정리 → 종료.

```text
TIMED_WAITING ──interrupt()──▶ RUNNABLE ──▶ InterruptedException 발생 ──▶ catch
 (sleep 중)                    (깨어남)     (인터럽트 상태 true → false)
```

[주의] `interrupt()`를 호출한다고 **즉시** 예외가 터지는 것은 아니다
- `InterruptedException`은 `sleep()`, `join()`, `wait()`처럼 **이 예외를 던지는 메서드를 호출하거나, 그 안에서 대기 중일 때만** 발생한다.
- 위 코드에서 `while (true)`나 `log("작업 중")`을 실행하는 도중에는 예외가 발생하지 않는다.
  - 인터럽트 상태만 `true`로 표시해두고, 다음 `Thread.sleep()` 호출 시점에 예외가 터진다.

[보충] 왜 `catch` 안의 상태가 `RUNNABLE`인가?
- `catch` 블록의 코드도 결국 CPU에서 실행되는 코드다. 실행되려면 스레드가 `RUNNABLE` 상태여야 한다.
- 즉, 인터럽트는 "예외를 던진다"기보다 **"대기 중인 스레드를 깨워서 다시 일하게 만든다"** 고 이해하는 게 정확하다.

[runFlag 방식 vs interrupt 방식]
| 구분 | runFlag (V1) | interrupt (V2) |
|---|---|---|
| 중단 신호 | `volatile boolean` 공유 변수 | `thread.interrupt()` |
| 대기 중 스레드 | 깨울 수 없음 → 대기 끝날 때까지 기다림 | 즉시 깨움 (`InterruptedException`) |
| 반응 속도 | 느림 (예제에서 약 2초 지연) | 빠름 (거의 즉시) |
| 종료 처리 위치 | `while` 조건 검사 | `catch (InterruptedException)` |

[남은 문제] — 다음 단계(V3)에서 다룰 내용
- `while (true)` 부분은 인터럽트 상태를 **확인하지 않는다**.
- `sleep()` 같은 메서드를 만나야만 인터럽트가 반영되므로, 대기 메서드 없이 계속 도는 코드라면 여전히 멈추지 않는다.
- → 반복 조건에서 인터럽트 상태를 직접 체크하는 방법이 필요하다.

---

## 인터럽트 - 시작 3, 4 (인터럽트 상태 직접 확인)
- `sleep()` 같은 대기 메서드가 없어도 멈출 수 있도록, 반복 조건에서 **인터럽트 상태를 직접 확인**한다.
- 예제 : `ThreadStopMainV3`, `ThreadStopMainV4`

| | V3 | V4 |
|---|---|---|
| 반복 조건 | `while (!Thread.currentThread().isInterrupted())` | `while (!Thread.interrupted())` |
| 상태 변경 | 조회만 함 → `true` 유지 | 조회 후 `false`로 **초기화** |
| 상태2 결과 | `true` | `false` |

[V3의 문제] 인터럽트 상태가 `true`로 남아 있다
- 반복문을 빠져나온 뒤 자원 정리 중에 `Thread.sleep()` 등을 호출하면 **곧바로 `InterruptedException`이 발생**한다.
- 이미 목적을 달성한 인터럽트가 엉뚱한 곳에서 다시 터지는 셈이다.

[V4의 해결] `Thread.interrupted()`
- 인터럽트 상태면 `true`를 반환하고 **상태를 `false`로 되돌린다**. 아니면 `false`를 반환하고 상태는 그대로.
- 인터럽트 예외가 발생할 때처럼 상태가 정상으로 돌아오므로, 이후 코드가 안전하게 실행된다.

[보충] `thread.stop()`은 쓰지 않는다
- 자원 정리할 기회도 없이 예측할 수 없는 시점에 스레드를 종료시키기 때문에 deprecated 되었다.
- 스레드 종료는 인터럽트로 **신호를 주고, 스레드가 스스로 정리하고 끝나도록** 하는 것이 정석이다.

---

## 프린터 예제 (인터럽트 실전 적용)
- 사용자가 입력한 문서를 `printer` 스레드가 큐에서 꺼내 출력(3초 소요)하는 예제. `q` 입력 시 종료.
- 예제 : `thread.control.printer.MyPrinterV1 ~ V4`
- 큐는 여러 스레드가 동시에 접근하므로 `ConcurrentLinkedQueue`를 사용한다.

| 버전 | 종료 방식 | 개선 포인트 |
|---|---|---|
| V1 | `volatile boolean work = false` | 출력 중(`sleep`)이면 끝날 때까지 기다려야 함 → 반응 느림 |
| V2 | `work = false` + `thread.interrupt()` | `sleep` 중에도 바로 깨어나 종료 → 반응 빠름 |
| V3 | `thread.interrupt()` + `while (!Thread.interrupted())` | `work` 플래그 제거. 인터럽트 하나로 종료 처리 통일 |
| V4 | V3 + 빈 큐일 때 `Thread.yield()` | 무의미한 바쁜 대기(busy-wait)의 CPU 낭비 완화 |

[V3의 남은 문제] 큐가 비어 있으면 `while` + `isEmpty()` 검사만 쉬지 않고 반복한다.
- CPU 입장에서는 할 일이 없는데도 계속 실행 스케줄링을 받아 자원을 낭비한다.
- → V4에서 `Thread.yield()`로 다른 스레드에게 실행 기회를 양보한다.

---

## yield - 양보하기
- `Thread.yield()` : 현재 스레드가 CPU를 **다른 스레드에게 양보**하겠다고 스케줄러에게 알리는 힌트.
- 예제 : `thread.yield.YieldMain` (스레드 1000개가 각각 0~9 출력)

[실행 방식 비교]
| 방식 | 동작 | 상태 |
|---|---|---|
| empty | 양보 없이 계속 실행. 한 스레드가 몰아서 출력되는 경향 | `RUNNABLE` |
| `sleep(1)` | 1ms 동안 실행 스케줄링에서 제외 → 다시 돌아오는 비용 발생 | `RUNNABLE` → `TIMED_WAITING` → `RUNNABLE` |
| `yield()` | 실행 대기열로 돌아가 다른 스레드에게 기회를 줌 | `RUNNABLE` 유지 |

[핵심]
- 자바의 `RUNNABLE`은 OS 관점의 **실행 중(Running)** 과 **실행 대기(Ready)** 를 모두 포함한다.
- `yield()`는 Running → Ready로 이동할 뿐, 대기 상태로 가지 않는다.
- 양보할 다른 스레드가 없으면 본인이 **곧바로 다시 실행**될 수 있다.
- 어디까지나 **힌트**라서 실제 동작은 OS 스케줄러에 따라 달라진다.

[sleep vs yield]
- `sleep()`은 상태 변화 + 정해진 시간 동안 무조건 쉰다 → 양보할 스레드가 없어도 쉬게 됨.
- `yield()`는 상태 변화 없이 "필요하면 넘겨줘" 정도의 가벼운 양보 → 프린터 V4처럼 바쁜 대기 루프에 적합.

---

## 복습 정리
- 스레드를 안전하게 멈추려면 외부에서 강제로 죽이지 말고(`stop()` 금지), **신호를 주고 스레드가 협력**해서 종료해야 한다.
- 공유 flag 방식은 간단하지만, 스레드가 대기 상태일 때는 신호를 받지 못해 반응이 늦다.
- `interrupt()`는 대기 상태(`WAITING`, `TIMED_WAITING`)의 스레드를 깨워 `InterruptedException`을 발생시킨다.
- `InterruptedException`이 발생하면 인터럽트 상태는 `false`로 돌아간다.
- `isInterrupted()`는 상태를 **조회만**, `Thread.interrupted()`는 조회 후 **`false`로 초기화**한다.
- 반복 조건에서 인터럽트 상태를 직접 확인할 땐 `Thread.interrupted()`를 사용하는 것이 안전하다.
- 인터럽트를 받은 뒤에도 자원 정리 같은 **마무리 작업을 정상적으로 수행**할 수 있다는 점이 큰 장점이다.
- `Thread.yield()`는 `RUNNABLE`을 유지한 채 CPU를 양보하는 힌트로, 바쁜 대기 루프의 CPU 낭비를 줄일 때 유용하다.
