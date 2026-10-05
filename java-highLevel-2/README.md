# 3. 스레드 제어와 생명 주기 1

## 스레드 기본 정보
- `Thread` 클래스는 스레드를 생성하고 관리하는 기능을 제공한다.
- 예제 : `thread.control.ThreadInfoMain`

```java
Thread myThread = new Thread(new HelloRunnable(), "myThread");
```

[스레드 생성]
- **Runnable 인터페이스** : 실행할 작업을 포함하는 인터페이스. `HelloRunnable`이 `Runnable`을 구현한 클래스다.
- **스레드 이름** : `"myThread"`를 이름으로 지정. 이름을 생략하면 `Thread-0`, `Thread-1`처럼 임의의 이름이 붙는다.

[스레드 정보 조회 메서드]
| 메서드 | 설명 | 예시 결과 |
|---|---|---|
| `toString()` | ID, 이름, 우선순위, 스레드 그룹을 포함한 문자열 | `Thread[#21,myThread,5,main]` |
| `threadId()` | 스레드의 고유 식별자. JVM 내에서 각 스레드마다 유일하며 직접 지정할 수 없다 | `21` |
| `getName()` | 스레드 이름. ID는 중복되지 않지만 이름은 중복될 수 있다 | `myThread` |
| `getPriority()` | 우선순위. 1(가장 낮음) ~ 10(가장 높음), 기본값 5 | `5` |
| `getThreadGroup()` | 스레드가 속한 그룹 | `java.lang.ThreadGroup[name=main,maxpri=10]` |
| `getState()` | 현재 상태 (`Thread.State` 열거형) | `NEW` |

[보충] 우선순위
- `setPriority()`로 변경할 수 있지만, 우선순위는 스케줄러에게 주는 **힌트**일 뿐이다.
- 실제 실행 순서는 JVM 구현과 운영체제에 따라 달라질 수 있다.

[보충] 스레드 그룹과 부모 스레드
- 스레드 그룹은 여러 스레드를 묶어서 일괄 종료, 우선순위 설정 등을 할 수 있는 기능이다.
- 새로 생성된 스레드는 자신을 생성한 스레드(**부모 스레드**)의 그룹에 속한다.
  - `myThread`는 `main` 스레드가 생성했으므로 `main` 스레드 그룹에 속한다.
- 실무에서 스레드 그룹을 직접 쓸 일은 거의 없다.

[결과] `main` 스레드는 실행 중이므로 `RUNNABLE`, `myThread`는 아직 `start()` 전이므로 `NEW`
```text
[     main] mainThread.getState() = RUNNABLE
[     main] myThread.getState() = NEW
```

---

## 스레드의 생명 주기
- 스레드는 생성 → 실행 → 종료까지 여러 상태를 거치며, 자바에서는 `Thread.State` 열거형으로 6가지 상태를 정의한다.
- 현재 상태는 `thread.getState()`로 확인할 수 있다.

[스레드의 상태]
- **NEW (새로운 상태)** : 스레드가 생성되었으나 아직 시작되지 않은 상태
  - ex) `new Thread(runnable)` 직후, `start()` 호출 전
- **RUNNABLE (실행 가능 상태)** : `start()` 호출 후 스레드가 실행 중이거나 실행될 준비가 된 상태
- **BLOCKED (차단 상태)** : `synchronized` 블록에 진입하기 위해 락(모니터)을 기다리는 상태
- **WAITING (대기 상태)** : 시간 제한 없이 다른 스레드의 작업(신호)을 기다리는 상태
  - ex) `wait()`, `join()`, `LockSupport.park()`
- **TIMED_WAITING (시간 제한 대기 상태)** : 일정 시간 동안 다른 스레드의 작업을 기다리는 상태
  - ex) `sleep(ms)`, `wait(ms)`, `join(ms)`
- **TERMINATED (종료 상태)** : `run()` 메서드 실행이 끝난 상태 (정상 종료 또는 예외로 인한 종료 모두 포함)

