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
            const Divider(),
            Text(
              'ChatGPT voice coach',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const Text(
              'Connect to the assistant on your Mac before starting monitoring. '
              'After an alert clears, the coach asks a question and sends your '
              'transcribed reply to the saved OpenAI conversation.',
            ),
            const SizedBox(height: 8),
            OutlinedButton(
              onPressed:
                  controller.busy ||
                      !controller.voice ||
                      controller.state.active
                  ? null
                  : controller.connectChatGpt,
              child: Text(
                controller.llm ? 'Check ChatGPT connection' : 'Connect ChatGPT',
              ),
            ),
            if (controller.chatGptVerified)
              const Text('ChatGPT conversation connected'),
            if (controller.llm)
              TextButton(
                onPressed: controller.busy || controller.state.active
                    ? null
                    : () => controller.updatePreferences(llmEnabled: false),
                child: const Text('Disconnect ChatGPT'),
              ),
            if (controller.error != null) Text(controller.error!),
            const SizedBox(height: 12),
            Text('Voice source: ${controller.state.assistant}'),
            const SizedBox(height: 20),
            const Text(
              'Camera frames stay on your phone. Adjust the camera if your eyes are hard to see.',
            ),
            const SizedBox(height: 14),
            const Text(
              'The phone’s speech service listens briefly after the question. Allow microphone access when starting monitoring.',
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
