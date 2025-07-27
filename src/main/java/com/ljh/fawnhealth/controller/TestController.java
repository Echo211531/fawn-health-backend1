//package com.ljh.fawnhealth.controller;
//
//import com.alipay.api.AlipayApiException;
//import jakarta.annotation.Resource;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//
//@RestController
//@RequestMapping("/test")
//public class TestController {
//
//    @Resource
//    private AliPayController aliPayController;
//
//    /**
//     * 生成可直接在手机浏览器中打开的支付页面
//     */
//    @GetMapping("/pay/mobile")
//    public String mobilePay() throws AlipayApiException {
//        Long testOrderId = 1L; // 替换为你的测试订单ID
//        String alipayForm = aliPayController.alipay(testOrderId);
//
//        // 包装成移动友好页面（自动提交表单）
//        return "<!DOCTYPE html>" +
//                "<html><head>" +
//                "<meta charset=\"UTF-8\">" +
//                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">" +
//                "<title>支付宝支付</title>" +
//                "<style>" +
//                "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; padding: 20px; text-align: center; }" +
//                ".loading { margin-top: 50px; font-size: 18px; color: #333; }" +
//                "</style>" +
//                "</head>" +
//                "<body>" +
//                "<div class=\"loading\">正在跳转支付宝...</div>" +
//                alipayForm +
//                "<script>document.forms[0].submit();</script>" +
//                "</body></html>";
//    }
//
//    /**
//     * 生成支付二维码（备用方案）
//     */
//    @GetMapping("/pay/qrcode")
//    public String qrCodePay() throws AlipayApiException {
//        Long testOrderId = 1L;
//        String payUrl = "http://g7da6c69.natappfree.cc/test/pay/mobile?orderId=" + testOrderId;
//
//        return "<!DOCTYPE html>" +
//                "<html><head>" +
//                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
//                "<title>扫码支付</title>" +
//                "<style>" +
//                "body { text-align: center; padding: 20px; }" +
//                "h3 { color: #1677ff; }" +
//                "img { margin: 20px auto; max-width: 80%; }" +
//                "</style>" +
//                "</head>" +
//                "<body>" +
//                "<h3>请使用支付宝扫码支付</h3>" +
//                "<img src='https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=" + payUrl + "'>" +
//                "<p>如果无法扫码，请<a href='" + payUrl + "'>点击这里</a>跳转支付</p>" +
//                "</body></html>";
//    }
//}