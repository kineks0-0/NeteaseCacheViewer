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

# api包下的类混淆会导致无法正常反射
-keep class io.github.kineks.neteaseviewer.data.api.** {*;}
-dontwarn io.github.kineks.neteaseviewer.**
-keep interface io.github.kineks.neteaseviewer.api.** { *; }


-keepnames class io.github.kineks.neteaseviewer.data.** {*;}
-dontwarn com.google.android.exoplayer2.**