package thread.start;

public class HelloThread extends Thread {

    @Override
    public void run() {
        super.run();
        System.out.println(Thread.currentThread().getName() + ": run()");
    }
}
