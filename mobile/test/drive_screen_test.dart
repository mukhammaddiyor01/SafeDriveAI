import 'dart:io';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:safedrive_ai/controllers/monitor_controller.dart';
import 'package:safedrive_ai/main.dart';

import 'monitor_controller_test.dart' show FakeMonitor;

void main() {
  testWidgets('dashboard fits narrow screens and settings remain reachable', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(360, 800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    if (const bool.fromEnvironment('SAVE_SCREENSHOT')) {
      final font = FontLoader('sans-serif');
      font.addFont(
        Future.value(
          ByteData.sublistView(
            File('/System/Library/Fonts/Supplemental/Arial.ttf')
                .readAsBytesSync(),
          ),
        ),
      );
      await tester.runAsync(font.load);
      final icons = FontLoader('MaterialIcons');
      icons.addFont(rootBundle.load('fonts/MaterialIcons-Regular.otf'));
      await tester.runAsync(icons.load);
    }
    final native = FakeMonitor();
    final boundary = GlobalKey();
    await tester.pumpWidget(
      RepaintBoundary(
        key: boundary,
        child: SafeDriveApp(controller: MonitorController(native)),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('SafeDriveAI'), findsOneWidget);
    expect(find.text('Start monitoring'), findsOneWidget);
    expect(tester.takeException(), isNull);
    if (const bool.fromEnvironment('SAVE_SCREENSHOT')) {
      final render =
          boundary.currentContext!.findRenderObject()! as RenderRepaintBoundary;
      await tester.runAsync(() async {
        final image = await render.toImage(pixelRatio: 2);
        final bytes = (await image.toByteData(format: ui.ImageByteFormat.png))!;
        final file = File('build/review/dashboard.png');
        await file.parent.create(recursive: true);
        await file.writeAsBytes(bytes.buffer.asUint8List());
      });
    }
    await tester.tap(find.byTooltip('Settings'));
    await tester.pumpAndSettle();
    expect(find.text('Voice and monitoring'), findsOneWidget);
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox());
    await native.events.close();
  });
}
