/**
 * @(#)PipedStreamDemo.java, 10月 07, 2021.
 * <p>
 * Copyright 2021 fenbi.com. All rights reserved.
 * FENBI.COM PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;

/**
 * 管道流
 *
 * @author wangweiwei
 */
public class PipedStreamDemo {

    public static void main(String[] args) throws IOException {
        PipedWriter writer = new PipedWriter();
        PipedReader reader = new PipedReader();
        //使用connect方法将输入流和输出流连接起来
        writer.connect(reader);
        Thread printThread = new Thread(new Printer(reader));
        //启动线程printThread
        printThread.start();
        int receive = 0;
        try {
            //读取输入的内容
            while ((receive = System.in.read()) != -1) {
                writer.write(receive);
            }
        } finally {
            writer.close();
        }
    }

    private static class Printer implements Runnable {

        private final PipedReader reader;

        public Printer(PipedReader reader) {
            this.reader = reader;
        }

        @Override
        public void run() {
            int receive = 0;
            try {
                while ((receive = reader.read()) != -1) {
                    //字符转换
                    System.out.print((char) receive);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}