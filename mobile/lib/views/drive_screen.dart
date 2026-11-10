import 'package:flutter/material.dart';

import '../controllers/monitor_controller.dart';
import '../models/monitor_state.dart';
import 'camera_panel.dart';
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
        body: SafeArea(
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 560),
              child: ListView(
                padding: const EdgeInsets.fromLTRB(22, 12, 22, 26),
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
                  Text(
                    state.title,
                    style: Theme.of(context).textTheme.headlineLarge,
                    semanticsLabel: 'Status: ${state.title}',
                  ),
                  const SizedBox(height: 9),
                  Text(
                    state.active ? 'Monitoring continues in the background.' : 'Mount your phone, then start monitoring before you drive.',
                  ),
                  const SizedBox(height: 16),
                  _StatusMessage(state: state),
                  if (state.assistant == 'Gemini Live connecting' ||
                      state.assistant == 'Gemini Live active') ...[
                    const SizedBox(height: 16),
                    Center(
                      child: CircleAvatar(
                        radius: 42,
                        backgroundColor: DriveColors.blue,
                        child: state.assistant == 'Gemini Live connecting'
                            ? const CircularProgressIndicator(
                                color: Colors.white,
                              )
                            : const Icon(
                                Icons.graphic_eq,
                                color: Colors.white,
                                size: 36,
                              ),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Center(child: Text(state.assistant)),
                  ],
                  if (state.assistant == 'Gemini Live unavailable' ||
                      state.assistant ==
                          'Gemini Live needs microphone permission')
                    Text(state.assistant),
                  if (controller.error != null) ...[
                    const SizedBox(height: 12),
                    Text(
                      controller.error!,
                      style: const TextStyle(color: DriveColors.red),
                    ),
                  ],
                  const SizedBox(height: 16),
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
                    style: state.active
                        ? FilledButton.styleFrom(
                            backgroundColor: DriveColors.ink,
                          )
                        : null,
                  ),
                  if (!state.active) ...[
                    const SizedBox(height: 8),
                    TextButton.icon(
                      onPressed: controller.busy || state.active
                          ? null
                          : controller.testAlarm,
                      icon: const Icon(Icons.volume_up_outlined, size: 20),
                      label: const Text('Test alarm'),
                    ),
                  ],
                  const SizedBox(height: 22),
                  CameraPanel(state: state),
                  const SizedBox(height: 16),
                  if (state.active) ...[
                    const SizedBox(height: 20),
                    Row(
                      children: [
                        _Metric(
                          label: state.sideEye ? 'Visible eye' : 'Eye openness',
                          value: state.eyeReading,
                        ),
                        const SizedBox(width: 12),
                        _Metric(
                          label: 'Camera-relative yaw',
                          value: state.yaw == null
                              ? '—'
                              : '${state.yaw!.round()}°',
                        ),
                        const SizedBox(width: 12),
                        _Metric(label: 'Alerts', value: '${state.alerts}'),
                      ],
                    ),
                  ],
                  const SizedBox(height: 12),
                  const Divider(),
                  const SizedBox(height: 14),
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Icon(
                        Icons.local_parking_outlined,
                        size: 23,
                        color: DriveColors.muted,
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          'If you feel sleepy, pull over safely and rest.',
                          style: Theme.of(context).textTheme.bodyMedium,
                        ),
                      ),
                    ],
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
        ? const Color(0xFF865600)
        : DriveColors.blue;
    return Semantics(
      liveRegion: true,
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: color.withValues(alpha: 0.08),
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
                  : Icons.info_outline,
              color: color,
              size: 22,
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                state.message,
                style: TextStyle(color: color, fontSize: 14, height: 1.4),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Metric extends StatelessWidget {
  const _Metric({required this.label, required this.value});
  final String label;
  final String value;
  @override
  Widget build(BuildContext context) => Expanded(
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          value,
          style: const TextStyle(
            fontSize: 25,
            fontWeight: FontWeight.w700,
            color: DriveColors.ink,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          label,
          style: const TextStyle(fontSize: 11, color: DriveColors.muted),
        ),
      ],
    ),
  );
}
