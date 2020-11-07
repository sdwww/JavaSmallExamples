import java.util.concurrent.CountDownLatch;

/**
 * CountDownLatch示例
 */
public class CountDownLatchExample {

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(2);
        Thread thread1 = new Thread(countDownLatch::countDown);
        Thread thread2 = new Thread(countDownLatch::countDown);
        thread1.start();
        thread2.start();
        countDownLatch.await();
        System.out.println("end");
    }
}