[그림] 스레드 상태 전이
```text
NEW ──start()──▶ RUNNABLE ──run() 종료──▶ TERMINATED
                  ▲   │
                  │   ▼
       BLOCKED / WAITING / TIMED_WAITING
```

[상태 전이 과정]
1. **NEW → RUNNABLE** : `start()` 호출
2. **RUNNABLE → BLOCKED / WAITING / TIMED_WAITING** : 락을 얻지 못하거나, `wait()`, `sleep()`, `join()` 등을 호출
3. **BLOCKED / WAITING / TIMED_WAITING → RUNNABLE** : 락을 얻거나, 기다리던 신호를 받거나, 시간이 지남
4. **RUNNABLE → TERMINATED** : `run()` 메서드 완료

[보충] RUNNABLE = "실행 중"만은 아니다
- RUNNABLE은 CPU에서 **실제로 실행 중인 경우**와 **스케줄링 큐에서 실행을 기다리는 경우**를 모두 포함한다. 자바는 이 둘을 구분하지 않는다.
- 앞서 정리한 스케줄링 그림에서 CPU 코어에서 실행 중인 스레드와 큐에서 대기 중인 스레드 모두 자바 입장에서는 RUNNABLE이다.
- 코어 수보다 스레드가 많으면 대부분의 RUNNABLE 스레드는 사실 자기 차례를 기다리는 중이다.

[보충] BLOCKED / WAITING / TIMED_WAITING은 RUNNABLE의 하위 상태가 아니다
- `getState()`가 반환하는 별도의 독립된 상태이다.
- 다만 흐름상 RUNNABLE에서 이 상태들로 넘어갔다가, 락을 얻거나 / 신호를 받거나 / 시간이 지나면 다시 RUNNABLE로 **돌아온다**.
- 이 상태의 스레드는 CPU를 사용하지 않으므로 스케줄링 큐에 들어가지 않는다. (I/O 바운드 작업에서 "스스로 CPU를 반납한다"는 것과 같은 맥락)
- 이 세 가지를 묶어 **일시 중지 상태(Suspended States)** 라고 부르기도 한다. (자바 공식 상태 이름은 아님)

[보충] TERMINATED에서는 되돌아갈 수 없다
- 종료된 스레드에 다시 `start()`를 호출하면 `IllegalThreadStateException`이 발생한다.
- NEW 상태가 아닌 스레드에 `start()`를 두 번 호출해도 같은 예외가 발생한다. 즉 `start()`는 스레드당 한 번만 호출할 수 있다.

### 생명 주기 확인 코드
- 예제 : `thread.control.ThreadStateMain`
- `Thread.currentThread()` : 해당 코드를 실행하는 스레드 객체를 조회
- `Thread.sleep(ms)` : 지정한 시간 동안 스레드를 **TIMED_WAITING** 상태로 만든다

```text
[     main] myThread.state1 = NEW             // start() 호출 전
[     main] myThread.start()
[ myThread] start
[ myThread] myThread.state2 = RUNNABLE        // run() 실행 중 (자기 자신이 조회)
[ myThread] sleep() start
[     main] myThread.state3 = TIMED_WAITING   // sleep(3000) 중, main이 1초 후 조회
[ myThread] sleep() end
[ myThread] myThread.state4 = RUNNABLE        // sleep 종료 후 다시 RUNNABLE
[ myThread] end
[     main] myThread.state5 = TERMINATED      // run() 종료
[     main] end
```

---

## 체크 예외 재정의
- `Runnable.run()`은 **체크 예외를 밖으로 던질 수 없다.** 그래서 `run()` 안에서 `Thread.sleep()`을 호출하면 반드시 `try-catch`로 잡아야 한다.
- 예제 : `thread.control.CheckedExceptionMain`

```java
public interface Runnable {
    void run(); // throws 선언이 없다
}
```

[메서드 재정의 시 예외 규칙]
- **체크 예외**
  - 부모 메서드가 체크 예외를 던지지 않으면, 재정의된 자식 메서드도 체크 예외를 던질 수 없다.
  - 자식 메서드는 부모 메서드가 던지는 체크 예외의 **하위 타입만** 던질 수 있다.
