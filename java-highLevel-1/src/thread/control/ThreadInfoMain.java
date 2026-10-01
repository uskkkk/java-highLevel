package thread.control;

import thread.start.HelloRunnable;

import static util.MyLogger.log;

public class ThreadInfoMain {

    public static void main(String[] args) throws InterruptedException {
        Thread mainThread = Thread.currentThread(); // 현재 쓰레드 생성 (main)
        log("mainThread =" + mainThread);
        log("mainThread.threadId() = " + mainThread.threadId());
        log("mainThread.getName = " + mainThread.getName());
        log("mainThread.getPriority() = " + mainThread.getPriority()); // 스레드의 우선순위를 결정하지만 운영체제에 따라 최적화를 다르게 하기 때문에 우선순위가 꼭 따라가진 않는다.
        log("mainThread.getThreadGroup() = " + mainThread.getThreadGroup());
        log("mainThread.getState() = " + mainThread.getState());


        Thread myThread = new Thread(new HelloRunnable(), "myThread");
        log("myThread =" + myThread);
        log("myThread.threadId() = " + myThread.threadId());
        log("myThread.getName = " + myThread.getName());
        log("myThread.getPriority() = " + myThread.getPriority()); // 스레드의 우선순위를 결정하지만 운영체제에 따라 최적화를 다르게 하기 때문에 우선순위가 꼭 따라가진 않는다.
        log("myThread.getThreadGroup() = " + myThread.getThreadGroup());
        log("myThread.getState() = " + myThread.getState());
        myThread.start();
        log("myThread.getState() = " + myThread.getState());
    }
}
