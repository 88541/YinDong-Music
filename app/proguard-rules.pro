# ═══════════════════════════════════════════════════════════════
# 云音乐 ProGuard / R8 加固规则
# ═══════════════════════════════════════════════════════════════

# ══════════ 1. 激进混淆策略 ══════════

# 混淆字典：反编译后变量名为 a/b/c/aa/ab 等无意义短标识
-obfuscationdictionary proguard-dictionary.txt
-classobfuscationdictionary proguard-dictionary.txt
-packageobfuscationdictionary proguard-dictionary.txt

# 所有类重新打包到极短的包名下
-repackageclasses 'c'
-allowaccessmodification

# 移除源文件名和行号（崩溃堆栈不暴露真实代码结构）
-renamesourcefileattribute ''
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,EnclosingMethod

# 优化：多轮优化 + 移除未使用的代码
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose

# 移除所有调试日志（release 中不留痕迹）
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# 移除 System.out / System.err
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# ══════════ 2. 必要保留规则 ══════════

# ── 数据模型 (JSON 解析用到反射) ──
-keep class com.xinzhonghe.yeyumusic.data.model.** { *; }

# ── OkHttp ──
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ── QuickJS JavaScript 引擎 ──
-keep class com.whl.quickjs.** { *; }
-keep class com.whl.quickjs.wrapper.** { *; }
-dontwarn com.whl.quickjs.**

# ── Media3 / ExoPlayer ──
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ── Coil ──
-dontwarn coil.**
-keep class coil.** { *; }

# ── MediaSessionService (系统通过反射实例化) ──
-keep class com.xinzhonghe.yeyumusic.MusicPlaybackService { *; }

# ── Compose ──
-dontwarn androidx.compose.**

# ── Application (系统反射实例化) ──
-keep class com.xinzhonghe.yeyumusic.CloudMusicApplication { *; }

# ══════════ 3. 安全加固专用规则 ══════════

# SecurityGuard 的检测方法名混淆后仍保持内部调用链完整
# (不需要 keep，因为内部互相调用；混淆掉反而更好)

# 防止通过反射找到关键安全类名
# StringEncryptor 也不 keep，让它被混淆

# ══════════ 4. 移除调试信息 ══════════

# 移除 Kotlin 元数据（防止 Kotlin Decompiler 还原）
-dontwarn kotlin.**
-dontwarn kotlinx.**
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNull(...);
    public static void checkNotNullParameter(...);
    public static void checkParameterIsNotNull(...);
    public static void checkNotNullExpressionValue(...);
    public static void checkExpressionValueIsNotNull(...);
    public static void checkReturnedValueIsNotNull(...);
}
