/**
 * @(#)FinallyDemo.java, 9月 28, 2021.
 * <p>
 * Copyright 2021 fenbi.com. All rights reserved.
 * FENBI.COM PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/**
 * @author wangweiwei
 */
public class FinallyDemo {

    public static void main(String[] args) {
        FinallyDemo finallyDemo = new FinallyDemo();

        int i = finallyDemo.method1();
        System.out.println(i);

        int j = finallyDemo.method2();
        System.out.println(j);
    }

    // finally会在return语句之后，return之前执行
    public int method1() {
        int i = 0;
        try {
            return ++i;
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            ++i;
        }
        return i;
    }

    // finally 的return会覆盖掉try的return
    public int method2() {
        int i = 0;
        try {
            return i++;
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            i++;
            return i;
        }
    }
}