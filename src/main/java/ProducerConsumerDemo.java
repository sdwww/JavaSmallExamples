/**
 * @(#)Main.java, 9月 29, 2021.
 * <p>
 * Copyright 2021 fenbi.com. All rights reserved.
 * FENBI.COM PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * 使用阻塞队列实现生产着消费者模型
 *
 * @author wangweiwei
 */
public class ProducerConsumerDemo {

    public static void main(String[] args) {
        BlockingQueue<Object> queue = new ArrayBlockingQueue<>(10);
        new Thread(new Producer(queue)).start();
        new Thread(new Producer(queue)).start();
        new Thread(new Consumer(queue)).start();
        new Thread(new Consumer(queue)).start();
        new Thread(new Consumer(queue)).start();
    }
}

class Producer implements Runnable {

    private BlockingQueue<Object> queue;

    public Producer(BlockingQueue<Object> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Object o = new Object();
            try {
                queue.put(o);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("放入" + o);
        }
    }
}

class Consumer implements Runnable {

    private BlockingQueue<Object> queue;

    public Consumer(BlockingQueue<Object> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Object poll = null;
            try {
                poll = queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("取出" + poll);
        }
    }
}