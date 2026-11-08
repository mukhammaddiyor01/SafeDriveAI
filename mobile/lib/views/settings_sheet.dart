import 'package:flutter/material.dart';

import '../controllers/monitor_controller.dart';

class SettingsSheet extends StatelessWidget {
  const SettingsSheet({super.key, required this.controller});
  final MonitorController controller;
  @override
  Widget build(BuildContext context) => SafeArea(
    child: ListenableBuilder(
      listenable: controller,
      builder: (context, _) => SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 30),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              'Voice and monitoring',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 12),
            SwitchListTile.adaptive(
              contentPadding: EdgeInsets.zero,
              title: const Text('Voice reminders'),
              subtitle: const Text(
                'Play a brief spoken reminder during a drowsiness alert. The alarm stays on.',
              ),
              value: controller.voice,
              onChanged: controller.busy
                  ? null
                  : (v) => controller.updatePreferences(voiceEnabled: v),
            ),
            SwitchListTile.adaptive(
              contentPadding: EdgeInsets.zero,
              title: const Text('AI voice coach'),
              subtitle: const Text(
                'Connect to the assistant over USB. Local reminders are available offline.',
              ),
              value: controller.llm,
              onChanged: controller.busy
                  ? null
                  : (v) => controller.updatePreferences(llmEnabled: v),
            ),
            const SizedBox(height: 12),
            Text('Voice source: ${controller.state.assistant}'),
            const SizedBox(height: 20),
            const Text(
              'Camera frames stay on your phone. Adjust the camera if your eyes are hard to see.',
            ),
            const SizedBox(height: 14),
            const Text(
              'Spoken reminders use your phone’s text-to-speech engine.',
            ),
            const SizedBox(height: 20),
            FilledButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Done'),
            ),
          ],
        ),
      ),
    ),
  );
}