- **언체크(런타임) 예외** : 예외 처리를 강제하지 않으므로 상관없이 던질 수 있다.

[왜 이런 규칙이 있을까?]
```java
class Parent {
    void method() throws InterruptedException { }
}
class Child extends Parent {
    @Override
    void method() throws Exception { } // 만약 허용된다면?
}

Parent p = new Child();
try {
    p.method();
} catch (InterruptedException e) {
    // Parent 기준으로는 InterruptedException만 잡으면 되지만,
    // 실제로는 Child가 Exception을 던져 처리되지 않는 예외가 생긴다
}
```
- 자식이 더 넓은 범위의 체크 예외를 던지면 **부모 타입으로 다루는 클라이언트 코드가 예외를 놓치게 된다.** → 체크 예외 강제가 깨진다.
- 자바 컴파일러는 이를 막기 위해 재정의 메서드의 체크 예외 범위를 제한한다.

[안전한 예외 처리]
- `run()`에서 체크 예외를 던질 수 없으므로 개발자가 반드시 `try-catch`로 처리하게 된다.
- 예외가 처리되지 않아 스레드가 비정상 종료되는 상황을 줄이는 효과가 있다.
- (참고) 최근에는 체크 예외보다 언체크 예외를 선호하는 추세다.

### ThreadUtils.sleep()
- 매번 `try-catch`를 쓰는 것이 번거로우므로 유틸리티로 분리한다. (`util.ThreadUtils`)

```java
public abstract class ThreadUtils {
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            log("인터럽트 발생" + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
```

```java
import static util.ThreadUtils.sleep;

public void run() {
    sleep(3000); // try-catch 불필요
}
```

---

## join
- `Thread.sleep()`은 **TIMED_WAITING**, `join()`은 **WAITING** 상태를 만든다.
- **WAITING** : 스레드가 다른 스레드의 특정 작업이 완료되기를 **무기한** 기다리는 상태

### join - 시작 (`JoinMainV0`)
```text
[     main] Start
[     main] End
[ thread-2] 작업 시작
[ thread-1] 작업 시작
[ thread-1] 작업 완료
[ thread-2] 작업 완료
```
- `main` 스레드는 `thread-1`, `thread-2`를 `start()`만 하고 **기다리지 않고** 바로 `End`를 출력한다.
- 그렇다면 `main`이 다른 스레드들의 작업이 끝날 때까지 기다리게 하려면 어떻게 해야 할까?

### join - 필요한 상황 (`JoinMainV1`)
- 1 ~ 100의 합을 두 스레드에 나눠서 계산한다.
  - `thread-1` : 1 ~ 50 → 1275
  - `thread-2` : 51 ~ 100 → 3775
  - `main` : 두 결과를 합산 → 5050

```text
[     main] Start
[ thread-1] 작업 시작
[ thread-2] 작업 시작
[     main] task1.result = 0
[     main] task2.result = 0
[     main] task1 + task2 = 0     // 기대값 5050이 아님!
[     main] End
[ thread-1] 작업 완료 result=1275
[ thread-2] 작업 완료 result=3775
```
- `main`은 `start()` 직후 바로 `result`를 읽는다. 이때 두 스레드는 아직 계산 중(2초 sleep)이므로 `result`는 초기값 **0**이다.
- `main`은 두 스레드가 **끝날 때까지 기다린 후** 결과를 읽어야 한다.

[보충] `this`와 스레드
- 각 스레드는 자신의 스택 프레임에서 `run()`을 실행하며, `run()` 안의 `this`는 그 `Runnable` 인스턴스를 가리킨다.
  - `thread-1`의 `this` → `task1`(x001), `thread-2`의 `this` → `task2`(x002)
- 그래서 같은 `SumTask` 코드를 실행해도 각자 다른 인스턴스의 `result`에 값을 저장한다.
- `main`은 `task1.result`, `task2.result`로 이 인스턴스들의 필드를 참조해 결과를 읽는다.

