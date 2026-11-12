# Low-light assistance — 2026-11-12

SD-01/03/05/08. Deterministic preprocessing, not new model training.

Original luminance is measured from the camera Y plane (video-range normalization)
and, when a recent face exists, from its unmodified RGB region. The face region
expires after two seconds and is discarded on rotation changes. Black/flat dark
input is insufficient evidence. A LOW scene uses a fixed gamma .65 curve with
maximum channel gain 3; black remains black. Five clearly bright frames are
required to leave enhancement. ML Kit and Face Landmarker share the SAME upright
processed bitmap; the preview remains the camera preview. Side-eye references
reset on enhancement transitions and unusable evidence. Frames/proxies/bitmaps
are released when asynchronous processing completes. No images are stored.

Insufficient light produces unknown eye evidence, a specific English message,
and the existing visibility-loss alarm after 3 seconds. It is never labeled as
sleep, never counts toward repeated closures, and cannot trigger assistant
recovery. Existing 15-second held alarms still take precedence.

## Offline verification plan (before stress results)

- Test padded Y buffers, histogram percentiles, monotonic bounded tone curve,
  black input, hysteresis, unknown evidence and alarm/recovery behavior.
- Use 24 deterministic development/train images: first four SHA-256-ranked
  frames in each front/left/right × open/closed group from the prior calibration
  TRAIN manifest. Do not read reserved test images or tune thresholds.
- Original, synthetic exposure factors .30 and .15, and all-black control.
  Compare frozen Face Landmarker landmark coverage before/after the SAME curve.
  These are artificial degradation checks, NOT real night accuracy or 80/20 training.
- Build release APK and run Flutter/Kotlin/Python regressions. Phone unavailable
  today: no on-device FPS, memory, night accuracy or alarm timing acceptance claim.
- Future real night acceptance must include parked Galaxy A51, frontal and both
  side angles, open/closed eyes, tunnel/headlight transitions, noise/motion blur,
  false alarms, latency and thermal/battery behavior. New training requires a
  fresh subject-disjoint 80/20 split; historical test subjects remain used.

## Limits and sources

No RGB enhancement can restore details absent from the captured frame. Actual
night operation is unverified. No flash, screen brightness override or hidden
camera behavior was introduced. This does not claim infrared night vision.

Android Low Light Boost AE requires supported Android 15+ hardware; it is not
assumed to exist on Galaxy A51. The current CameraX dependency is unchanged.
Official references: https://developer.android.com/media/camera/lowlight/low-light-boost-ae
and https://developer.android.com/media/camera/camerax/configuration


## Recorded result

40 Kotlin tests, 41 Python tests + 6 subtests, and 7 Flutter tests passed.
Flutter analyzer reported no issues and the arm64 release APK built successfully.
Synthetic stress: 24/24 face/eye-geometry detections in original, .30 and .15
exposure variants with and without enhancement. Black controls: 0/24.
No coverage gain was demonstrated on this small development set; no open/closed
accuracy, real night performance or device latency claim follows from it.
[Machine-readable report](experiments/low-light-stress-2026-11-12.json).
Phone testing was unavailable; the APK was not installed in this session.
