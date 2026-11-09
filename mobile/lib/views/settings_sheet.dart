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
              title: const Text('Gemini Live'),
              subtitle: const Text(
                'Start a live conversation after an alert clears. '
                'Sends microphone audio to Gemini.',
              ),
              value: controller.geminiLive,
              onChanged: controller.busy || controller.state.active
                  ? null
                  : controller.setGeminiLive,
            ),
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('Gemini Live connection'),
              subtitle: Text(
                controller.geminiLiveWireless
                    ? 'HTTPS server configured'
                    : 'USB to Mac assistant',
              ),
              onTap: controller.busy || controller.state.active
                  ? null
                  : () async {
                      var address = '';
                      var secret = '';
                      final values = await showDialog<(String, String)>(
                        context: context,
                        builder: (dialogContext) => AlertDialog(
                          title: const Text('Connect without USB'),
                          content: SingleChildScrollView(
                            child: Column(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                TextField(
                                  keyboardType: TextInputType.url,
                                  decoration: const InputDecoration(
                                    labelText: 'HTTPS server URL',
                                  ),
                                  onChanged: (value) => address = value.trim(),
                                ),
                                TextField(
                                  obscureText: true,
                                  enableSuggestions: false,
                                  autocorrect: false,
                                  decoration: const InputDecoration(
                                    labelText: 'Server access token',
                                  ),
                                  onChanged: (value) => secret = value.trim(),
                                ),
                              ],
                            ),
                          ),
                          actions: [
                            TextButton(
                              onPressed: () => Navigator.pop(dialogContext),
                              child: const Text('Cancel'),
                            ),
                            if (controller.geminiLiveWireless)
                              TextButton(
                                onPressed: () =>
                                    Navigator.pop(dialogContext, ('', '')),
                                child: const Text('Use USB'),
                              ),
                            FilledButton(
                              onPressed: () {
                                if (address.isNotEmpty && secret.isNotEmpty) {
                                  Navigator.pop(dialogContext, (
                                    address,
                                    secret,
                                  ));
                                }
                              },
                              child: const Text('Save server'),
                            ),
                          ],
                        ),
                      );
                      if (values != null) {
                        await controller.setGeminiLiveBroker(
                          values.$1,
                          values.$2,
                        );
                      }
                    },
            ),
            const Divider(),
            Text(
              'ChatGPT voice coach',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const Text(
              'Connect your ChatGPT account to use the coach directly on this phone. '
              'Recent voice turns are stored on this device and sent as context.',
            ),
            const SizedBox(height: 8),
            if (controller.waitingForChatGpt) ...[
              const Text('Waiting for approval in your browser.'),
              TextButton(
                onPressed: () async {
                  var address = '';
                  final value = await showDialog<String>(
                    context: context,
                    builder: (dialogContext) => AlertDialog(
                      title: const Text('Finish ChatGPT sign-in'),
                      content: TextField(
                        autocorrect: false,
                        enableSuggestions: false,
                        keyboardType: TextInputType.url,
                        decoration: const InputDecoration(
                          hintText: 'Paste the 127.0.0.1 address from Chrome',
                        ),
                        onChanged: (value) => address = value.trim(),
                      ),
                      actions: [
                        TextButton(
                          onPressed: () => Navigator.pop(dialogContext),
                          child: const Text('Cancel'),
                        ),
                        TextButton(
                          onPressed: () =>
                              Navigator.pop(dialogContext, address),
                          child: const Text('Finish sign-in'),
                        ),
                      ],
                    ),
                  );
                  if (value != null && value.isNotEmpty) {
                    await controller.completeChatGptSignIn(value);
                  }
                },
                child: const Text('Paste browser return address'),
              ),
              TextButton(
                onPressed: controller.cancelChatGptSignIn,
                child: const Text('Cancel sign-in'),
              ),
            ] else if (controller.chatGptEmail == null) ...[
              FilledButton(
                onPressed: controller.busy || controller.state.active
                    ? null
                    : controller.signInChatGpt,
                child: const Text('Continue with ChatGPT'),
              ),
              OutlinedButton(
                onPressed:
                    controller.busy ||
                        !controller.voice ||
                        controller.state.active
                    ? null
                    : controller.connectChatGpt,
                child: const Text('Connect Mac assistant'),
              ),
              if (controller.chatGptVerified)
                const Text('Mac assistant connected'),
              if (controller.llm)
                TextButton(
                  onPressed: controller.busy || controller.state.active
                      ? null
                      : () => controller.updatePreferences(llmEnabled: false),
                  child: const Text('Disconnect Mac assistant'),
                ),
            ] else ...[
              Text('Connected as ${controller.chatGptEmail}'),
              TextButton(
                onPressed: controller.busy || controller.state.active
                    ? null
                    : controller.signOutChatGpt,
                child: const Text('Sign out of ChatGPT'),
              ),
            ],
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
