/**
 * @(#)InsertSortDemo.java, 10月 01, 2021.
 * <p>
 * Copyright 2021 fenbi.com. All rights reserved.
 * FENBI.COM PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/**
 * 插入排序
 * @author wangweiwei
 */
public class InsertSortDemo {

    public static void main(String[] args) {
        int[] nums =new int[]{1,2,3,6,5,4};
        InsertSortDemo demo = new InsertSortDemo();
        demo.insertSort(nums);
        for(int num:nums){
            System.out.println(num);
        }
    }

    public void insertSort(int[] nums) {
        for (int i = 1; i < nums.length; i++) {
            // 保存每次需要插入的那个数
            int temp = nums[i];
            int j;
            for (j = i; j > 0 && nums[j - 1] > temp; j--) {
                // 大于需要插入的数往后移动。最后不大于temp的数就空出来j
                nums[j] = nums[j - 1];
            }
            nums[j] = temp;
        }
    }
}