class MonitorState {
  const MonitorState({
    this.status = 'stopped',
    this.message = 'Ready to monitor',
    this.active = false,
    this.eyesOpen,
    this.sideEye = false,
    this.closedMs = 0,
    this.yaw,
    this.pitch,
    this.perclos,
    this.alarm = false,
    this.alerts = 0,
    this.assistant = 'Voice reminders',
  });

  factory MonitorState.fromMap(Map<dynamic, dynamic> map) => MonitorState(
    status: map['status'] as String? ?? 'stopped',
    message: map['message'] as String? ?? '',
    active: map['active'] as bool? ?? false,
    eyesOpen: (map['eyesOpen'] as num?)?.toDouble(),
    sideEye: map['sideEye'] as bool? ?? false,
    closedMs: (map['closedMs'] as num?)?.toInt() ?? 0,
    yaw: (map['yaw'] as num?)?.toDouble(),
    pitch: (map['pitch'] as num?)?.toDouble(),
    perclos: (map['perclos'] as num?)?.toDouble(),
    alarm: map['alarm'] as bool? ?? false,
    alerts: (map['alerts'] as num?)?.toInt() ?? 0,
    assistant: map['assistant'] as String? ?? 'Voice reminders',
  );
  final String status;
  final String message;
  final bool active;
  final double? eyesOpen;
  final bool sideEye;
  final int closedMs;
  final double? yaw;
  final double? pitch;
  final double? perclos;
  final bool alarm;
  final int alerts;
  final String assistant;

  String get eyeReading {
    final openness = eyesOpen;
    if (openness == null) return '—';
    if (sideEye) return openness > 0 ? 'Open' : 'Closed';
    return '${(openness * 100).round()}%';
  }

  String get title => switch (status) {
    'starting' => 'Starting camera',
    'monitoring' => 'Monitoring active',
    'limited' => 'Limited visibility',
    'warning' => 'Check your position',
    'drowsy' => 'Pull over and rest',
    'error' => 'Monitoring stopped',
    _ => 'Before you drive',
  };
}
