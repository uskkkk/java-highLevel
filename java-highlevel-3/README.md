# 4. 스레드 제어와 생명 주기 2

## 목차
- [인터럽트 - 시작 1 (runFlag 방식)](#인터럽트---시작-1-runflag-방식)
- [인터럽트 - 시작 2 (interrupt 방식)](#인터럽트---시작-2-interrupt-방식)
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

## 복습 정리
- 스레드를 외부에서 **강제로 죽이는 방법은 없다**. 스레드 스스로 멈추도록 **신호를 주고, 스레드가 협력**해서 종료해야 한다.
- 공유 flag 방식은 간단하지만, 스레드가 대기 상태일 때는 신호를 받지 못해 반응이 늦다.
- `interrupt()`는 대기 상태(`WAITING`, `TIMED_WAITING`)의 스레드를 깨워 `InterruptedException`을 발생시킨다.
- `InterruptedException`이 발생하면 인터럽트 상태는 `false`로 돌아간다.
- `isInterrupted()`는 인터럽트 상태를 **조회만** 하고 값을 바꾸지 않는다.
- 인터럽트를 받은 뒤에도 `catch`에서 자원 정리 같은 **마무리 작업을 정상적으로 수행**할 수 있다는 점이 큰 장점이다.
