# ML Kit discovers Firebase component registrars by reflection.
# R8 must retain their public no-argument constructors in optimized APKs.
# https://issuetracker.google.com/issues/574078150
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    public <init>();
}

# Protobuf-lite reads generated fields by their original names at runtime.
# MediaPipe model initialization fails if R8 removes fields such as platform_.
-keep class * extends com.google.protobuf.GeneratedMessageLite {
    *;
}

# MediaPipe's logger discovers its caller from stack frames.
# Its factories must survive R8 inlining and class merging.
-keep class com.google.common.flogger.** {
    *;
}

# Native Graph callbacks and face-result converters are reached through JNI.
-keep class com.google.mediapipe.framework.** {
    *;
}
-keep class com.google.mediapipe.tasks.vision.facelandmarker.** {
    *;
}

# These optional Graph template/profiler APIs are not shipped in Tasks Vision
# and are not used by SafeDriveAI's face-landmark graph.
-dontwarn com.google.mediapipe.proto.CalculatorProfileProto$CalculatorProfile
-dontwarn com.google.mediapipe.proto.GraphTemplateProto$CalculatorGraphTemplate
