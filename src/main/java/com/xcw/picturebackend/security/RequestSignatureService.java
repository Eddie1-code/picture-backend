package com.xcw.picturebackend.security;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.xcw.picturebackend.config.RequestWrapper;
import com.xcw.picturebackend.config.SecurityProtectionProperties;
import com.xcw.picturebackend.exception.BusinessException;
import com.xcw.picturebackend.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestSignatureService {

    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_NONCE = "X-Nonce";
    private static final String HEADER_SIGNATURE = "X-Signature";

    private final StringRedisTemplate stringRedisTemplate;
    private final SecurityProtectionProperties securityProtectionProperties;

    public void verifyIfRequired(HttpServletRequest request) {
        SecurityProtectionProperties.Signature signature = securityProtectionProperties.getSignature();
        if (!signature.isEnabled()) {
            return;
        }
        String path = request.getRequestURI();
        if (!isProtectedPath(path, signature)) {
            return;
        }
        String timestampText = request.getHeader(HEADER_TIMESTAMP);
        String nonce = request.getHeader(HEADER_NONCE);
        String clientSignature = request.getHeader(HEADER_SIGNATURE);
        if (StrUtil.hasBlank(timestampText, nonce, clientSignature)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "签名参数缺失");
        }
        long timestamp = parseTimestamp(timestampText);
        long current = Instant.now().getEpochSecond();
        if (Math.abs(current - timestamp) > signature.getAllowedTimestampSkewSeconds()) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "请求已过期");
        }
        ensureNonceNotReplay(nonce, signature.getNonceTtlSeconds());
        String bodyHash = resolveBodyHash(request);
        String method = request.getMethod();
        String plainText = method + "\n" + path + "\n" + timestampText + "\n" + nonce + "\n" + bodyHash;
        String expected = hmacSha256Base64(plainText, signature.getSecret());
        boolean matched = MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                clientSignature.getBytes(StandardCharsets.UTF_8));
        if (!matched) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "签名校验失败");
        }
    }

    private boolean isProtectedPath(String path, SecurityProtectionProperties.Signature signature) {
        if (StrUtil.isBlank(path)) {
            return false;
        }
        for (String prefix : signature.getProtectedPathPrefixes()) {
            if (StrUtil.isNotBlank(prefix) && path.startsWith(prefix.trim())) {
                return true;
            }
        }
        return false;
    }

    private long parseTimestamp(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效时间戳");
        }
    }

    private void ensureNonceNotReplay(String nonce, int ttlSeconds) {
        String key = "security:nonce:" + nonce;
        try {
            Boolean success = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, "1", ttlSeconds, TimeUnit.SECONDS);
            if (!Boolean.TRUE.equals(success)) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "重复请求已拦截");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("nonce 去重检查异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "签名校验异常");
        }
    }

    private String resolveBodyHash(HttpServletRequest request) {
        if (request instanceof RequestWrapper) {
            return DigestUtil.sha256Hex(((RequestWrapper) request).getBody());
        }
        return DigestUtil.sha256Hex("");
    }

    private String hmacSha256Base64(String content, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] signBytes = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signBytes);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "签名计算异常");
        }
    }
}
