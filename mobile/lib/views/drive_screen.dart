import 'package:flutter/material.dart';

import '../controllers/monitor_controller.dart';
import '../models/monitor_state.dart';
import 'camera_panel.dart';
import 'head_angle_indicator.dart';
import 'settings_sheet.dart';
import 'theme.dart';

class DriveScreen extends StatefulWidget {
  const DriveScreen({super.key, required this.controller});
  final MonitorController controller;
  @override
  State<DriveScreen> createState() => _DriveScreenState();
}

class _DriveScreenState extends State<DriveScreen> {
  @override
  void initState() {
    super.initState();
    widget.controller.initialize();
  }

  @override
  void dispose() {
    widget.controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: widget.controller,
    builder: (context, _) {
      final controller = widget.controller;
      final state = controller.state;
      return Scaffold(
        bottomNavigationBar: SafeArea(
          top: false,
          child: Center(
            heightFactor: 1,
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 560),
              child: Padding(
                padding: const EdgeInsets.fromLTRB(18, 6, 18, 8),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: FilledButton.icon(
                            onPressed: controller.busy
                                ? null
                                : controller.toggleMonitoring,
                            icon: Icon(
                              state.active
                                  ? Icons.stop_rounded
                                  : Icons.play_arrow_rounded,
                            ),
                            label: FittedBox(
                              fit: BoxFit.scaleDown,
                              child: Text(
                                controller.busy
                                    ? 'Please wait…'
                                    : state.active
                                    ? 'Stop monitoring'
                                    : 'Start monitoring',
                              ),
                            ),
                            style: FilledButton.styleFrom(
                              backgroundColor: DriveColors.ink,
                            ),
                          ),
                        ),
                        if (!state.active) ...[
                          const SizedBox(width: 10),
                          IconButton.filledTonal(
                            tooltip: 'Test alarm',
                            onPressed: controller.busy
                                ? null
                                : controller.testAlarm,
                            icon: const Icon(Icons.volume_up_outlined),
                            style: IconButton.styleFrom(
                              minimumSize: const Size(54, 54),
                            ),
                          ),
                        ],
                      ],
                    ),
                    const SizedBox(height: 5),
                    const Text(
                      'If sleepy, pull over safely and rest.',
                      textAlign: TextAlign.center,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(fontSize: 11),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
        body: SafeArea(
          bottom: false,
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 900),
              child: LayoutBuilder(
                builder: (context, constraints) {
                  final compact = constraints.maxHeight < 620;
                  final landscape =
                      constraints.maxWidth > constraints.maxHeight;
                  final gap = compact ? 6.0 : 12.0;
                  final limited = {'limited', 'warning'}.contains(state.status);
                  final status = Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Semantics(
                        liveRegion: true,
                        child: FittedBox(
                          fit: BoxFit.scaleDown,
                          alignment: Alignment.centerLeft,
                          child: Text(
                            state.title,
                            style: Theme.of(context).textTheme.headlineLarge
                                ?.copyWith(fontSize: compact ? 23 : 28),
                          ),
                        ),
                      ),
                      SizedBox(height: gap),
                      _StatusMessage(state: state, compact: compact),
                      if (controller.error != null) ...[
                        SizedBox(height: gap),
                        Tooltip(
                          message: controller.error!,
                          child: Text(
                            controller.error!,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              color: DriveColors.red,
                              fontSize: 12,
                            ),
                          ),
                        ),
                      ],
                    ],
                  );
                  final readings = Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      HeadAngleIndicator(
                        yaw: state.yaw,
                        limited: limited || state.eyesOpen == null,
                      ),
                      SizedBox(height: gap),
                      Row(
                        children: [
                          Expanded(
                            child: Tooltip(
                              message: 'Estimated eye-open score, not a physical eyelid measurement.',
                              child: _ReadingTile(
                                icon: Icons.visibility_outlined,
                                label: 'Eye-open score',
                                value: state.eyeReading,
                                color: state.eyesOpen == null
                                    ? DriveColors.muted
                                    : DriveColors.blue,
                              ),
                            ),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: _ReadingTile(
                              icon: Icons.notifications_none_rounded,
                              label: 'Alerts',
                              value: '${state.alerts}',
                              color: state.alerts > 0
                                  ? DriveColors.amber
                                  : DriveColors.blue,
                            ),
                          ),
                        ],
                      ),
                    ],
                  );
                  return Padding(
                    padding: EdgeInsets.fromLTRB(18, compact ? 2 : 8, 18, 6),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        _Header(controller: controller),
                        SizedBox(height: gap),
                        if (controller.geminiLive ||
                            state.assistant.startsWith('Gemini Live')) ...[
                          _GeminiStrip(state: state),
                          SizedBox(height: gap),
                        ],
                        if (landscape)
                          Expanded(
                            child: Row(
                              crossAxisAlignment: CrossAxisAlignment.stretch,
                              children: [
                                Expanded(child: CameraPanel(state: state)),
                                const SizedBox(width: 14),
                                Expanded(
                                  child: LayoutBuilder(
                                    builder: (context, panel) => FittedBox(
                                      fit: BoxFit.scaleDown,
                                      alignment: Alignment.topCenter,
                                      child: SizedBox(
                                        width: panel.maxWidth,
                                        child: Column(
                                          mainAxisSize: MainAxisSize.min,
                                          children: [
                                            status,
                                            if (state.active) ...[
                                              SizedBox(height: gap),
                                              readings,
                                            ],
                                          ],
                                        ),
                                      ),
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          )
                        else ...[
                          status,
                          SizedBox(height: gap),
                          Expanded(child: CameraPanel(state: state)),
                          if (state.active) ...[
                            SizedBox(height: gap),
                            readings,
                          ],
                        ],
                      ],
                    ),
                  );
                },
              ),
            ),
          ),
        ),
      );
    },
  );
}

