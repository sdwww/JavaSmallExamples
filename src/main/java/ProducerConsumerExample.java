import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 生产者消费者模式示例
 */
public class ProducerConsumerExample {
    public static void main(String[] args) {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        BlockingQueue<Integer> blockingQueue = new BlockingQueue<>(10);
        Thread producer0 = new Thread(new Producer(blockingQueue, atomicInteger));
        Thread producer1 = new Thread(new Producer(blockingQueue, atomicInteger));
        Thread consumer = new Thread(new Consumer(blockingQueue));
        producer0.start();
        producer1.start();
        consumer.start();

    }

    private static class Producer implements Runnable {

        private final BlockingQueue<Integer> blockingQueue;

        private final AtomicInteger atomicInteger;

        public Producer(BlockingQueue<Integer> blockingQueue, AtomicInteger atomicInteger) {
            this.blockingQueue = blockingQueue;
            this.atomicInteger = atomicInteger;
        }

        @Override
        public void run() {
            while (true) {
                int value = atomicInteger.incrementAndGet();
                blockingQueue.put(value);
                System.out.println("线程:" + Thread.currentThread().getName() + "生产:" + value);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class Consumer implements Runnable {

        private final BlockingQueue<Integer> blockingQueue;

        public Consumer(BlockingQueue<Integer> blockingQueue) {
            this.blockingQueue = blockingQueue;
        }

        @Override
        public void run() {
            while (true) {
                Object take = blockingQueue.take();
                System.out.println("线程:" + Thread.currentThread().getName() + "消费:" + take);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class BlockingQueue<E> {
        private final Queue<E> queue = new LinkedList<>();
        private final int maxSize;
        private final ReentrantLock reentrantLock = new ReentrantLock();
        private final Condition notFull = reentrantLock.newCondition();
        private final Condition notEmpty = reentrantLock.newCondition();

        public BlockingQueue(int maxSize) {
            this.maxSize = maxSize;
        }

        public E take() {
            E e = null;
            reentrantLock.lock();
            try {
                while (queue.size() == 0) {
                    notEmpty.await();
                }
                e = queue.poll();
                notFull.signal();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            } finally {
                reentrantLock.unlock();
            }
            return e;
        }

        public void put(E e) {
            reentrantLock.lock();
            try {
                while (queue.size() >= maxSize) {
                    notFull.await();
                }
                queue.offer(e);
                notEmpty.signal();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            } finally {
                reentrantLock.unlock();
            }
        }
    }
}
