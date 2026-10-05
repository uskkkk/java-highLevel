package thread.control;

import static util.MyLogger.log;

public class ThreadStateMain {
    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(new MyRunnable(), "myThread");
        log("myThread.state1 =" + thread.getState()); //  RUNNABLE
        log("myThread.start()");
        thread.start();
        Thread.sleep(1000);
        log("MyThrea.state2 = " + thread.getState()); //  TIMED_WAITING
    }

    static class MyRunnable implements Runnable {


        @Override
        public void run() {
            try {
                log("start");
                log("MyThrea.state2 = " + Thread.currentThread().getState()); //  RUNNABLE
                log("sleep() start");
                Thread.sleep(3000);
                log("MyThrea.state2 = " + Thread.currentThread().getState()); //  RUNNABLE
                log("sleep() end");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
