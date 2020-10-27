import java.util.Arrays;
import java.util.HashMap;

/**
 * 测试hashMap线程安全问题
 * @author www
 */
public class HashmapThreadSafety {

    /**
     * @param args
     */
    public static void main(String[] args) throws InterruptedException {
        HashMap<Integer, String> hashMap = new HashMap<>();
        Thread[] threads =new Thread[100000];
        for (int i = 0; i < 100000; i++) {
            int finalI = i;
            threads[i] = new Thread(() -> {
                hashMap.put(finalI, String.valueOf(finalI));
            });
        }
        Arrays.stream(threads).forEach(Thread::start);
        Thread.sleep(1000);
        for (int i = 0; i < 100000; i++) {
            if (null == hashMap.get(i)) {
                System.out.println(i);
            }
        }
    }
}