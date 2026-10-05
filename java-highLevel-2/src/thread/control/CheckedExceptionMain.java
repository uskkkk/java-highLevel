package thread.control;

import static util.ThreadUtils.sleep;

public class CheckedExceptionMain {
    public static void main(String[] args) throws Exception {
        throw new Exception();
    }

    static class CheckedRunnable implements Runnable {


        @Override
        public void run()
//                throws Exception
        {
            // 자바에서 메서드를 재정의 할 때, 재정의 메서드가 지켜야할 예외와 관련된 규칙이 있다.
//            throw new Exception();
            sleep(1000);
        }
    }
}
