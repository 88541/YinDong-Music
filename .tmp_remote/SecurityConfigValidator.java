package com.cheang.cloudmusic.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class SecurityConfigValidator {

    private static final Pattern STRONG_PASSWORD =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{12,}$");

    private final AppProperties props;

    public SecurityConfigValidator(AppProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void validate() {
        List<String> errors = new ArrayList<>();

        // challenge 开关允许按发布阶段灰度切换，不在启动期强制

        // 必要密钥
        validateSecret(errors, "app.sign-key", props.getSignKey(),
                List.of("my-sign-key-change-this", "changeme", "default", "test"));
        validateSecret(errors, "app.secret-key", props.getSecretKey(),
                List.of("your-super-secret-key-change-this", "changeme", "default", "test"));
        validateAesKey(errors, props.getAesKey());
        validateSecret(errors, "app.sponsor-key", props.getSponsorKey(),
                List.of("ckYwselqtW2ziyXx_DrG3fWlCMjInjWE", "changeme", "default", "test"));

        // 管理员账号
        if (isBlank(props.getAdminUsername())) {
            errors.add("app.admin-username 不能为空");
        }
        String adminPwd = props.getAdminPassword();
        if (isBlank(adminPwd)) {
            errors.add("app.admin-password 不能为空");
        } else if (!STRONG_PASSWORD.matcher(adminPwd).matches()) {
            errors.add("app.admin-password 必须至少12位，且包含大小写字母、数字、特殊字符");
        }
        List<String> adminAllowedIps = props.getAdminAllowedIps();
        if (adminAllowedIps == null || adminAllowedIps.isEmpty()) {
            errors.add("app.admin-allowed-ips 不能为空");
        }
        if (isBlank(props.getBlockedUserAgents())) {
            errors.add("app.blocked-user-agents 不能为空");
        }

        // APP key 配置
        Map<String, AppProperties.AppConfig> apps = props.getApps();
        if (apps == null || apps.isEmpty()) {
            errors.add("app.apps 不能为空");
        } else {
            for (Map.Entry<String, AppProperties.AppConfig> e : apps.entrySet()) {
                String appId = e.getKey();
                AppProperties.AppConfig cfg = e.getValue();
                if (cfg == null || isBlank(cfg.getKey()) || cfg.getKey().length() < 20) {
                    errors.add("app.apps." + appId + ".key 强度不足(至少20位)");
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException("安全配置校验失败: " + String.join("; ", errors));
        }
    }

    private void validateAesKey(List<String> errors, String aesKey) {
        if (isBlank(aesKey)) {
            errors.add("app.aes-key 不能为空");
            return;
        }
        if (aesKey.length() < 16) {
            errors.add("app.aes-key 长度至少16位");
        }
        if ("1234567890abcdef".equalsIgnoreCase(aesKey)
                || "abcdefghijklmnop".equalsIgnoreCase(aesKey)
                || "aaaaaaaaaaaaaaaa".equalsIgnoreCase(aesKey)) {
            errors.add("app.aes-key 不能使用默认/弱密钥");
        }
    }

    private void validateSecret(List<String> errors, String name, String value, List<String> weakWords) {
        if (isBlank(value)) {
            errors.add(name + " 不能为空");
            return;
        }
        String lower = value.toLowerCase();
        for (String weak : weakWords) {
            if (lower.contains(weak.toLowerCase())) {
                errors.add(name + " 不能包含弱/默认值");
                break;
            }
        }
        if (value.length() < 24) {
            errors.add(name + " 长度至少24位");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
