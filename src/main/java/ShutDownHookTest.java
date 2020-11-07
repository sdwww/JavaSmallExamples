/**
 * 优雅关机
 */
public class ShutDownHookTest {

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            System.out.println("do someThing");
        }));
        throw new RuntimeException();
    }
}
