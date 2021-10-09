import org.openjdk.jol.info.ClassLayout;

/**
 * @author www
 * JOL：查看Java 对象布局、大小工具
 */
public class JolDemo {

    public static void main(String[] args) {

        // 一个object占用16个字节，其中mark word占用8位，class pointer占用4位（压缩指针开启，未开启8位）
        // 数据0位，填充
        Object object = new Object();
        System.out.println(ClassLayout.parseInstance(object).toPrintable());

        // 一个boolean类型数组占用一个字节，length类型为int，占用4位
        boolean[] booleans = new boolean[1024];
        System.out.println(ClassLayout.parseInstance(booleans).toPrintable());

        // 一个boolean类型占用一个字节
        boolean b = false;
        System.out.println(ClassLayout.parseInstance(b).toPrintable());
    }
}
