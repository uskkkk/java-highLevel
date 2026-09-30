package thread.start;

public class BadThreadMain {

    public static void main(String[] args) {
        System.out.println(Thread.currentThread().getName() + " : main() start");

        HelloThread helloThread = new HelloThread();
        System.out.println(Thread.currentThread().getName() + ": start()  호출 전");
        helloThread.run();
        System.out.println(Thread.currentThread().getName() + ": start()  호출 후");

        System.out.println(Thread.currentThread().getName() + " : main() end");

//        run 메서드를 실행하면 main 스택에 run() 스택 프레임이 올라간다.
//        Thread의 스택 영역에 올라가지 않으므로 순차적으로 실행된다.
    }
}
