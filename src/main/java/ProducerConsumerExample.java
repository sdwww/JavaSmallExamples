import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 生产者
 */
class Producer implements Runnable {
    private ReentrantLock lock;
    private Condition full;
    private Condition empty;
    private Queue<Integer> queue;
    private int maxSize;
    private int count;

    Producer(ReentrantLock lock, Condition full, Condition empty, Queue<Integer> queue, int maxSize) {
        this.lock = lock;
        this.queue = queue;
        this.full = full;
        this.empty = empty;
        this.maxSize = maxSize;
    }

    public void run() {
        try {
            while (true) {
                lock.lock();
                while (queue.size() == maxSize) {
                    full.await();
                }
                queue.offer(count);
                System.out.println(Thread.currentThread().toString() + "producer:" + count);
                count++;
                empty.signalAll();
                lock.unlock();
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

/**
 * 消费者
 */
class Consumer implements Runnable {
    private ReentrantLock lock;
    private Condition full;
    private Condition empty;
    private Queue<Integer> queue;

    public Consumer(ReentrantLock lock, Condition full, Condition empty, Queue<Integer> queue) {
        this.lock = lock;
        this.full = full;
        this.empty = empty;
        this.queue = queue;
    }

    public void run() {
        try {
            while (true) {
                lock.lock();
                while (queue.size() == 0) {
                    empty.await();
                }
                int number = queue.poll();
                System.out.println(Thread.currentThread().toString() + "Consumer:" + number);
                full.signalAll();
                lock.unlock();
                Thread.sleep(3000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

/**
 * 生产者消费者模式示例
 */
public class ProducerConsumerExample {
    public static void main(String[] args) throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
        Condition proCondition = lock.newCondition();
        Condition conCondition = lock.newCondition();
        LinkedList<Integer> queue = new LinkedList<>();
        ExecutorService executor = Executors.newFixedThreadPool(4);

        executor.submit(new Producer(lock, conCondition, proCondition, queue, 10));
        executor.submit(new Consumer(lock, conCondition, proCondition, queue));
        executor.submit(new Consumer(lock, conCondition, proCondition, queue));
        executor.shutdown();
    }
}
