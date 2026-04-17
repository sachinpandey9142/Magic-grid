# Proguard rules for Magic Grid

# Kotlin
-keep class kotlin.** { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Keep all our game classes
-keep class com.magicgrid.** { *; }
-keepclassmembers class com.magicgrid.** { *; }

# AndroidX
-keep class androidx.** { *; }
-keepclassmembers class androidx.** { *; }

# Android framework
-keep class android.** { *; }

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep parcelables
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
