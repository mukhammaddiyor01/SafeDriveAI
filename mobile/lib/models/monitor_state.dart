class MonitorState {
  const MonitorState({
    this.status = 'stopped',
    this.message = 'Kuzatuvni boshlashga tayyor',
    this.active = false,
    this.eyesOpen,
    this.closedMs = 0,
    this.yaw,
    this.pitch,
    this.perclos,
    this.alarm = false,
    this.alerts = 0,
    this.assistant = 'Ovozli eslatmalar',
  });

  factory MonitorState.fromMap(Map<dynamic, dynamic> map) => MonitorState(
    status: map['status'] as String? ?? 'stopped',
    message: map['message'] as String? ?? '',
    active: map['active'] as bool? ?? false,
    eyesOpen: (map['eyesOpen'] as num?)?.toDouble(),
    closedMs: (map['closedMs'] as num?)?.toInt() ?? 0,
    yaw: (map['yaw'] as num?)?.toDouble(),
    pitch: (map['pitch'] as num?)?.toDouble(),
    perclos: (map['perclos'] as num?)?.toDouble(),
    alarm: map['alarm'] as bool? ?? false,
    alerts: (map['alerts'] as num?)?.toInt() ?? 0,
    assistant: map['assistant'] as String? ?? 'Ovozli eslatmalar',
  );
  final String status;
  final String message;
  final bool active;
  final double? eyesOpen;
  final int closedMs;
  final double? yaw;
  final double? pitch;
  final double? perclos;
  final bool alarm;
  final int alerts;
  final String assistant;

  String get title => switch (status) {
    'starting' => 'Kamera ochilmoqda',
    'monitoring' => 'Kuzatuv faol',
    'limited' => 'Ko‘rinish cheklangan',
    'warning' => 'Diqqat talab etiladi',
    'drowsy' => 'To‘xtab, dam oling',
    'error' => 'Kuzatuv to‘xtadi',
    _ => 'Safarga tayyorlaning',
  };
}
