import 'package:flutter_test/flutter_test.dart';
import 'package:safedrive_ai/models/monitor_state.dart';

void main() {
  test('lighting evidence survives native state mapping', () {
    final dark = MonitorState.fromMap({
      'status': 'limited',
      'lighting': 'insufficient',
    });
    expect(dark.lighting, 'insufficient');
    expect(dark.eyeReading, '—');
    expect(MonitorState.fromMap({}).lighting, 'normal');
  });
  test('eye score stays continuous at frontal and side angles', () {
    for (final side in [false, true]) {
      expect(MonitorState(eyesOpen: 0.82, sideEye: side).eyeReading, '82%');
      expect(MonitorState(eyesOpen: 0.48, sideEye: side).eyeReading, '48%');
      expect(MonitorState(sideEye: side).eyeReading, '—');
      expect(MonitorState(eyesOpen: double.nan, sideEye: side).eyeReading, '—');
      expect(MonitorState(eyesOpen: 1.1, sideEye: side).eyeReading, '—');
    }
  });
}
