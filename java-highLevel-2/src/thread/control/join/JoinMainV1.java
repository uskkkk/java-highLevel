package thread.control.join;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class JoinMainV1 {

    public static void main(String[] args) {
        log("Start");
        SumTask task1 = new SumTask(1,50);
        SumTask task2 = new SumTask(51,100);
        Thread thread1 = new Thread(task1,"Thread-1");
        Thread thread2 = new Thread(task2,"Thread-2");

        thread1.start();
        thread2.start();

//        sleep(3000); task1, task2의 주소값에 있는 result 값을 가져오기전에 main 스택이 먼저 끝나기 때문에 0을 반환해버린다.
        log("task1.result = " + task1.result);
        log("task2.result = " + task2.result);

        int sumAll = task1.result + task2.result;
        log("task1 + task2 = " + sumAll);
        log("End");
    }

    static class SumTask implements Runnable {

        int startValue;
        int endValue;
        int result = 0;

        public SumTask(int startValue, int endValue) {
            this.startValue = startValue;
            this.endValue = endValue;
        }

        @Override
        public void run() {
            log("작업 시작");
            sleep(2000);
            int sum = 0;
            for (int i = startValue; i <= endValue; i++) {
                sum += i;
            }
            result = sum;
            log("작업 완료 result = " + result);
        }
    }
}
