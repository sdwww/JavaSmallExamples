/**
 * @(#)HashMapTreeDemo.java, 10月 09, 2021.
 * <p>
 * Copyright 2021 fenbi.com. All rights reserved.
 * FENBI.COM PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

import java.util.HashMap;

/**
 * hashmap树化示例
 *
 * @author wangweiwei
 */
public class HashMapTreeDemo {

    // 当链表长度大于8时，转换成红黑树
    // 当数组长度小于64时，优先进行扩容
    public static void main(String[] args) {
        HashMap<SameHashCode, Integer> hashMap = new HashMap<>(64);
        for (int i = 0; i < 8; i++) {
            hashMap.put(new SameHashCode(i), i);
        }
    }

    private static class SameHashCode {

        private final int val;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SameHashCode that = (SameHashCode) o;
            return val == that.val;
        }

        public SameHashCode(int val) {
            this.val = val;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }
}