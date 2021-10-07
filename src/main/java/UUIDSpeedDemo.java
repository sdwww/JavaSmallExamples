/**
 * @(#)UUIDSpeedDemo.java, 9月 30, 2021.
 * <p>
 * Copyright 2021 fenbi.com. All rights reserved.
 * FENBI.COM PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

import java.util.UUID;

/**
 * @author wangweiwei
 */
public class UUIDSpeedDemo {

    // uuid生成是微秒级别
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            UUID.randomUUID().toString();
        }
        System.out.println("生成100W UUID 时间：" + (System.currentTimeMillis() - start));
    }
}