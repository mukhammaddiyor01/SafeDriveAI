package uz.safedrive.safedrive_ai.controller;

import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class ReleaseEyeProbe extends Instrumentation {
    private Bundle arguments;

    @Override public void onCreate(Bundle input) {
        super.onCreate(input);
        arguments = input;
        start();
    }

    @Override public void onStart() {
        Bundle report = new Bundle();
        try {
            ClassLoader loader = getTargetContext().getClassLoader();
            Class<?> serviceClass = loader.loadClass("uz.safedrive.safedrive_ai.controller.MonitorService");
            Object[] service = new Object[1];
            runOnMainSync(() -> {
                try {
                    service[0] = serviceClass.getConstructor().newInstance();
                    Method attach = ContextWrapper.class.getDeclaredMethod("attachBaseContext", Context.class);
                    attach.setAccessible(true);
                    attach.invoke(service[0], getTargetContext());
                } catch (Exception error) {
                    throw new RuntimeException(error);
                }
            });
            Class<?> type = loader.loadClass(arguments.getString("modelClass"));
            Constructor<?> constructor = type.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            Object model = constructor.newInstance(service[0]);
            Method observe = type.getDeclaredMethod(arguments.getString("observeMethod"), Bitmap.class, Rect.class, float.class);
            observe.setAccessible(true);
            Bitmap image;
            try (java.io.InputStream input = getContext().getAssets().open("side.jpg")) {
                image = BitmapFactory.decodeStream(input);
            }
            try {
                Rect bounds = new Rect(0, 0, image.getWidth(), image.getHeight());
                float yaw = Float.parseFloat(arguments.getString("yaw", "-43"));
                boolean expected = Boolean.parseBoolean(arguments.getString("expectedOpen", "true"));
                Object eye = observe.invoke(model, image, bounds, yaw);
                report.putString("eye", String.valueOf(eye));
                if (!Boolean.valueOf(expected).equals(eye)) throw new IllegalStateException("Eye state does not match fixture");
            } finally {
                image.recycle();
                type.getMethod("close").invoke(model);
            }
            finish(-1, report);
        } catch (Throwable error) {
            android.util.Log.e("SafeDriveAIProbe", "Release model failure", error);
            Throwable root = error;
            for (int index = 0; index < 10 && root.getCause() != null; index++) root = root.getCause();
            report.putString("root", root.toString());
            report.putString("error", error.getClass().getSimpleName());
            report.putString("cause", String.valueOf(error.getCause() == null ? error : error.getCause()));
            finish(1, report);
        }
    }
}
