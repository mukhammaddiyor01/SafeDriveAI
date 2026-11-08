import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:safedrive_ai/controllers/monitor_controller.dart';
import 'package:safedrive_ai/models/monitor_state.dart';
import 'package:safedrive_ai/services/native_monitor.dart';

class FakeMonitor implements NativeMonitor {
  final events = StreamController<MonitorState>.broadcast();
  int starts = 0;
  bool denyPermission = false;
  bool? coachEnabled;
  @override
  Stream<MonitorState> get states => events.stream;
  @override
  Future<MonitorState> currentState() async => const MonitorState();
  @override
  Future<Map<dynamic, dynamic>> preferences() async => {
    'voice': true,
    'llm': false,
  };
  @override
  Future<void> start() async {
    if (denyPermission) {
      throw PlatformException(code: 'camera', message: 'Kamera ruxsati kerak');
    }
    starts++;
    events.add(const MonitorState(status: 'monitoring', active: true));
  }

  @override
  Future<void> stop() async => events.add(const MonitorState());
  @override
  Future<void> testAlarm() async {}
  @override
  Future<void> setGeminiLive(bool enabled) async {}
  @override
  Future<void> connectChatGpt() async {}
  @override
  Future<String?> chatGptAccount() async => null;
  @override
  Future<String?> signInChatGpt() async => 'Driver';
  @override
  Future<void> cancelChatGptSignIn() async {}
  @override
  Future<void> completeChatGptSignIn(String address) async {}
  @override
  Future<bool?> signOutChatGpt() async => true;
  @override
  Future<void> setPreferences({required bool voice, required bool llm}) async {
    coachEnabled = llm;
  }
}

void main() {
  test(
    'phone account enables the voice coach and sign-out disables it',
    () async {
      final native = FakeMonitor();
      final controller = MonitorController(native);
      await controller.initialize();

      await controller.signInChatGpt();
      expect(controller.chatGptEmail, 'Driver');
      expect(native.coachEnabled, true);

      await controller.signOutChatGpt();
      expect(controller.chatGptEmail, isNull);
      expect(native.coachEnabled, false);

      controller.dispose();
      await native.events.close();
    },
  );

  test('permission denial clears busy and allows retry', () async {
    final native = FakeMonitor();
    final controller = MonitorController(native);
    await controller.initialize();
    native.denyPermission = true;
    await controller.toggleMonitoring();
    expect(controller.busy, false);
    expect(controller.error, 'Kamera ruxsati kerak');
    native.denyPermission = false;
    await controller.toggleMonitoring();
    await Future<void>.delayed(Duration.zero);
    expect(native.starts, 1);
    expect(controller.state.active, true);
    controller.dispose();
    await native.events.close();
  });
  test('service events update state and stop works', () async {
    final native = FakeMonitor();
    final controller = MonitorController(native);
    await controller.initialize();
    await controller.toggleMonitoring();
    await Future<void>.delayed(Duration.zero);
    native.events.add(
      const MonitorState(status: 'drowsy', active: true, alarm: true),
    );
    await Future<void>.delayed(Duration.zero);
    expect(controller.state.title, 'Pull over and rest');
    await controller.toggleMonitoring();
    await Future<void>.delayed(Duration.zero);
    expect(controller.state.active, false);
    controller.dispose();
    await native.events.close();
  });
}
