# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Applications/Android Studio.app/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

-keep public class org.dwallach.calwatch.CalWatchFaceService
-keep public class org.dwallach.calwatch.CalWatchFaceService.Engine
-keep public class org.dwallach.calwatch.WearReceiverService
-keep public class org.dwallach.calwatch.XWatchfaceReceiver
-keep public class org.dwallach.calwatch.proto.**

-keep class com.squareup.wire.** { *; }

-dontwarn okio.**

-dontobfuscate