class _Header extends StatelessWidget {
  const _Header({required this.controller});
  final MonitorController controller;

  @override
  Widget build(BuildContext context) => Row(
    children: [
      const Icon(Icons.shield_outlined, color: DriveColors.blue, size: 25),
      const SizedBox(width: 7),
      Expanded(
        child: Text(
          'SafeDriveAI',
          style: Theme.of(context).textTheme.titleLarge?.copyWith(fontSize: 20),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
      ),
      const Tooltip(
        message: 'Loud alarm',
        child: Icon(
          Icons.volume_up_outlined,
          color: DriveColors.blue,
          size: 21,
        ),
      ),
      const SizedBox(width: 10),
      const Tooltip(
        message: 'Car audio when connected',
        child: Icon(Icons.bluetooth, color: DriveColors.blue, size: 21),
      ),
      IconButton(
        tooltip: 'Settings',
        icon: const Icon(Icons.tune_rounded),
        onPressed: () => showModalBottomSheet<void>(
          context: context,
          isScrollControlled: true,
          showDragHandle: true,
          builder: (_) => SettingsSheet(controller: controller),
        ),
      ),
    ],
  );
}

class _GeminiStrip extends StatelessWidget {
  const _GeminiStrip({required this.state});
  final MonitorState state;

  @override
  Widget build(BuildContext context) {
    final connecting = state.assistant == 'Gemini Live connecting';
    final active = state.assistant == 'Gemini Live active';
    final failed =
        state.assistant == 'Gemini Live unavailable' ||
        state.assistant == 'Gemini Live needs microphone permission';
    final label = state.assistant.startsWith('Gemini Live')
        ? state.assistant
        : 'Gemini Live ready';
    final color = failed ? DriveColors.red : DriveColors.blue;
    return Semantics(
      liveRegion: true,
      child: Container(
        key: const ValueKey('gemini-panel'),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: color.withValues(alpha: 0.07),
          borderRadius: BorderRadius.circular(14),
        ),
        child: Row(
          children: [
            CircleAvatar(
              radius: 17,
              backgroundColor: color,
              child: connecting
                  ? const SizedBox(
                      width: 17,
                      height: 17,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : Icon(
                      active ? Icons.graphic_eq : Icons.mic_none_rounded,
                      color: Colors.white,
                      size: 21,
                    ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Tooltip(
                message: label,
                child: Text(
                  label,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    color: color,
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _StatusMessage extends StatelessWidget {
  const _StatusMessage({required this.state, required this.compact});
  final MonitorState state;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final warning = {'warning', 'limited'}.contains(state.status);
    final danger = state.status == 'drowsy' || state.status == 'error';
    final color = danger
        ? DriveColors.red
        : warning
        ? DriveColors.amber
        : DriveColors.blue;
    final message = state.status == 'monitoring' && state.sideEye
        ? 'Visible eye detected'
        : state.message;
    return Tooltip(
      message: message,
      child: Container(
        padding: warning || danger
            ? EdgeInsets.all(compact ? 8 : 10)
            : EdgeInsets.zero,
        decoration: BoxDecoration(
          color: warning || danger
              ? color.withValues(alpha: 0.08)
              : Colors.transparent,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(
              danger
                  ? Icons.warning_amber_rounded
                  : warning
                  ? Icons.visibility_off_outlined
                  : Icons.visibility_outlined,
              color: color,
              size: 21,
            ),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                message,
                maxLines: compact ? 2 : 3,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  color: warning || danger ? color : DriveColors.muted,
                  fontSize: 13,
                  height: 1.25,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ReadingTile extends StatelessWidget {
  const _ReadingTile({
    required this.icon,
    required this.label,
    required this.value,
    required this.color,
  });
  final IconData icon;
  final String label;
  final String value;
  final Color color;

  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
    decoration: BoxDecoration(
      border: Border.all(color: DriveColors.border),
      borderRadius: BorderRadius.circular(12),
    ),
    child: Row(
      children: [
        Icon(icon, color: color, size: 22),
        const SizedBox(width: 8),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                label,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontSize: 11),
              ),
              FittedBox(
                fit: BoxFit.scaleDown,
                child: Text(
                  value,
                  style: TextStyle(
                    color: color,
                    fontSize: 22,
                    height: 1.1,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ],
          ),
        ),
      ],
    ),
  );
}
