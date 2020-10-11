/**
 * 证明指令重排序
 *
 * @author www
 */
public class ProofInstructionReorder {
    static int x = 0, y = 0, a = 0, b = 0;

    public static void main(String[] args) throws InterruptedException {
        int count = 0;
        while (true) {
            x = 0;
            y = 0;
            a = 0;
            b = 0;
            Thread one = new Thread(() -> {
                a = 1;
                y = b;
            });
            Thread other = new Thread(() -> {
                b = 1;
                x = a;
            });
            one.start();
            other.start();
            one.join();
            other.join();
            if (x == 0 && y == 0) {
                System.out.println("第" + count + "次，x=0，y=0");
                break;
            }
            count++;
        }
    }
}
