package thread.start;

public class DaemonThreadMain {

    public static void main(String[] args) {
        // JVM이 종료되는 시점은 main 스레드가 종료되는것보다 user 스레드가 모두 종료되었을때 비로소 JVM이 종료가된다.
        System.out.println(Thread.currentThread().getName() + ": main() start");

        DaemonThread daemonThread = new DaemonThread();
        daemonThread.setDaemon(true); // true : user 스레드를 기다리지 않고 main 스레드가 종료되면 JVM 종료
        daemonThread.start();

        System.out.println(Thread.currentThread().getName() + ": main() end");
    }

    static class DaemonThread extends Thread {

        @Override
        public void run() {
            super.run();
            System.out.println(Thread.currentThread().getName() +": run() start");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException();
            }
            System.out.println(Thread.currentThread().getName() +": run() end");
        }
    }
}
