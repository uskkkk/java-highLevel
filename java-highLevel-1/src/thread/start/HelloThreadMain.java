package thread.start;

public class HelloThreadMain {

    public static void main(String[] args) {
        System.out.println(Thread.currentThread().getName() + " : main() start");

        HelloThread helloThread = new HelloThread();
        System.out.println(Thread.currentThread().getName() + ": start()  호출 전");
        helloThread.start();
        System.out.println(Thread.currentThread().getName() + ": start()  호출 후");

        System.out.println(Thread.currentThread().getName() + " : main() end");

//        main 스레드는 HelloThread 스레드에게 명령 후 start 메서드를 빠져나온다.
//        helloThread의 run 메서드를 기다려주지 않는다.

    }
}
