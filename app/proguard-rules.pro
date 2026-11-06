# The app's own classes are kept as they are: libraries still get shrunk and optimised,
# and nothing that is stored by name (Room entities, backups, preferences) can be renamed.
-keep class app.tsuzuku.** { *; }
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
-dontwarn org.slf4j.**
