import java.util.Arrays;
import java.util.HashMap;

/**
 * 测试hashMap线程安全问题
 *
 * @author www
 */
public class HashmapThreadSafety {

    /**
     * @param args
     */
    public static void main(String[] args) throws InterruptedException {
        HashMap<Integer, String> hashMap = new HashMap<>();
        Thread[] threads = new Thread[100];
        for (int i = 0; i < 100; i++) {
            int finalI = i;
            threads[i] = new Thread(() -> put(hashMap, finalI));
        }
        Arrays.stream(threads).forEach(Thread::start);
        Thread.sleep(2000);
        for (int i = 0; i < 10000; i++) {
            if (null == hashMap.get(i)) {
                System.out.println(i);
            }
        }
        System.out.println(hashMap.keySet().size());
    }

    public static void put(HashMap<Integer, String> hashMap, int i) {
        for (int j = i * 100; j < i * 100 + 100; j++) {
            hashMap.put(j, String.valueOf(j));
        }
    }
}