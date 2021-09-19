/**
 * @(#)LocalityDemo.java, 9月 18, 2021.
 * <p>
 * Copyright 2021 fenbi.com. All rights reserved.
 * FENBI.COM PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/**
 * 局部性原理
 * 时间连续性，空间连续性和顺序连续性
 *
 * @author wangweiwei
 */
public class LocalityDemo {

    public static void main(String[] args) {
        int[][] nums1 = new int[5000][5000];
        long startTime1 = System.currentTimeMillis();
        for (int i = 0; i < nums1.length; i++) {
            for (int j = 0; j < nums1[0].length; j++) {
                nums1[i][j] = i + j;
            }
        }
        System.out.println(System.currentTimeMillis() - startTime1);

        int[][] nums2 = new int[5000][5000];
        long startTime2 = System.currentTimeMillis();
        for (int j = 0; j < nums2[0].length; j++) {
            for (int i = 0; i < nums2.length; i++) {
                nums2[i][j] = i + j;
            }
        }
        System.out.println(System.currentTimeMillis() - startTime2);
    }
}