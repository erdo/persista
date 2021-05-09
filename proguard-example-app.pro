# https://github.com/Kotlin/kotlinx.serialization/blob/master/README.md#android

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class foo.bar.example.**$$serializer { *; }
-keepclassmembers class foo.bar.example.** {
    *** Companion;
}
-keepclasseswithmembers class foo.bar.example.** {
    kotlinx.serialization.KSerializer serializer(...);
}




#
#
#
#
#-keepclassmembers,allowoptimization class foo.bar.example.** {
#    *** Companion;
#}
#-keepclassmembers,allowoptimization class foo.bar.example.** {
#    kotlinx.serialization.KSerializer serializer(...);
#}
#
#
#
#
#
## Kotlin serialization looks up the generated serializer classes through a function on companion
## objects. The companions are looked up reflectively so we need to explicitly keep these functions.
#-keepclasseswithmembers class **.*$Companion {
#    kotlinx.serialization.KSerializer serializer(...);
#}
## If a companion has the serializer function, keep the companion field on the original type so that
## the reflective lookup succeeds.
#-if class **.*$Companion {
#  kotlinx.serialization.KSerializer serializer(...);
#}
#-keepclassmembers class <1>.<2> {
#  <1>.<2>$Companion Companion;
#}



