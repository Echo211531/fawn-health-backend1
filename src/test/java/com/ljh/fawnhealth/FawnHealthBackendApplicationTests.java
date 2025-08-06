package com.ljh.fawnhealth;

import com.ljh.fawnhealth.ai.agent.FawnManus;
import com.ljh.fawnhealth.ai.tool.ZhipuWebSearchTool;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FawnHealthBackendApplicationTests {

    @Resource
    private ZhipuWebSearchTool zhipuWebSearchTool;
    @Test
    void searchWeb() {
//        try {
//            String result = zhipuWebSearchTool.webSearch("人工智能最新发展");
//            System.out.println(result);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
//    @Resource
//    private HealthApp healthApp;
//    @Test
//    void doChatWithMcp() {
//        String chatId = UUID.randomUUID().toString();
//        // 测试地图 MCP
//        String message = "帮我搜索一些蔡徐坤照片";
//        String answer =  healthApp.doChatWithMcp(message,chatId);
//        System.out.println(answer);
//    }
    @Resource
    private FawnManus fawnManus;

//    @Test
//    void run() {
//        String userPrompt = """
//        我居住在上海静安区，今天肚子有点痛，请帮我找到 5 公里内合适的医院，
//        并结合一些网络图片，制定一份详细的看病计划，
//        并以 PDF 格式输出""";
//        String answer = fawnManus.run(userPrompt);
//        System.out.println(answer);
//    }
}
