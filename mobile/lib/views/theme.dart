import 'package:flutter/material.dart';

abstract final class DriveColors {
  static const ink = Color(0xFF172B46);
  static const blue = Color(0xFF275FC1);
  static const background = Color(0xFFF2F5F9);
  static const muted = Color(0xFF596C82);
  static const red = Color(0xFFBD372D);
}

ThemeData buildTheme() => ThemeData(
  useMaterial3: true,
  colorScheme: ColorScheme.fromSeed(
    seedColor: DriveColors.blue,
    primary: DriveColors.blue,
    surface: Colors.white,
    error: DriveColors.red,
  ),
  scaffoldBackgroundColor: DriveColors.background,
  fontFamily: 'sans-serif',
  textTheme: const TextTheme(
    headlineLarge: TextStyle(
      fontSize: 30,
      fontWeight: FontWeight.w700,
      color: DriveColors.ink,
      height: 1.15,
    ),
    titleLarge: TextStyle(
      fontSize: 22,
      fontWeight: FontWeight.w700,
      color: DriveColors.ink,
    ),
    bodyLarge: TextStyle(fontSize: 16, color: DriveColors.ink, height: 1.4),
    bodyMedium: TextStyle(fontSize: 14, color: DriveColors.muted, height: 1.4),
  ),
  filledButtonTheme: FilledButtonThemeData(
    style: FilledButton.styleFrom(
      minimumSize: const Size.fromHeight(58),
      textStyle: const TextStyle(
        fontFamily: 'sans-serif',
        fontSize: 17,
        fontWeight: FontWeight.w700,
      ),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
    ),
  ),
);
