//package com.ljh.fawnhealth.controller;
//
//import com.alibaba.fastjson.JSONObject;
////import com.alipay.api.AlipayApiException;
////import com.alipay.api.internal.util.AlipaySignature;
//import com.ljh.fawnhealth.commen.BaseResponse;
//import com.ljh.fawnhealth.config.ResultUtils;
//import com.ljh.fawnhealth.model.entity.Order;
//import com.ljh.fawnhealth.service.OrderService;
//import com.ljh.fawnhealth.utils.PayUtil;
//import jakarta.annotation.Resource;
//import jakarta.servlet.http.HttpServletRequest;
//import org.springframework.web.bind.annotation.*;
//
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//
//@RestController
//@RequestMapping("/alipay")
//public class AliPayController {
//    @Resource
//    private PayUtil payUtil;
//    @Resource
//    private OrderService orderService;
//
////    @PostMapping("/pay")
////    public BaseResponse<String> alipay(@RequestParam Long orderId) throws AlipayApiException {
////        // 获取订单信息
////        Order order = orderService.getOrder(orderId);
////        if (order == null) {
////            ResultUtils.success("订单支付成功");
////        }
////
////        // 检查订单状态
////        if (order.getStatus() != 0) {
////            ResultUtils.success("订单状态异常，无法支付");
////        }
////        Order updateOrder = new Order();
////        updateOrder.setId(orderId);
////        updateOrder.setStatus(1); // 关键：手动设置目标状态为1（已支付待发货）
////        orderService.updateOrderStatus(updateOrder);
////        return ResultUtils.success("订单支付成功");
////    }
//
////    @PostMapping("/pay")
////    public String alipay(@RequestParam Long orderId) throws AlipayApiException {
////        // 获取订单信息
////        Order order = orderService.getOrder(orderId);
////        if (order == null) {
////            return "订单不存在";
////        }
////
////        // 检查订单状态
////        if (order.getStatus() != 0) {
////            return "订单状态异常，无法支付";
////        }
////
////        // 生成订单号（支付宝要求唯一）
////        String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
////        String user = UUID.randomUUID().toString().replace("-", "").toUpperCase();
////        String orderNum = time + user;
////
////        // 更新订单编号
////        order.setOrderNo(orderNum);
////        orderService.updateOrder(order);
////
////        // 调用支付宝支付接口
////        return payUtil.sendRequestToAlipay(
////                orderNum,
////                order.getPaymentAmount().floatValue(),
////                order.getOrderNo() // 使用订单号作为商品名称
////        );
////    }
//
//    @GetMapping("/toSuccess")
//    public String returns(@RequestParam String out_trade_no) {
//        try {
//            String queryResult = payUtil.query(out_trade_no);
//            JSONObject jsonObject = JSONObject.parseObject(queryResult);
//            JSONObject response = jsonObject.getJSONObject("alipay_trade_query_response");
//
//            if (response != null) {
//                String tradeStatus = response.getString("trade_status");
//
//                // 根据订单号获取订单
//                Order order = orderService.getOrderByOrderNo(out_trade_no);
//                if (order == null) {
//                    return "订单不存在";
//                }
//
//                if ("TRADE_SUCCESS".equals(tradeStatus)) {
//                    // 支付成功，更新订单状态
//                    order.setStatus(1); // 已支付待发货
//                    order.setPaymentTime(new Date());
//                    orderService.updateOrder(order);
//
//                    return "redirect:http://localhost:8081/#/paysuccess";
//                } else {
//                    // 支付失败
//                    return "redirect:http://localhost:8081/#/payfail";
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return "redirect:http://localhost:8081/#/payfail";
//    }
//
//    @PostMapping("/notify")
//    public String notify(HttpServletRequest request) {
//        Map<String, String[]> requestParams = request.getParameterMap();
//        Map<String, String> params = new HashMap<>();
//
//        for (String name : requestParams.keySet()) {
//            String[] values = requestParams.get(name);
//            String valueStr = "";
//            for (int i = 0; i < values.length; i++) {
//                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
//            }
//            params.put(name, valueStr);
//        }
//
//        try {
//            boolean signVerified = AlipaySignature.rsaCheckV1(
//                    params,
//                    payUtil.getAlipayPublicKey(), // 从PayUtil获取
//                    payUtil.getCharset(),         // 从PayUtil获取
//                    payUtil.getSignType()         // 从PayUtil获取
//            );
//
//            if (signVerified) {
//                String tradeStatus = params.get("trade_status");
//                String outTradeNo = params.get("out_trade_no");
//
//                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
//                    // 处理支付成功的业务逻辑
//                    Order order = orderService.getOrderByOrderNo(outTradeNo);
//                    if (order != null && order.getStatus() == 0) {
//                        order.setStatus(1); // 已支付待发货
//                        order.setPaymentTime(new Date());
//                        orderService.updateOrder(order);
//                    }
//                    return "success";
//                }
//            }
//        } catch (AlipayApiException e) {
//            e.printStackTrace();
//        }
//        return "fail";
//    }
//}