import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * jdk代理简单实现
 */
public class JdkProxyExample {

    public static void main(String[] args) {
        B b = new B();
        A a = (A) Proxy.newProxyInstance(B.class.getClassLoader(), B.class.getInterfaces(), (proxy, method, args1) -> {
            Arrays.stream(args1).forEach(System.out::println);
            return method.invoke(b, args1);
        });
        System.out.println(a.say("hello world"));
    }
}

interface A {
    int say(String something);
}

class B implements A {

    @Override
    public int say(String something) {
        System.out.println("say:" + something);
        return something.hashCode();
    }
}
