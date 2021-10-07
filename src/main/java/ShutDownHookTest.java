/**
 * 优雅关机
 */
public class ShutDownHookTest {

    public static void main(String[] args) throws CloneNotSupportedException {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("do someThing");
        }));
        ShutDownHookTest shutDownHookTest = new ShutDownHookTest();
        Integer a = Integer.valueOf(123);
        Integer b = Integer.valueOf(123);
        System.out.println(a == b);
    }
}
