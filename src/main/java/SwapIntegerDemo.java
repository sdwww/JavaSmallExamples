/**
 * @(#)SwapIntegerDemo.java, 10月 28, 2021.
 * <p>
 * Copyright 2021 fenbi.com. All rights reserved.
 * FENBI.COM PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

import java.lang.reflect.Field;

/**
 * 交换integer
 *
 * @author wangweiwei
 */
public class SwapIntegerDemo {

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        Integer a = 2;
        Integer b = 3;
        SwapIntegerDemo swapIntegerDemo = new SwapIntegerDemo();
        swapIntegerDemo.swap(a, b);
        System.out.println("a=" + a);
        System.out.println("b=" + b);
        // 此时IntegerCache 2的位置其实是3
        Integer c = 2;
        System.out.println(c.intValue());
    }

    private void swap(Integer a, Integer b) throws NoSuchFieldException, IllegalAccessException {
        // 通过反射拿到两个对象的value值
        Field field = Integer.class.getDeclaredField("value");
        field.setAccessible(true);
        int aValue = (int) field.get(a);
        int bValue = (int) field.get(b);
        field.setInt(aValue, bValue);
        field.setInt(bValue, aValue);
        field.setAccessible(false);
    }
}