import 'package:flutter/material.dart';

import 'theme.dart';

class HeadAngleIndicator extends StatelessWidget {
  const HeadAngleIndicator({
    super.key,
    required this.yaw,
    required this.limited,
  });

  final double? yaw;
  final bool limited;

  @override
  Widget build(BuildContext context) {
    final angle = yaw;
    final color = limited ? DriveColors.amber : DriveColors.blue;
    return Semantics(
      label: 'Head angle relative to camera',
      value: angle == null ? 'Unavailable' : '${angle.round()} degrees',
      child: ExcludeSemantics(
        child: Row(
          children: [
            SizedBox(
              width: 84,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Head angle', style: TextStyle(fontSize: 11)),
                  Text(
                    angle == null ? '—' : '${angle.round()}°',
                    style: TextStyle(
                      fontSize: 23,
                      fontWeight: FontWeight.w700,
                      color: angle == null ? DriveColors.muted : color,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  SizedBox(
                    height: 20,
                    width: double.infinity,
                    child: CustomPaint(
                      painter: _AngleScale(
                        position: angle == null
                            ? null
                            : ((angle.clamp(-60, 60) + 60) / 120),
                        color: color,
                      ),
                    ),
                  ),
                  const Row(
                    children: [
                      Expanded(
                        child: FittedBox(
                          fit: BoxFit.scaleDown,
                          alignment: Alignment.centerLeft,
                          child: Text('Left', style: TextStyle(fontSize: 11)),
                        ),
                      ),
                      Expanded(
                        child: FittedBox(
                          fit: BoxFit.scaleDown,
                          child: Text(
                            'Forward',
                            style: TextStyle(fontSize: 11),
                          ),
                        ),
                      ),
                      Expanded(
                        child: FittedBox(
                          fit: BoxFit.scaleDown,
                          alignment: Alignment.centerRight,
                          child: Text('Right', style: TextStyle(fontSize: 11)),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _AngleScale extends CustomPainter {
  const _AngleScale({required this.position, required this.color});

  final double? position;
  final Color color;

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = DriveColors.border
      ..strokeWidth = 3
      ..strokeCap = StrokeCap.round;
    final start = 7.0;
    final end = size.width - 7;
    final middle = size.height / 2;
    canvas.drawLine(Offset(start, middle), Offset(end, middle), paint);
    for (final fraction in [0.0, 0.5, 1.0]) {
      final x = start + (end - start) * fraction;
      canvas.drawLine(Offset(x, middle - 5), Offset(x, middle + 5), paint);
    }
    final marker = position;
    if (marker != null) {
      final center = Offset(start + (end - start) * marker, middle);
      canvas.drawCircle(center, 8, Paint()..color = DriveColors.background);
      canvas.drawCircle(center, 6, Paint()..color = color);
    }
  }

  @override
  bool shouldRepaint(_AngleScale oldDelegate) =>
      position != oldDelegate.position || color != oldDelegate.color;
}
