-keep public class org.microemu.** { public protected *; }
-keep class ru.playsoftware.j2meloader.util.SparseIntArrayAdapter { *; }
-keep class ru.playsoftware.j2meloader.BuildConfig { *; }

-keep class com.arthenica.mobileffmpeg.** { *; }
-keep class ru.playsoftware.j2meloader.crashes.models.* { *; }

# Preserve all public midlets.
-keep public class * extends javax.microedition.midlet.MIDlet

# Preserve all native method names and the names of their classes.
-keepclasseswithmembernames class * {
    native <methods>;
}
