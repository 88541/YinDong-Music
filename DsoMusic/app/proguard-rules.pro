# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class com.dirror.music.music.** { *; }
-keep class com.dirror.music.data.** { *; }
-keep class com.dirror.music.room.** { *; }

# 悬浮歌词服务相关类
-keep class com.dirror.music.service.FloatingLyricsService { *; }
-keep class com.dirror.music.ui.activity.FloatingLyricsSettingsActivity { *; }
-keep class com.dirror.music.util.FloatingLyricsSettings { *; }

# 保持 Service 类
-keep class * extends android.app.Service { *; }

# 保持 Activity 类
-keep class * extends android.app.Activity { *; }

# 保持 ViewBinding 类
-keep class * extends androidx.viewbinding.ViewBinding { *; }

-ignorewarnings

-keepattributes Signature, *Annotation*

# keep BmobSDK
-dontwarn cn.bmob.v3.**
-keep class cn.bmob.v3.** {*;}

# keep 友盟
-keep class com.umeng.** {*;}

# 您如果使用了稳定性模块可以加入该混淆
-keep class com.uc.** {*;}
-keepclassmembers class * {
   public <init> (org.json.JSONObject);
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# keep okhttp3、okio
-dontwarn okhttp3.**
-keep class okhttp3.** { *;}
-keep interface okhttp3.** { *; }
-dontwarn okio.**

# keep rx
-dontwarn sun.misc.**
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
    long producerIndex;
    long consumerIndex;
}

# Kotlin 协程
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# LiveData 和 ViewModel
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.LiveData { *; }

# 保持 Kotlin 元数据
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes EnclosingMethod

# 保持所有使用 @Keep 注解的类
-keep @androidx.annotation.Keep class * { *; }

# 防止 WindowManager 相关类被混淆
-keep class android.view.WindowManager { *; }
-keep class android.view.WindowManager$LayoutParams { *; }

# 防止通知相关类被混淆
-keep class android.app.Notification { *; }
-keep class android.app.NotificationChannel { *; }
-keep class android.app.NotificationManager { *; }
-keep class androidx.core.app.NotificationCompat { *; }

# 保持所有矢量图资源
-keep class android.graphics.drawable.VectorDrawable { *; }
-keep class androidx.vectordrawable.graphics.drawable.VectorDrawableCompat { *; }

# 保持 R 类中的资源 ID
-keepclassmembers class **.R$* {
    public static <fields>;
}

# 保持 Fragment
-keep class * extends androidx.fragment.app.Fragment { *; }

# 保持 RecyclerView Adapter
-keep class * extends androidx.recyclerview.widget.RecyclerView.Adapter { *; }

# 保持所有 drawable 资源引用
-keep class **.R$drawable {
    public static <fields>;
}

# 防止 ImageView 相关属性被混淆
-keepclassmembers class android.widget.ImageView {
    public void setImageDrawable(android.graphics.drawable.Drawable);
    public void setImageResource(int);
}

# DataBinding / ViewBinding
-keep class * extends androidx.databinding.DataBindingComponent { *; }
-keep class * extends androidx.databinding.ViewDataBinding { *; }
-keep class * extends androidx.viewbinding.ViewBinding { *; }
-keepclassmembers class * extends androidx.databinding.ViewDataBinding {
    public static ** bind(android.view.View);
    public static ** inflate(android.view.LayoutInflater);
    public static ** inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
}
-keepclassmembers class * extends androidx.viewbinding.ViewBinding {
    public static ** bind(android.view.View);
    public static ** inflate(android.view.LayoutInflater);
    public static ** inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
}