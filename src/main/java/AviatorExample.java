import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;

import java.util.HashMap;
import java.util.Map;

public class AviatorExample {

    public static void main(String[] args) {
        // 定义规则表达式
        String expression = "(age >= 18 && sex == '男') || area == '北京'";

        // 准备环境变量
        Map<String, Object> env = new HashMap<>();
        env.put("age", 25); // 假设用户年龄为25岁
        env.put("sex", "男"); // 假设用户性别为男
        env.put("area", "上海"); // 假设用户所在地区为上海

        // 执行表达式并打印结果
        Boolean result = (Boolean) AviatorEvaluator.execute(expression, env);
        System.out.println("用户是否享受优惠：" + result); // 预期输出：false，因为虽然年龄和性别符合条件，但地区不符合
    }
}
