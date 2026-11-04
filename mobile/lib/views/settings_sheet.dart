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
              'Ovoz va kuzatuv',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 12),
            SwitchListTile.adaptive(
              contentPadding: EdgeInsets.zero,
              title: const Text('Ovozli eslatmalar'),
              subtitle: const Text(
                'Dam olish haqida qisqa savol va eslatmalar. Signal doim yoqilgan.',
              ),
              value: controller.voice,
              onChanged: controller.busy
                  ? null
                  : (v) => controller.updatePreferences(voiceEnabled: v),
            ),
            SwitchListTile.adaptive(
              contentPadding: EdgeInsets.zero,
              title: const Text('Kompyuterdagi AI yordamchi'),
              subtitle: const Text(
                'USB orqali mahalliy LLM’ga ulanish. Ulanmasa, oddiy ovozli eslatma ishlaydi.',
              ),
              value: controller.llm,
              onChanged: controller.busy
                  ? null
                  : (v) => controller.updatePreferences(llmEnabled: v),
            ),
            const SizedBox(height: 12),
            Text('Yordamchi: ${controller.state.assistant}'),
            const SizedBox(height: 20),
            const Text(
              'Kamera tasvirlari saqlanmaydi va serverga yuborilmaydi. Ko‘zlar yonboshda, qorong‘ida yoki ko‘zoynak ortida ko‘rinmasa, ilova ko‘rinish cheklanganini bildiradi.',
            ),
            const SizedBox(height: 14),
            const Text(
              'Ovoz tili telefoningizdagi Text-to-Speech xizmatiga bog‘liq. O‘zbekcha ovoz mavjud bo‘lmasa, inglizcha eslatma ishlatiladi. Bu versiya ovozli javobni tinglamaydi.',
            ),
            const SizedBox(height: 20),
            FilledButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Tayyor'),
            ),
          ],
        ),
      ),
    ),
  );
}
