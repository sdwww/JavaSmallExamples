/**
 * @(#)BloomFilterExample.java, 8月 05, 2021.
 * <p>
 * Copyright 2021 fenbi.com. All rights reserved.
 * FENBI.COM PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

/**
 * 布隆过滤器
 *
 * @author wangweiwei
 */
public class BloomFilterDemo {

    public static void main(String[] args) {
        int total = 1000000; // 总数量
        BloomFilter<String> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_8), total * 10);
        // 初始化 1000000 条数据到过滤器中
        for (int i = 0; i < total; i++) {
            bloomFilter.put("" + i);
        }
        // 判断值是否存在过滤器中
        int count = 0;
        for (int i = 0; i < total + 10000; i++) {
            if (bloomFilter.mightContain("" + i)) {
                count++;
            }
        }
        System.out.println("已匹配数量 " + count);
    }
}