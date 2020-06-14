# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\tools\adt-bundle-windows-x86_64-20131030\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-keep class javax.** { *; }
-keep class com.siemens.mp.** { *; }
-keep class com.samsung.util.** { *; }
-keep class com.sonyericsson.accelerometer.** { *; }
-keep class com.sprintpcs.media.** { *; }
-keep class com.mascotcapsule.micro3d.v3.** { *; }
-keep class com.motorola.funlight.** { *; }
-keep class com.nokia.mid.** { *; }
-keep class com.sun.midp.midlet.** { *; }
-keep class com.vodafone.util.** { *; }
-keep class org.microemu.** { *; }
# Keep the BuildConfig
-keep class ru.playsoftware.j2meloader.BuildConfig { *; }

-keep class android.support.v7.widget.SearchView { *; }
-keep class com.arthenica.mobileffmpeg.** { *; }
-keep class ru.playsoftware.j2meloader.crashes.AppCenterAPI** { *; }

-dontnote android.net.http.*
-dontnote org.apache.commons.codec.**
-dontnote org.apache.http.**
