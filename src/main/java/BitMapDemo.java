/**
 * @(#)BitMapDemo.java, 9月 23, 2021.
 * <p>
 * Copyright 2021 fenbi.com. All rights reserved.
 * FENBI.COM PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

import java.util.BitSet;

/**
 * bitMap demo
 * @author wangweiwei
 */
public class BitMapDemo {

    public static void main(String[] args) {
        int[] array = new int[]{1, 2, 3, 22, 0, 3};
        BitSet bitSet = new BitSet(100);
        //将数组内容组bitmap
        for (int j : array) {
            bitSet.set(j, true);
        }
        System.out.println(bitSet.size());
        System.out.println(bitSet.get(3));
    }
}