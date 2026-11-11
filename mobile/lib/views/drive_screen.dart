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
      final limited = {'limited', 'warning'}.contains(state.status);
      return Scaffold(
        bottomNavigationBar: SafeArea(
          top: false,
          child: Center(
            heightFactor: 1,
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 560),
              child: Padding(
                padding: const EdgeInsets.fromLTRB(22, 12, 22, 14),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    FilledButton.icon(
                      onPressed: controller.busy
                          ? null
                          : controller.toggleMonitoring,
                      icon: Icon(
                        state.active
                            ? Icons.stop_rounded
                            : Icons.play_arrow_rounded,
                      ),
                      label: Text(
                        controller.busy
                            ? 'Please wait…'
                            : state.active
                            ? 'Stop monitoring'
                            : 'Start monitoring',
                      ),
                      style: FilledButton.styleFrom(
                        backgroundColor: DriveColors.ink,
                      ),
                    ),
                    if (!state.active)
                      TextButton.icon(
                        onPressed: controller.busy
                            ? null
                            : controller.testAlarm,
                        icon: const Icon(Icons.volume_up_outlined, size: 20),
                        label: const Text('Test alarm'),
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
              constraints: const BoxConstraints(maxWidth: 560),
              child: ListView(
                padding: const EdgeInsets.fromLTRB(22, 12, 22, 14),
                children: [
                  Row(
                    children: [
                      const Icon(
                        Icons.shield_outlined,
                        color: DriveColors.blue,
                        size: 29,
                      ),
                      const SizedBox(width: 9),
                      Expanded(
                        child: Text(
                          'SafeDriveAI',
                          style: Theme.of(context).textTheme.titleLarge,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
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
                  ),
                  const SizedBox(height: 22),
                  Semantics(
                    liveRegion: true,
                    child: Text(
                      state.title,
                      style: Theme.of(context).textTheme.headlineLarge,
                    ),
                  ),
                  const SizedBox(height: 10),
                  _StatusMessage(state: state),
                  if (controller.error != null) ...[
                    const SizedBox(height: 12),
                    Text(
                      controller.error!,
                      style: const TextStyle(color: DriveColors.red),
                    ),
                  ],
                  const SizedBox(height: 22),
                  CameraPanel(state: state),
                  if (state.active) ...[
                    const SizedBox(height: 20),
                    HeadAngleIndicator(
                      yaw: state.yaw,
                      limited: limited || state.eyesOpen == null,
                    ),
                    const SizedBox(height: 22),
                    _ReadingRow(
                      icon: Icons.visibility_outlined,
                      label: state.sideEye ? 'Visible eye' : 'Eye openness',
                      value: state.eyeReading,
                      color: state.eyesOpen == null
                          ? DriveColors.muted
                          : DriveColors.blue,
                    ),
                    const SizedBox(height: 10),
                    _ReadingRow(
                      icon: Icons.notifications_none_rounded,
                      label: 'Alerts',
                      value: '${state.alerts}',
                      color: state.alerts > 0
                          ? DriveColors.amber
                          : DriveColors.blue,
                    ),
                  ],
                  const SizedBox(height: 16),
                  const _AudioSummary(),
                  if (state.assistant == 'Gemini Live connecting' ||
                      state.assistant == 'Gemini Live active') ...[
                    const SizedBox(height: 16),
                    Center(
                      child: CircleAvatar(
                        radius: 34,
                        backgroundColor: DriveColors.blue,
                        child: state.assistant == 'Gemini Live connecting'
                            ? const CircularProgressIndicator(
                                color: Colors.white,
                              )
                            : const Icon(
                                Icons.graphic_eq,
                                color: Colors.white,
                                size: 32,
                              ),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Center(child: Text(state.assistant)),
                  ],
                  if (state.assistant == 'Gemini Live unavailable' ||
                      state.assistant ==
                          'Gemini Live needs microphone permission') ...[
                    const SizedBox(height: 12),
                    Text(
                      state.assistant,
                      style: const TextStyle(color: DriveColors.red),
                    ),
                  ],
                  const SizedBox(height: 20),
                  const Text(
                    'If you feel sleepy, pull over safely and rest.',
                    textAlign: TextAlign.center,
                  ),
                ],
              ),
            ),
          ),
        ),
      );
    },
  );
}

class _StatusMessage extends StatelessWidget {
  const _StatusMessage({required this.state});
  final MonitorState state;

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
    return Container(
      padding: warning || danger ? const EdgeInsets.all(14) : EdgeInsets.zero,
      decoration: BoxDecoration(
        color: warning || danger
            ? color.withValues(alpha: 0.08)
            : Colors.transparent,
        borderRadius: BorderRadius.circular(14),
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
            size: 23,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              message,
              style: TextStyle(
                color: warning || danger ? color : DriveColors.muted,
                fontSize: 15,
                height: 1.4,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _ReadingRow extends StatelessWidget {
  const _ReadingRow({
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
    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 15),
    decoration: BoxDecoration(
      border: Border.all(color: DriveColors.border),
      borderRadius: BorderRadius.circular(12),
    ),
    child: Row(
      children: [
        Icon(icon, color: color, size: 25),
        const SizedBox(width: 12),
        Expanded(
          child: Text(
            label,
            style: const TextStyle(
              color: DriveColors.ink,
              fontSize: 17,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
        const SizedBox(width: 8),
        Flexible(
          child: Text(
            value,
            textAlign: TextAlign.end,
            style: TextStyle(
              color: color,
              fontSize: 23,
              fontWeight: FontWeight.w700,
            ),
          ),
        ),
      ],
    ),
  );
}

class _AudioSummary extends StatelessWidget {
  const _AudioSummary();

  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
    decoration: BoxDecoration(
      border: Border.all(color: DriveColors.border),
      borderRadius: BorderRadius.circular(12),
    ),
    child: Row(
      children: [
        const Icon(Icons.volume_up_outlined, color: DriveColors.blue, size: 25),
        const SizedBox(width: 9),
        const Expanded(child: Text('Loud alarm')),
        const SizedBox(width: 12),
        const Icon(Icons.bluetooth, color: DriveColors.blue, size: 23),
        const SizedBox(width: 6),
        const Expanded(
          child: Text(
            'Car audio\nwhen connected',
            style: TextStyle(fontSize: 12),
          ),
        ),
      ],
    ),
  );
}
