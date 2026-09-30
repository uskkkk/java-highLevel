package thread.start.test;

import static thread.util.MyLogger.log;

public class StartTest1Main {


    public static void main(String[] args) {
//        CounterThread counterThread = new CounterThread();
//        counterThread.start();

        CounterRunnable counterRunnable = new CounterRunnable();
        Thread thread = new Thread(counterRunnable);
        thread.start();
    }


    static class CounterThread extends Thread {

        private static int cnt;

        @Override
        public void run() {
            super.run();

            while (cnt < 5) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException();
                }
                cnt++;
                log(cnt);
            }

        }
    }

    static class CounterRunnable implements Runnable {
        @Override
        public void run() {
            for (int i = 1; i <= 5; i++) {
                log("value : " + i);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
