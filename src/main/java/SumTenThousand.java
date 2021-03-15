import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 多线程计算  1+2+3+4+5+......+n 的值
 *
 * @author ZeWe
 */
public class SumTenThousand {

    private static Integer sum = 0;

    private static CountDownLatch latch;
    private static Lock lock;

    public static void main(String[] args) throws InterruptedException {

        lock = new ReentrantLock();
        //线程数量
        int latchSize = 10;
        latch = new CountDownLatch(latchSize);

        int n = 10000;
        int avg = n / latchSize;
        int rem = n % latchSize;
        int left, right;
        for (int i = 1; i <= latchSize; i++) {
            left = (i - 1) * avg + 1;
            right = i == latchSize ? (i * avg + rem) : (i * avg);
            new Thread(new Run(left, right)).start();
        }

        latch.await(); // 等待10个进程完全结束，在进行主线程

        System.out.println("sum:" + sum);

    }


    static class Run implements Runnable {
        private int left;
        private int right;

        public Run(int left, int right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public void run() {
            int res = 0;
            for (int i = left; i <= right; i++) {
                res += i;
            }

            lock.lock(); // static sum 并发相加 加锁
            try {
                sum += res;
                latch.countDown(); // 完成一个 减去一个
            } finally {
                lock.unlock();
            }

        }

    }

}
