# javax.annotation
-dontwarn javax.annotation.processing.AbstractProcessor
-dontwarn javax.annotation.processing.SupportedOptions
-dontwarn javax.annotation.processing.Processor

# Gson
### The following rules are needed for R8 in "full mode" which only adheres to `-keepattribtues` if
### the corresponding class or field is matches by a `-keep` rule as well, see
### https://r8.googlesource.com/r8/+/refs/heads/master/compatibility-faq.md#r8-full-mode

# Keep class TypeToken (respectively its generic signature)
-keep class com.google.gson.reflect.TypeToken { *; }

# Keep any (anonymous) classes extending TypeToken
-keep class * extends com.google.gson.reflect.TypeToken

# Keep classes with @JsonAdapter annotation
-keep @com.google.gson.annotations.JsonAdapter class *

# Keep fields with @SerializedName annotation, but allow obfuscation of their names
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Keep fields with any other Gson annotation
-keepclassmembers class * {
  @com.google.gson.annotations.Expose <fields>;
  @com.google.gson.annotations.JsonAdapter <fields>;
  @com.google.gson.annotations.Since <fields>;
  @com.google.gson.annotations.Until <fields>;
}

# Keep no-args constructor of classes which can be used with @JsonAdapter
# By default their no-args constructor is invoked to create an adapter instance
-keep class * extends com.google.gson.TypeAdapter {
  <init>();
}
-keep class * implements com.google.gson.TypeAdapterFactory {
  <init>();
}
-keep class * implements com.google.gson.JsonSerializer {
  <init>();
}
-keep class * implements com.google.gson.JsonDeserializer {
  <init>();
}
