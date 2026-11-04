# ML Kit discovers Firebase component registrars by reflection.
# R8 must retain their public no-argument constructors in optimized APKs.
# https://issuetracker.google.com/issues/574078150
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    public <init>();
}