### join - sleep 사용 (`JoinMainV2`)
- `main`에서 `sleep(3000)`으로 충분히 기다린 뒤 결과를 읽으면 `5050`이 나온다.
- 하지만 **문제점**
  - 작업이 언제 끝날지 정확히 알 수 없다. 너무 짧으면 결과를 못 받고, 너무 길면 시간을 낭비한다.
- 상태를 계속 확인하는 방법도 있지만 CPU를 낭비한다.
```java
while (thread.getState() != TERMINATED) {
    // 스레드 상태가 종료될 때까지 계속 반복 → CPU 낭비
}
```
- → `join()`을 사용하면 깔끔하게 해결된다.

### join - join 사용 (`JoinMainV3`)
```java
thread1.start();
thread2.start();

thread1.join(); // thread-1이 종료될 때까지 main 대기
thread2.join(); // thread-2가 종료될 때까지 main 대기
```
```text
[     main] join() - main 스레드가 thread1, thread2 종료까지 대기
[ thread-2] 작업 완료 result = 3775
[ thread-1] 작업 완료 result = 1275
[     main] main 스레드 대기 완료
[     main] task1 + task2 = 5050
```
- `join()`을 호출한 스레드(`main`)는 대상 스레드가 **TERMINATED** 될 때까지 **WAITING** 상태로 대기한다.
- 대상 스레드가 종료되면 호출한 스레드는 다시 **RUNNABLE**이 되어 다음 코드를 수행한다.
- 이미 종료된 스레드에 `join()`을 호출하면 기다리지 않고 바로 빠져나온다.
  - `thread-1`, `thread-2`가 거의 동시에 끝나므로 `thread2.join()`은 사실상 바로 반환된다.
- `join()`은 `InterruptedException`(체크 예외)을 던지므로 `main`에 `throws InterruptedException`을 선언했다.

### join - 특정 시간 만큼만 대기 (`JoinMainV4`)
- `join()` : 대상 스레드가 끝날 때까지 **무기한** 대기 → **WAITING**
- `join(ms)` : 지정한 시간만큼만 대기 → **TIMED_WAITING**. 시간이 지나면 대상 스레드가 안 끝났어도 빠져나와 **RUNNABLE**이 된다.

```text
[     main] join(1000) - main 스레드가 thread1 종료까지 1초 대기
[ thread-1] 작업 시작
[     main] main 스레드 대기 종료          // 1초 후 빠져나옴
[     main] task1.result = 0              // thread-1은 아직 2초 작업 중
[ thread-1] 작업 완료 result = 1275
```
- 다른 스레드가 작업을 완료할 때까지 반드시 기다려야 하면 `join()`, 일정 시간만 기다리고 포기해도 되면 `join(ms)`를 사용한다.

---

## 문제와 풀이

### 문제 1 - join() 활용 1 (`JoinTestMain`)
```java
t1.start();
t1.join();
t2.start();
t2.join();
t3.start();
t3.join();
```
- 각 스레드는 1초 간격으로 1 ~ 3을 출력(총 3초)한다.
- 시작 직후 바로 `join()` 하므로 t1 → t2 → t3가 **순차적으로** 실행된다.
- **총 실행 시간 : 약 9초**

### 문제 2 - join() 활용 2 (`JoinTestMain2`)
- 문제 1의 코드를 **3초**에 끝나도록 변경하기
```java
t1.start();
t2.start();
t3.start();

t1.join();
t2.join();
t3.join();
```
- 세 스레드를 먼저 모두 `start()`해서 **동시에** 실행한 뒤, 마지막에 `join()`으로 모두 끝날 때까지 기다린다.
- **총 실행 시간 : 약 3초**

[정리]
- `start()` 직후 `join()` → 순차 실행 (작업 시간의 합)
- 모두 `start()` 후 한꺼번에 `join()` → 병렬 실행 (가장 긴 작업 시간)
