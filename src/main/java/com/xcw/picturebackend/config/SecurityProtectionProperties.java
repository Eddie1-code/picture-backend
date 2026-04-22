package com.xcw.picturebackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局安全防护配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "security.protection")
public class SecurityProtectionProperties {

    private RateLimit rateLimit = new RateLimit();

    private Signature signature = new Signature();

    private Blacklist blacklist = new Blacklist();

    private Alert alert = new Alert();

    @Data
    public static class RateLimit {
        /**
         * 是否开启全局限流
         */
        private boolean enabled = true;

        /**
         * enforce=false 时仅记录，不阻断请求
         */
        private boolean enforce = true;

        /**
         * 仅在这些前缀内生效，空表示全局
         */
        private List<String> includePathPrefixes = new ArrayList<>();

        /**
         * 排除路径前缀
         */
        private List<String> excludePathPrefixes = new ArrayList<>();

        /**
         * 写请求限流配置（POST/PUT/PATCH/DELETE）
         */
        private Rule writeRule = new Rule(60, 40);

        /**
         * 读请求限流配置（GET）
         */
        private Rule readRule = new Rule(60, 160);

        /**
         * 高风险接口限流配置（路径前缀命中时覆盖默认值）
         */
        private Rule strictRule = new Rule(60, 15);

        private List<String> strictPathPrefixes = new ArrayList<>();

        /**
         * 自定义路径规则（优先级最高）
         */
        private List<PathRule> customPathRules = new ArrayList<>();
    }

    @Data
    public static class Rule {
        private int windowSeconds;
        private int limit;

        public Rule() {
        }

        public Rule(int windowSeconds, int limit) {
            this.windowSeconds = windowSeconds;
            this.limit = limit;
        }
    }

    @Data
    public static class PathRule {
        /**
         * 路径前缀，例如 /api/user/login
         */
        private String pathPrefix;

        /**
         * 可选：限定生效方法（GET/POST...），为空时表示所有方法
         */
        private List<String> methods = new ArrayList<>();

        private int windowSeconds = 60;

        private int limit = 20;
    }

    @Data
    public static class Signature {
        /**
         * 是否开启签名验签
         * 默认关闭，避免与前端未对齐时影响现有功能
         */
        private boolean enabled = false;

        /**
         * 参与 HMAC 的密钥，建议通过环境变量注入
         */
        private String secret = "change-this-sign-secret";

        /**
         * 时间戳允许偏差（秒）
         */
        private int allowedTimestampSkewSeconds = 300;

        /**
         * nonce 去重缓存时间（秒）
         */
        private int nonceTtlSeconds = 120;

        /**
         * 需要签名校验的接口前缀
         */
        private List<String> protectedPathPrefixes = new ArrayList<>();
    }

    @Data
    public static class Crawler {
        /**
         * 是否开启基础反爬规则
         */
        private boolean enabled = true;

        /**
         * 是否拦截空 User-Agent
         */
        private boolean blockEmptyUserAgent = true;

        /**
         * 反爬规则生效路径
         */
        private List<String> protectedPathPrefixes = new ArrayList<>();

        /**
         * 可疑 UA 关键字（小写匹配）
         */
        private List<String> blockedUserAgentKeywords = new ArrayList<>();
    }

    @Data
    public static class WebSocket {
        /**
         * 是否开启 WebSocket 握手限流
         */
        private boolean enabled = true;

        /**
         * 握手限流规则（user + ip 维度）
         */
        private Rule handshakeRule = new Rule(60, 30);
    }

    @Data
    public static class Blacklist {
        /**
         * 是否开启黑名单前置拦截
         */
        private boolean enabled = true;
    }

    private Crawler crawler = new Crawler();

    private WebSocket websocket = new WebSocket();

    @Data
    public static class Alert {
        /**
         * 是否开启限流告警日志
         */
        private boolean enabled = true;

        private int global429PerMinuteThreshold = 200;

        private int singleIp429PerMinuteThreshold = 80;

        private int anonymous429PerMinuteThreshold = 120;
    }
}
