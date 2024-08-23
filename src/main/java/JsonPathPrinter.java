import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonPathPrinter {

    public static void main(String[] args) throws JsonProcessingException {
        String jsonString = "{\"name\":\"John\", \"age\":30, \"city\":\"New York\", " +
                "\"details\":{\"hobbies\":[\"reading\", \"swimming\"], " +
                "\"address\":{\"street\":\"123 Elm St\", \"city\":\"Springfield\"}}}";

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonString);
        printJsonPath(rootNode, "object");
    }

    private static void printJsonPath(JsonNode node, String currentPath) {
        if (node.isObject()) {
            // 处理对象节点
            node.fields().forEachRemaining(entry -> {
                printJsonPath(entry.getValue(), currentPath + "." + entry.getKey());
            });
        } else if (node.isArray()) {
            // 处理数组节点
            for (int i = 0; i < node.size(); i++) {
                JsonNode element = node.get(i);
                // 数组元素是对象或数组时，递归打印
                printJsonPath(element, currentPath + "[" + i + "]");
            }
        } else {
            // 简单类型，直接打印
            System.out.println(currentPath);
        }
    }
}
