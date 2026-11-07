# The app's own classes are kept as they are: libraries still get shrunk and optimised,
# and nothing that is stored by name (Room entities, backups, preferences) can be renamed.
-keep class app.tsuzuku.** { *; }
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
-dontwarn org.slf4j.**

# WorkManager creates input mergers and workers by reflection. Without these rules R8 strips
# OverwritingInputMerger's constructor, no worker can start, and every Glance widget stays a
# blank loading box (Glance renders widgets inside a WorkManager worker).
-keep class * extends androidx.work.InputMerger { public <init>(); }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
