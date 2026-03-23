package com.zr.health.interceptor;

import com.zr.health.config.JwtProperties;
import com.zr.health.constant.JwtClaimsConstant;
import com.zr.health.context.BaseContext;
import com.zr.health.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * jwt令牌校验的拦截器
 */
@Component
@Slf4j
public class JwtTokenAdminInterceptor implements HandlerInterceptor {

    @Resource
    private JwtProperties jwtProperties;

    /**
     * 校验jwt
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("JwtTokenAdminInterceptor 执行了！请求路径为：{}", request.getRequestURI());

        // 跨域预检请求（OPTIONS）不应做鉴权校验，否则会导致前端在已登录状态下也被401拦截
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        // 初始化当前线程的用户ID，防止残留数据
        BaseContext.removeCurrentId();

        // 判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            log.info("非Controller方法，直接放行");
            return true;
        }

        // 1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getUserTokenName());
        // 兼容标准 Authorization: Bearer <token> 写法，便于Postman/网关接入
        if (token == null || token.trim().isEmpty()) {
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null && authorization.startsWith("Bearer ")) {
                token = authorization.substring(7).trim();
            }
        }
        log.info("获取到的令牌: {}", token != null ? "***" : "null");

        // 2、校验令牌
        try {
            if (token == null || token.trim().isEmpty()) {
                throw new IllegalArgumentException("缺少JWT令牌");
            }

            log.info("开始解析JWT");
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
            log.info("当前用户ID：{}", userId);

            // 使用ThreadLocal存储用户ID
            BaseContext.setCurrentId(userId);
            log.info("已存储用户ID到ThreadLocal");

            return true;
        } catch (Exception ex) {
            log.error("JWT 校验失败：{}", ex.getMessage(), ex);

            // 确保清除可能存在的ThreadLocal数据
            BaseContext.removeCurrentId();

            // 返回未授权响应
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":40100,\"data\":\"null\",\"msg\":\"未登录或Token已失效\"}");

            return false;
        }
    }

    /**
     * 请求完成后清除ThreadLocal中的用户信息
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        log.info("请求处理完成，清除ThreadLocal中的用户ID");
        BaseContext.removeCurrentId();
    }
}