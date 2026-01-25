import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;

import java.util.Collection;

/**
 * AC自动机
 */
public class ACDemo {

    public static void main(String[] args) {
        // 1. 创建Trie并添加关键词
        Trie trie = Trie.builder()
                .addKeyword("he")
                .addKeyword("she")
                .addKeyword("his")
                .addKeyword("hers")
                .ignoreOverlaps()
                .build();

        // 2. 搜索文本
        String text = "ushersjdjdjdjjjjjdjdjdjfodsdsjmofmodsmdhdhdhhdoieioqwifcsuvasdbjbdsjbvjfabfjfavbjbjasbjvfbabvf" +
                "jabjfsabjabjvbajbvjabjabvjabsvjbasjbfjasdfdlajkfdskaqiwqoiiurwhsdjhsajkqioiuhvibasuvbavsbjakbiqbwebre" +
                "uvbuwreqavsnbaksnla;vnieaiewbureberb fbfabvfjkvfbjfbvjbsafkbkasbkjasbkafsbfrjrjjrjracsasncnjsdaweqiqwy" +
                "rbfuwq84383245ufbweuib3u21fg77qwb7c jsax jasd cjbsvuabv8qvwbeu79erbv7bew79rbv98ewqg8vbewwsbqv8nasvibve" +
                "wuvbdsuvfbdjasadsbvfbajbadsfubierbv8ewv8basivbdsfvb dshjvbjdsvbjhbv dsfajbvjbdfjsvbjv dffbfvjbdf";
        Collection<Emit> emits = trie.parseText(text);

        // 3. 输出结果
        System.out.println("匹配结果:");
        for (Emit emit : emits) {
            System.out.printf("关键词: '%s', 起始: %d, 结束: %d%n",
                    emit.getKeyword(), emit.getStart(), emit.getEnd());
        }
    }
}
