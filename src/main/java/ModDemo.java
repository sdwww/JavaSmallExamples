/**
 * @(#)ModDemo.java, 8月 30, 2021.
 * <p>
 * Copyright 2021 fenbi.com. All rights reserved.
 * FENBI.COM PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/**
 * 取模demo
 *
 * @author wangweiwei
 */
public class ModDemo {

    // i%(2^n) == i&(2^n-1)
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        for (long i = 1; i < 1000000000L; i++) {
            long mod = i % 1024L;
        }
        System.out.println(System.currentTimeMillis() - startTime);

        startTime = System.currentTimeMillis();
        for (long i = 1; i < 1000000000L; i++) {
            long mod = i & 1023L;
        }
        System.out.println(System.currentTimeMillis() - startTime);
    }
}