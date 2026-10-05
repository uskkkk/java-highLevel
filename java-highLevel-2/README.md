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

[보충] RUNNABLE = "실행 중"만은 아니다
- RUNNABLE은 CPU에서 **실제로 실행 중인 경우**와 **스케줄링 큐에서 실행을 기다리는 경우**를 모두 포함한다. 자바는 이 둘을 구분하지 않는다.
- 앞서 정리한 스케줄링 그림에서 CPU 코어에서 실행 중인 스레드와 큐에서 대기 중인 스레드 모두 자바 입장에서는 RUNNABLE이다.
- 코어 수보다 스레드가 많으면 대부분의 RUNNABLE 스레드는 사실 자기 차례를 기다리는 중이다.

[보충] BLOCKED / WAITING / TIMED_WAITING은 RUNNABLE의 하위 상태가 아니다
- `getState()`가 반환하는 별도의 독립된 상태이다.
- 다만 흐름상 RUNNABLE에서 이 상태들로 넘어갔다가, 락을 얻거나 / 신호를 받거나 / 시간이 지나면 다시 RUNNABLE로 **돌아온다**.
- 이 상태의 스레드는 CPU를 사용하지 않으므로 스케줄링 큐에 들어가지 않는다. (I/O 바운드 작업에서 "스스로 CPU를 반납한다"는 것과 같은 맥락)

[보충] TERMINATED에서는 되돌아갈 수 없다
- 종료된 스레드에 다시 `start()`를 호출하면 `IllegalThreadStateException`이 발생한다.
- NEW 상태가 아닌 스레드에 `start()`를 두 번 호출해도 같은 예외가 발생한다. 즉 `start()`는 스레드당 한 번만 호출할 수 있다.
