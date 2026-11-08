import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import '../models/monitor_state.dart';
import '../services/native_monitor.dart';

class MonitorController extends ChangeNotifier {
  MonitorController(this._native);
  final NativeMonitor _native;
  StreamSubscription<MonitorState>? _subscription;
  MonitorState state = const MonitorState();
  bool busy = false;
  bool waitingForChatGpt = false;
  bool voice = true;
  bool llm = false;
  bool chatGptVerified = false;
  String? chatGptEmail;
  String? error;
  bool _disposed = false;
  bool _initialized = false;
  void _changed() {
    if (!_disposed) notifyListeners();
  }

  Future<void> initialize() async {
    if (_initialized) return;
    _initialized = true;
    _subscription = _native.states.listen(
      (value) {
        state = value;
        _changed();
      },
      onError: (Object failure) {
        error = 'Monitoring disconnected. Reopen the app.';
        _changed();
      },
    );
    await _perform(() async {
      state = await _native.currentState();
      final prefs = await _native.preferences();
      voice = prefs['voice'] as bool? ?? true;
      llm = prefs['llm'] as bool? ?? false;
      chatGptEmail = await _native.chatGptAccount();
    });
  }

  Future<void> _perform(Future<void> Function() action) async {
    if (busy || _disposed) return;
    busy = true;
    error = null;
    _changed();
    try {
      await action();
    } on PlatformException catch (failure) {
      error = failure.message ?? 'Could not complete the action.';
    } catch (_) {
      error = 'Could not complete the action. Reopen the app.';
    } finally {
      busy = false;
      _changed();
    }
  }

  Future<void> toggleMonitoring() =>
      _perform(state.active ? _native.stop : _native.start);
  Future<void> testAlarm() => _perform(_native.testAlarm);
  Future<void> signInChatGpt() => _perform(() async {
    waitingForChatGpt = true;
    _changed();
    try {
      chatGptEmail = await _native.signInChatGpt();
    } finally {
      waitingForChatGpt = false;
    }
    if (chatGptEmail == null) return;
    await _native.setPreferences(voice: true, llm: true);
    voice = true;
    llm = true;
    chatGptVerified = false;
  });

  Future<void> cancelChatGptSignIn() => _native.cancelChatGptSignIn();

  Future<void> signOutChatGpt() => _perform(() async {
    final revoked = await _native.signOutChatGpt();
    chatGptEmail = null;
    await _native.setPreferences(voice: voice, llm: false);
    llm = false;
    if (revoked != true) {
      error =
          'Signed out on this phone. Remote revocation could not be confirmed.';
    }
  });

  Future<void> connectChatGpt() => _perform(() async {
    chatGptVerified = false;
    await _native.connectChatGpt();
    await _native.setPreferences(voice: voice, llm: true);
    llm = true;
    chatGptVerified = true;
  });

  Future<void> updatePreferences({bool? voiceEnabled, bool? llmEnabled}) =>
      _perform(() async {
        final nextVoice = voiceEnabled ?? voice;
        final nextLlm = llmEnabled ?? llm;
        await _native.setPreferences(voice: nextVoice, llm: nextLlm);
        voice = nextVoice;
        llm = nextLlm;
        if (!nextLlm) chatGptVerified = false;
      });
  @override
  void dispose() {
    _disposed = true;
    _subscription?.cancel();
    super.dispose();
  }
}
