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

-keepclasseswithmembernames class * {
    native <methods>;
}

-allowaccessmodification
-ignorewarnings
-dontnote
-dontwarn **
-optimizations !code/simplification/arithmetic
-optimizationpasses 5
-overloadaggressively

-keep class rikka.shizuku.ShizukuProvider { *; }
-keep class rikka.shizuku.ShizukuProvider$* { *; }
-keep class moe.shizuku.api.BinderContainer { *; }
-keep class moe.shizuku.api.BinderContainer$* { *; }