import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 如何捕获线程池异常
 */
public class ThreadPoolException {

    // 线程池返回的是ExecutionException
    public static void main(String[] args) throws Throwable {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            Future<Integer> future = executorService.submit(ThreadPoolException::throwOneException);
            try {
                future.get();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
        executorService.shutdown();
    }

    public static Integer throwOneException() {
        throw new NullPointerException();
    }
}
