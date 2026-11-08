import 'dart:convert';
import 'dart:io';

import 'package:flutter/services.dart';

import '../models/monitor_state.dart';

abstract interface class NativeMonitor {
  Stream<MonitorState> get states;
  Future<MonitorState> currentState();
  Future<Map<dynamic, dynamic>> preferences();
  Future<void> start();
  Future<void> stop();
  Future<void> testAlarm();
  Future<void> connectChatGpt();
  Future<String?> chatGptAccount();
  Future<String?> signInChatGpt();
  Future<void> cancelChatGptSignIn();
  Future<void> completeChatGptSignIn(String address);
  Future<bool?> signOutChatGpt();
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
  Future<String?> chatGptAccount() =>
      _methods.invokeMethod<String>('chatGptAccount');
  @override
  Future<String?> signInChatGpt() =>
      _methods.invokeMethod<String>('signInChatGpt');
  @override
  Future<void> cancelChatGptSignIn() =>
      _methods.invokeMethod<void>('cancelChatGptSignIn');
  @override
  Future<void> completeChatGptSignIn(String address) => _methods
      .invokeMethod<void>('completeChatGptSignIn', {'address': address});
  @override
  Future<bool?> signOutChatGpt() =>
      _methods.invokeMethod<bool>('signOutChatGpt');
  @override
  Future<void> connectChatGpt() async {
    final client = HttpClient();
    try {
      await (() async {
        final request = await client.post('127.0.0.1', 8765, '/connect');
        final response = await request.close();
        final body = await response.transform(utf8.decoder).join();
        if (response.statusCode != HttpStatus.ok ||
            (jsonDecode(body) as Map<String, dynamic>)['conversation_id'] ==
                null) {
          throw const FormatException('ChatGPT connection unavailable');
        }
      })().timeout(const Duration(seconds: 10));
    } on SocketException {
      throw PlatformException(
        code: 'coach_connection',
        message: 'Start the Mac assistant and reconnect the ADB tunnel.',
      );
    } catch (_) {
      throw PlatformException(
        code: 'coach_connection',
        message: 'ChatGPT connection failed. Check the OpenAI key and model in the Mac assistant terminal.',
      );
    } finally {
      client.close(force: true);
    }
  }

  @override
  Future<void> setPreferences({required bool voice, required bool llm}) =>
      _methods.invokeMethod<void>('preferences', {'voice': voice, 'llm': llm});
}
