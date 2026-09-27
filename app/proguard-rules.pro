-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
}

-assumenosideeffects class java.util.Objects {
    ** requireNonNull(...);
}

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

-keep class com.aurora.AuroraApplication { *; }
-keep class com.aurora.AuroraManagerProvider { *; }

-keep class com.aurora.adb.** { *; }
-keepnames class com.aurora.adb.**
-keepclassmembers class com.aurora.adb.** { *; }

-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

-keep class org.conscrypt.** { *; }
-dontwarn org.conscrypt.**

-keep class org.lsposed.hiddenapibypass.** { *; }

-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-keepnames class moe.shizuku.api.BinderContainer

-keep class androidx.** { *; }
-dontwarn androidx.**

-allowaccessmodification
-repackageclasses aurora.x
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile