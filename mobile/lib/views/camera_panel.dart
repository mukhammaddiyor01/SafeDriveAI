import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

import '../models/monitor_state.dart';
import 'theme.dart';

class CameraPanel extends StatelessWidget {
  const CameraPanel({super.key, required this.state});
  final MonitorState state;
  @override
  Widget build(BuildContext context) => ClipRRect(
    borderRadius: BorderRadius.circular(22),
    child: AspectRatio(
      aspectRatio: 4 / 3,
      child: ColoredBox(
        color: DriveColors.ink,
        child: Stack(
          fit: StackFit.expand,
          children: [
            if (state.active && defaultTargetPlatform == TargetPlatform.android)
              const AndroidView(viewType: 'uz.safedrive/camera')
            else
              const _CameraGuide(),
            Positioned(
              top: 14,
              right: 14,
              child: DecoratedBox(
                decoration: BoxDecoration(
                  color: DriveColors.blue,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 12,
                    vertical: 8,
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(
                        state.active
                            ? Icons.videocam_outlined
                            : Icons.videocam_off_outlined,
                        size: 17,
                        color: Colors.white,
                      ),
                      const SizedBox(width: 7),
                      Text(
                        state.active ? 'Front camera' : 'Camera off',
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            if (state.active && state.status == 'starting')
              const Center(
                child: CircularProgressIndicator(color: Colors.white),
              ),
          ],
        ),
      ),
    ),
  );
}

class _CameraGuide extends StatelessWidget {
  const _CameraGuide();
  @override
  Widget build(BuildContext context) => const Padding(
    padding: EdgeInsets.fromLTRB(14, 48, 14, 14),
    child: Center(
      child: FittedBox(
        fit: BoxFit.scaleDown,
        child: SizedBox(
          width: 240,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                Icons.center_focus_strong_outlined,
                color: Color(0xFF9DB4D6),
                size: 64,
              ),
              SizedBox(height: 14),
              Text(
                'Keep your face and shoulders in frame',
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.white, fontSize: 14),
              ),
              SizedBox(height: 4),
              Text(
                'Use a secure phone mount',
                textAlign: TextAlign.center,
                style: TextStyle(color: Color(0xFFB6C7E0), fontSize: 12),
              ),
            ],
          ),
        ),
      ),
    ),
  );
}
