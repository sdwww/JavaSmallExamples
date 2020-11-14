import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CyclicBarrier示例
 */
public class CyclicBarrierExample {

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CyclicBarrier cyclicBarrier = new CyclicBarrier(2, () -> System.out.println("merge"));
        for (int i = 0; i < 2; i++) {
            executorService.submit(() -> {
                try {
                    System.out.println(Thread.currentThread() + "step1");
                    cyclicBarrier.await();
                    System.out.println(Thread.currentThread() + "step2");
                    cyclicBarrier.await();
                    System.out.println(Thread.currentThread() + "step3");
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
            });
        }
        executorService.shutdown();
    }
}
