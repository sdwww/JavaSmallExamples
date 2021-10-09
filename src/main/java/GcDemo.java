/**
 * @(#)GcDemo.java, 10月 07, 2021.
 * <p>
 * Copyright 2021 fenbi.com. All rights reserved.
 * FENBI.COM PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/**
 * @author wangweiwei
 */
public class GcDemo {

    private static final int ONE_MB = 1024 * 1024;

    // -Xmx30M -Xms30M -XX:+PrintGCDetails -XX:+UseConcMarkSweepGC -XX:+PretenureSizeThreshold=5M -XX:SurvivorRatio=8
    // 堆空间的大小为30M,其中eden区8M,survivor区1M,old区20M
    // 打印gc明细
    // 指定使用CMS
    // 超过5M的大对象直接进入老年代
    // eden区和survivor区比例8：1
    public static void main(String[] args) throws InterruptedException {
        byte[] bytes = allocate16M();
        byte[] b1 = new byte[15 * ONE_MB];
        Thread.sleep(10000);
    }

    private static byte[] allocate16M() {
        byte[] b1 = new byte[4 * ONE_MB];
        byte[] b2 = new byte[4 * ONE_MB];
        byte[] b3 = new byte[4 * ONE_MB];
        byte[] b4 = new byte[4 * ONE_MB];
        return b3;
    }
}