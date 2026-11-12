import 'dart:io';
import 'dart:ui' as ui;

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:safedrive_ai/controllers/monitor_controller.dart';
import 'package:safedrive_ai/main.dart';
import 'package:safedrive_ai/models/monitor_state.dart';
import 'package:safedrive_ai/views/camera_panel.dart';

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
  testWidgets('active dashboard keeps stop reachable with large text', (
    tester,
  ) async {
    debugDefaultTargetPlatformOverride = TargetPlatform.linux;
    addTearDown(() => debugDefaultTargetPlatformOverride = null);
    tester.platformDispatcher.textScaleFactorTestValue = 1.3;
    addTearDown(tester.platformDispatcher.clearTextScaleFactorTestValue);
    tester.view.physicalSize = const Size(320, 640);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final native = FakeMonitor();
    final boundary = GlobalKey();
    await tester.pumpWidget(
      RepaintBoundary(
        key: boundary,
        child: SafeDriveApp(controller: MonitorController(native)),
      ),
    );
    await tester.pumpAndSettle();
    native.events.add(
      const MonitorState(
        status: 'monitoring',
        message: 'Monitoring active',
        active: true,
        eyesOpen: 0.82,
        sideEye: true,
        yaw: -44,
        assistant: 'Gemini Live active',
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Visible eye detected'), findsOneWidget);
    expect(find.text('Stop monitoring').hitTestable(), findsOneWidget);
    expect(tester.takeException(), isNull);

    expect(find.byType(Scrollable), findsNothing);
    expect(find.text('82%').hitTestable(), findsOneWidget);
    expect(find.text('Eye score').hitTestable(), findsOneWidget);
    expect(
      tester.getTopLeft(find.byKey(const ValueKey('gemini-panel'))).dy,
      lessThan(tester.getTopLeft(find.byType(CameraPanel)).dy),
    );
    if (const bool.fromEnvironment('SAVE_SCREENSHOT')) {
      final render =
          boundary.currentContext!.findRenderObject()! as RenderRepaintBoundary;
      await tester.runAsync(() async {
        final image = await render.toImage(pixelRatio: 2);
        final bytes = (await image.toByteData(format: ui.ImageByteFormat.png))!;
        await File('build/review/active-dashboard.png')
            .writeAsBytes(bytes.buffer.asUint8List());
      });
    }
    expect(find.text('-44°'), findsOneWidget);
    expect(find.text('82%'), findsOneWidget);
    expect(find.text('Stop monitoring').hitTestable(), findsOneWidget);
    expect(tester.takeException(), isNull);

    native.events.add(
      const MonitorState(
        status: 'monitoring',
        active: true,
        sideEye: true,
        eyesOpen: 0.82,
        yaw: -44,
        lighting: 'low',
        message: 'Low-light assistance active. Keep your face visible.',
      ),
    );
    await tester.pumpAndSettle();
    expect(
      find.text('Low-light assistance active. Keep your face visible.'),
      findsOneWidget,
    );
    expect(find.byType(Scrollable), findsNothing);
    expect(tester.takeException(), isNull);

    native.events.add(
      const MonitorState(
        status: 'limited',
        message: 'Eyes not clearly visible.',
        active: true,
        yaw: 68,
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('68°'), findsOneWidget);
    expect(find.text('—').hitTestable(), findsOneWidget);
    expect(find.byType(Scrollable), findsNothing);
    expect(tester.takeException(), isNull);

    tester.view.physicalSize = const Size(640, 360);
    native.events.add(
      const MonitorState(
        status: 'limited',
        message: 'Side angle limits eye tracking. Adjust the phone mount while parked.',
        active: true,
        yaw: 43,
        assistant: 'Gemini Live needs microphone permission',
      ),
    );
    await tester.pumpAndSettle();
    expect(find.byType(Scrollable), findsNothing);
    expect(find.text('Stop monitoring').hitTestable(), findsOneWidget);
    expect(find.text('—').hitTestable(), findsOneWidget);
    expect(tester.takeException(), isNull);

    await tester.tap(find.text('Stop monitoring'));
    await tester.pumpAndSettle();
    expect(find.text('Start monitoring').hitTestable(), findsOneWidget);
    await tester.pumpWidget(const SizedBox());
    await native.events.close();
    debugDefaultTargetPlatformOverride = null;
  });
}
