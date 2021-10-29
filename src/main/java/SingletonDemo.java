/**
 * 单例
 *
 * @author www
 */
public class SingletonDemo {

    public static void main(String[] args) {
        Singleton instance1 = Singleton.getInstance();
        Singleton instance2 = Singleton.getInstance();
        System.out.println(instance1 == instance2);

        Singleton1 instance3 = Singleton1.getInstance();
        Singleton1 instance4 = Singleton1.getInstance();
        System.out.println(instance3 == instance4);
    }

}

class Singleton {
    private static volatile Singleton singleton;

    private Singleton() {
    }

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

// 静态内部类，延迟加载
class Singleton1 {

    private static class SingletonInstance {
        private static final Singleton1 INSTANCE = new Singleton1();
    }

    public static Singleton1 getInstance() {
        return SingletonInstance.INSTANCE;
    }
}
