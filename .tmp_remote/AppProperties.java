package com.cheang.cloudmusic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    // ===== 密钥 =====
    private String signKey;
    private String aesKey;
    private String secretKey;

    // ===== 多APP =====
    private Map<String, AppConfig> apps;

    // ===== 验证 =====
    private int tokenExpireSeconds = 300;
    private int heartbeatInterval = 120;
    private int maxDeviceSessions = 3;
    private List<String> allowedPackages;

    // ===== 管理员 =====
    private String adminUsername;
    private String adminPassword;
    private List<String> adminAllowedIps;

    // ===== 安全 =====
    private int rateLimitPerMinute = 30;
    private int deviceRateLimitPerHour = 200;
    private int musicRateLimitPerMinute = 20;
    private int maxDeviceIpsPerHour = 5;
    private int signTimestampTolerance = 300;
    private int musicSignTimestampTolerance = 60;
    private int nonceExpireSeconds = 120;
    private boolean challengeRequired = false;
    private int challengeExpireSeconds = 30;
    private int challengeSessionExpireSeconds = 300;
    private int challengeTimestampToleranceSeconds = 120;
    private boolean challengeBindIp = true;
    private boolean challengeRequireReqSignature = true;
    private String blockedUserAgents;

    // ===== 外部API =====
    private String externalApiHost;
    private String qishuiApiUrl;
    private String qishuiApiKey;
    private String yunzhiHost;
    private String yunzhiToken;
    private String qqMusicCookie;

    // ===== 赞助 =====
    private String sponsorKey;

    // ===== 插件 =====
    private int pluginTokenTtlDays = 7;
    private boolean builtinPluginEnabled = true;

    // ===== 可信代理 IP (只有来自这些 IP 才读 X-Forwarded-For) =====
    private List<String> trustedProxyIps;

    /** XOR 加密用的 key (AES_KEY 前 8 字节) */
    public byte[] xorKey() {
        return aesKey.substring(0, 8).getBytes();
    }

    // ===== 内部类: 单个 APP 配置 =====
    public static class AppConfig {
        private String name;
        private String key;
        private String latestVersion;
        private String minVersion;
        private String updateUrl;
        private boolean forceUpdate;
        private boolean appEnabled = true;
        private String announcement = "";

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getLatestVersion() { return latestVersion; }
        public void setLatestVersion(String latestVersion) { this.latestVersion = latestVersion; }
        public String getMinVersion() { return minVersion; }
        public void setMinVersion(String minVersion) { this.minVersion = minVersion; }
        public String getUpdateUrl() { return updateUrl; }
        public void setUpdateUrl(String updateUrl) { this.updateUrl = updateUrl; }
        public boolean isForceUpdate() { return forceUpdate; }
        public void setForceUpdate(boolean forceUpdate) { this.forceUpdate = forceUpdate; }
        public boolean isAppEnabled() { return appEnabled; }
        public void setAppEnabled(boolean appEnabled) { this.appEnabled = appEnabled; }
        public String getAnnouncement() { return announcement; }
        public void setAnnouncement(String announcement) { this.announcement = announcement; }
    }

    // ===== getter / setter (全部) =====

    public String getSignKey() { return signKey; }
    public void setSignKey(String signKey) { this.signKey = signKey; }
    public String getAesKey() { return aesKey; }
    public void setAesKey(String aesKey) { this.aesKey = aesKey; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public Map<String, AppConfig> getApps() { return apps; }
    public void setApps(Map<String, AppConfig> apps) { this.apps = apps; }
    public int getTokenExpireSeconds() { return tokenExpireSeconds; }
    public void setTokenExpireSeconds(int v) { this.tokenExpireSeconds = v; }
    public int getHeartbeatInterval() { return heartbeatInterval; }
    public void setHeartbeatInterval(int v) { this.heartbeatInterval = v; }
    public int getMaxDeviceSessions() { return maxDeviceSessions; }
    public void setMaxDeviceSessions(int v) { this.maxDeviceSessions = v; }
    public List<String> getAllowedPackages() { return allowedPackages; }
    public void setAllowedPackages(List<String> v) { this.allowedPackages = v; }
    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String v) { this.adminUsername = v; }
    public String getAdminPassword() { return adminPassword; }
    public void setAdminPassword(String v) { this.adminPassword = v; }
    public List<String> getAdminAllowedIps() { return adminAllowedIps; }
    public void setAdminAllowedIps(List<String> adminAllowedIps) { this.adminAllowedIps = adminAllowedIps; }
    public int getRateLimitPerMinute() { return rateLimitPerMinute; }
    public void setRateLimitPerMinute(int v) { this.rateLimitPerMinute = v; }
    public int getDeviceRateLimitPerHour() { return deviceRateLimitPerHour; }
    public void setDeviceRateLimitPerHour(int v) { this.deviceRateLimitPerHour = v; }
    public int getMusicRateLimitPerMinute() { return musicRateLimitPerMinute; }
    public void setMusicRateLimitPerMinute(int v) { this.musicRateLimitPerMinute = v; }
    public int getMaxDeviceIpsPerHour() { return maxDeviceIpsPerHour; }
    public void setMaxDeviceIpsPerHour(int v) { this.maxDeviceIpsPerHour = v; }
    public int getSignTimestampTolerance() { return signTimestampTolerance; }
    public void setSignTimestampTolerance(int v) { this.signTimestampTolerance = v; }
    public int getMusicSignTimestampTolerance() { return musicSignTimestampTolerance; }
    public void setMusicSignTimestampTolerance(int v) { this.musicSignTimestampTolerance = v; }
    public int getNonceExpireSeconds() { return nonceExpireSeconds; }
    public void setNonceExpireSeconds(int v) { this.nonceExpireSeconds = v; }
    public boolean isChallengeRequired() { return challengeRequired; }
    public void setChallengeRequired(boolean challengeRequired) { this.challengeRequired = challengeRequired; }
    public int getChallengeExpireSeconds() { return challengeExpireSeconds; }
    public void setChallengeExpireSeconds(int challengeExpireSeconds) { this.challengeExpireSeconds = challengeExpireSeconds; }
    public int getChallengeSessionExpireSeconds() { return challengeSessionExpireSeconds; }
    public void setChallengeSessionExpireSeconds(int challengeSessionExpireSeconds) { this.challengeSessionExpireSeconds = challengeSessionExpireSeconds; }
    public int getChallengeTimestampToleranceSeconds() { return challengeTimestampToleranceSeconds; }
    public void setChallengeTimestampToleranceSeconds(int challengeTimestampToleranceSeconds) { this.challengeTimestampToleranceSeconds = challengeTimestampToleranceSeconds; }
    public boolean isChallengeBindIp() { return challengeBindIp; }
    public void setChallengeBindIp(boolean challengeBindIp) { this.challengeBindIp = challengeBindIp; }
    public boolean isChallengeRequireReqSignature() { return challengeRequireReqSignature; }
    public void setChallengeRequireReqSignature(boolean challengeRequireReqSignature) { this.challengeRequireReqSignature = challengeRequireReqSignature; }
    public String getBlockedUserAgents() { return blockedUserAgents; }
    public void setBlockedUserAgents(String v) { this.blockedUserAgents = v; }
    public String getExternalApiHost() { return externalApiHost; }
    public void setExternalApiHost(String v) { this.externalApiHost = v; }
    public String getQishuiApiUrl() { return qishuiApiUrl; }
    public void setQishuiApiUrl(String v) { this.qishuiApiUrl = v; }
    public String getQishuiApiKey() { return qishuiApiKey; }
    public void setQishuiApiKey(String v) { this.qishuiApiKey = v; }
    public String getYunzhiHost() { return yunzhiHost; }
    public void setYunzhiHost(String v) { this.yunzhiHost = v; }
    public String getYunzhiToken() { return yunzhiToken; }
    public void setYunzhiToken(String v) { this.yunzhiToken = v; }
    public String getQqMusicCookie() { return qqMusicCookie; }
    public void setQqMusicCookie(String v) { this.qqMusicCookie = v; }
    public String getSponsorKey() { return sponsorKey; }
    public void setSponsorKey(String v) { this.sponsorKey = v; }
    public int getPluginTokenTtlDays() { return pluginTokenTtlDays; }
    public void setPluginTokenTtlDays(int v) { this.pluginTokenTtlDays = v; }
    public boolean isBuiltinPluginEnabled() { return builtinPluginEnabled; }
    public void setBuiltinPluginEnabled(boolean v) { this.builtinPluginEnabled = v; }
    public List<String> getTrustedProxyIps() { return trustedProxyIps; }
    public void setTrustedProxyIps(List<String> v) { this.trustedProxyIps = v; }
}
