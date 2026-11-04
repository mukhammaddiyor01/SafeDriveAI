import 'package:flutter/material.dart';

import 'controllers/monitor_controller.dart';
import 'services/native_monitor.dart';
import 'views/drive_screen.dart';
import 'views/theme.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(SafeDriveApp(controller: MonitorController(MethodChannelMonitor())));
}

class SafeDriveApp extends StatelessWidget {
  const SafeDriveApp({super.key, required this.controller});
  final MonitorController controller;
  @override
  Widget build(BuildContext context) => MaterialApp(
    title: 'SafeDriveAI',
    debugShowCheckedModeBanner: false,
    theme: buildTheme(),
    home: DriveScreen(controller: controller),
  );
}
