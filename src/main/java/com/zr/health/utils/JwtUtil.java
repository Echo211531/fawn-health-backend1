package com.zr.health.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JwtUtil 工具类，提供了生成和解析 JSON Web Token（JWT）的功能。
 * JWT 是一种用于在网络应用中安全传输信息的开放标准（RFC 7519），
 * 常用于身份验证和授权场景。
 */
public class JwtUtil {

    /**
     * 生成 JWT（JSON Web Token）。
     * 使用 Hs256 算法进行签名，私钥使用传入的固定秘钥。
     *
     * @param secretKey jwt 秘钥，用于对 JWT 进行签名，确保 JWT 的完整性和真实性，
     *                  应妥善保管，不能泄露。
     * @param ttlMillis jwt 过期时间，以毫秒为单位，指定 JWT 的有效时间范围。
     * @param claims    包含要存储在 JWT 中的自定义信息的 Map，例如用户 ID、用户名等。
     *                  这些信息将作为 JWT 的负载部分。
     * @return 生成的 JWT 字符串，可用于在客户端和服务端之间传输信息。
     */
    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
        // 指定签名的时候使用的签名算法，这里使用 Hs256 算法，也就是 JWT 头部的 alg 字段值
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        // 计算 JWT 的过期时间，当前时间加上指定的过期时间（毫秒）
        long expMillis = System.currentTimeMillis() + ttlMillis;
        Date exp = new Date(expMillis);

        // 构建 JWT，设置 JWT 的各个部分
        JwtBuilder builder = Jwts.builder()
                // 如果有私有声明，一定要先设置这个自己创建的私有的声明，这个是给 builder 的 claim 赋值，
                // 一旦写在标准的声明赋值之后，就会覆盖那些标准的声明的值
                .setClaims(claims)
                // 设置签名使用的签名算法和签名使用的秘钥，将秘钥转换为字节数组
                .signWith(signatureAlgorithm, secretKey.getBytes(StandardCharsets.UTF_8))
                // 设置 JWT 的过期时间
                .setExpiration(exp);

        // 生成最终的 JWT 字符串
        return builder.compact();
    }

    /**
     * 解析 JWT（JSON Web Token），从加密后的 Token 中提取出负载信息。
     *
     * @param secretKey jwt 秘钥，此秘钥必须与生成 JWT 时使用的秘钥一致，
     *                  用于验证 JWT 的签名，确保 Token 的合法性。
     * @param token     加密后的 JWT 字符串，即需要解析的 Token。
     * @return 解析后的 Claims 对象，其中包含了 JWT 中的负载信息，如自定义声明和标准声明。
     */
    public static Claims parseJWT(String secretKey, String token) {
        // 使用 Jwts.parser() 方法创建一个 JWT 解析器
        Claims claims = Jwts.parser()
                // 设置用于验证签名的秘钥，将秘钥转换为字节数组
                .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                // 解析传入的 JWT 字符串，并获取其负载部分（即 body）
                .parseClaimsJws(token).getBody();
        return claims;
    }
}