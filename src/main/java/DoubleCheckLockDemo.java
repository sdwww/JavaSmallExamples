import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * DCL
 *
 * @author www
 */
public class DoubleCheckLockDemo {

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        ConcurrentHashMap<Singleton, String> map = new ConcurrentHashMap<>();
        for (int i = 0; i < 100; i++) {
            executorService.execute(() -> map.put(Singleton.getInstance(), "1"));
        }
        System.out.println(map.entrySet().size());
        executorService.shutdown();
    }

}

class Singleton {
    private static volatile Singleton singleton;

    public static Singleton getInstance() {
        if (singleton == null) {
            synchronized (Singleton.class) {
                if (singleton == null) {
                    singleton = new Singleton();
                }
            }
        }
        return singleton;
    }
}