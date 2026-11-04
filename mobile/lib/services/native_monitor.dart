import 'package:flutter/services.dart';

import '../models/monitor_state.dart';

abstract interface class NativeMonitor {
  Stream<MonitorState> get states;
  Future<MonitorState> currentState();
  Future<Map<dynamic, dynamic>> preferences();
  Future<void> start();
  Future<void> stop();
  Future<void> testAlarm();
  Future<void> setPreferences({required bool voice, required bool llm});
}

class MethodChannelMonitor implements NativeMonitor {
  static const _methods = MethodChannel('uz.safedrive/monitor');
  static const _events = EventChannel('uz.safedrive/states');
  @override
  Stream<MonitorState> get states => _events.receiveBroadcastStream().map(
    (event) => MonitorState.fromMap(event as Map<dynamic, dynamic>),
  );
  @override
  Future<MonitorState> currentState() async => MonitorState.fromMap(
    await _methods.invokeMapMethod<dynamic, dynamic>('state') ?? {},
  );
  @override
  Future<Map<dynamic, dynamic>> preferences() async =>
      await _methods.invokeMapMethod<dynamic, dynamic>('preferences') ?? {};
  @override
  Future<void> start() => _methods.invokeMethod<void>('start');
  @override
  Future<void> stop() => _methods.invokeMethod<void>('stop');
  @override
  Future<void> testAlarm() => _methods.invokeMethod<void>('testAlarm');
  @override
  Future<void> setPreferences({required bool voice, required bool llm}) =>
      _methods.invokeMethod<void>('preferences', {'voice': voice, 'llm': llm});
}
