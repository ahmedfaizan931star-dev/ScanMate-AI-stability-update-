# ScanMate AI Pro safe release rules.
# Release R8 is enabled for v1.5.0. These rules keep scanner/OCR/Room/Compose integrations stable.

-keep class com.synthbyte.scanmate.data.** { *; }
-keep class com.synthbyte.scanmate.domain.** { *; }
-keep class com.google.zxing.** { *; }
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-keep class androidx.camera.** { *; }
-keep class androidx.room.** { *; }
-keep class com.squareup.moshi.** { *; }
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes InnerClasses,EnclosingMethod
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.**
-dontwarn okio.**
-keep class com.synthbyte.scanmate.widgets.** { *; }
-keep class * extends android.appwidget.AppWidgetProvider { *; }
