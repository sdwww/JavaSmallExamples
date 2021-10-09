/**
 * @(#)HashIdUtil.java, 10月 09, 2021.
 * <p>
 * Copyright 2021 fenbi.com. All rights reserved.
 * FENBI.COM PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

import lombok.NonNull;
import org.hashids.Hashids;

/**
 * @author wangweiwei
 */
public class HashIdUtil {

    private static final String SALT = "abcdefg";

    /**
     * 加密id
     */
    public static String encode(@NonNull Long id) {
        Hashids hashIds = new Hashids(SALT);
        return hashIds.encode(id);
    }

    public static void main(String[] args) {
        for (int i = Integer.MAX_VALUE-1000000; i < Integer.MAX_VALUE; i++) {
            String encode = HashIdUtil.encode((long) i);
            if (i % 10000 == 0) {
                System.out.println(encode);
            }
        }
    }
}