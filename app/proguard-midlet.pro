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
-keep public class org.microemu.** { public protected *; }
-keep class ru.playsoftware.j2meloader.util.SparseIntArrayAdapter { *; }
# Keep the BuildConfig
-keep class ru.playsoftware.j2meloader.BuildConfig { *; }

-keep class com.arthenica.mobileffmpeg.** { *; }
-keep class ru.playsoftware.j2meloader.crashes.models.* { *; }

# Preserve all public midlets.

-keep public class * extends javax.microedition.midlet.MIDlet

# Preserve all native method names and the names of their classes.

-keepclasseswithmembernames class * {
    native <methods>;
}

# Your midlet may contain more items that need to be preserved;
# typically classes that are dynamically created using Class.forName:

# -keep public class mypackage.MyClass
# -keep public interface mypackage.MyInterface
# -keep public class * implements mypackage.MyInterface