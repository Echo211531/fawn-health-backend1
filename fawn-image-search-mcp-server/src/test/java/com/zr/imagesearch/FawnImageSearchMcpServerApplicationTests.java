package com.zr.imagesearch;

import com.zr.imagesearch.tools.ImageSearchTool;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FawnImageSearchMcpServerApplicationTests {

    @Test
    void contextLoads() {
    }
    @Resource
    private ImageSearchTool imageSearchTool;
//    @Test
//    void searchImage() {
//        String result = imageSearchTool.searchImage("computer");
//        Assertions.assertNotNull(result);
//    }
}
