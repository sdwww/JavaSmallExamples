import org.openjdk.jol.info.ClassLayout;

/**
 * @author www
 * JOL：查看Java 对象布局、大小工具
 */
public class JolDemo {
    public static void main(String[] args) {
        Object object = new Object();
        System.out.println(ClassLayout.parseInstance(object).toPrintable());
    }
}
