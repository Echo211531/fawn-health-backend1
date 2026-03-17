package com.zr.health.manager;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

@Component
public class BannedWordsManager {

    private Set<String> bannedWords = new HashSet<>();

    @PostConstruct
    public void loadBannedWords() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("banned_words.txt"), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    bannedWords.add(line);
                }
            }
            System.out.println("加载违禁词条数：" + bannedWords.size());
        } catch (Exception e) {
            System.err.println("加载违禁词文件失败：" + e.getMessage());
        }
    }

    /**
     * 判断内容是否包含违禁词
     * @param content 待检测文本
     * @return true 包含违禁词，false 不包含
     */
    public boolean containsBannedWord(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        for (String word : bannedWords) {
            if (content.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
