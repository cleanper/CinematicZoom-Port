package mix.cinematiczoom;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;

public class ZoomManager {
    private static boolean zoomHeld = false;

    private static float currentZoomMul = 1.0f;
    private static float targetZoomMul = 1.0f;
    private static float holdZoomMul = ZoomConfig.INSTANCE.baseZoomMultiplier;

    private static float currentBarsPct = 0f;
    private static float targetBarsPct = 0f;

    private static long lastNs = 0L;

    private static Boolean prevHudHidden = null;
    private static Boolean prevSmoothCamera = null;

    private static final double NANOS_TO_MILLIS = 1_000_000.0;
    private static final double MAX_DELTA_MS = 50.0;
    private static final float ZOOM_EPSILON = 1e-4f;
    private static final float BARS_EPSILON = 1e-3f;

    public static void tick(MinecraftClient client, KeyBinding key) {
        boolean inScreen = client.currentScreen != null;
        boolean wantZoom = key.isPressed() && !inScreen;

        boolean starting = wantZoom && !zoomHeld;
        boolean ending = !wantZoom && zoomHeld;

        if (starting) {
            holdZoomMul = clamp(ZoomConfig.INSTANCE.baseZoomMultiplier,
                    ZoomConfig.INSTANCE.minZoomMultiplier,
                    ZoomConfig.INSTANCE.maxZoomMultiplier);

            // Прячем HUD/прицел
            if (ZoomConfig.INSTANCE.hideHudDuringZoom) {
                prevHudHidden = client.options.hudHidden;
                client.options.hudHidden = true;
            }
            // Включаем кинематографичную камеру
            if (ZoomConfig.INSTANCE.enableCinematicCamera) {
                prevSmoothCamera = client.options.smoothCameraEnabled;
                client.options.smoothCameraEnabled = true;
            }
        }

        if (ending) {
            // Вернуть HUD
            if (prevHudHidden != null) {
                client.options.hudHidden = prevHudHidden;
                prevHudHidden = null;
            }
            // Вернуть камеру
            if (prevSmoothCamera != null) {
                client.options.smoothCameraEnabled = prevSmoothCamera;
                prevSmoothCamera = null;
            }
        }

        zoomHeld = wantZoom;
        targetZoomMul = zoomHeld ? holdZoomMul : 1.0f;
        targetBarsPct = zoomHeld ? ZoomConfig.INSTANCE.barsPercent : 0f;
    }

    public static void frameUpdate() {
        final int smooth = ZoomConfig.INSTANCE.smoothMs;
        long now = System.nanoTime();

        if (lastNs == 0L) {
            lastNs = now;
            return;
        }

        double dtMs = Math.min((now - lastNs) / NANOS_TO_MILLIS, MAX_DELTA_MS);
        lastNs = now;

        if (smooth <= 0) {
            currentZoomMul = targetZoomMul;
            currentBarsPct = targetBarsPct;
            return;
        }

        final double alpha = 1.0 - Math.exp(-dtMs / (smooth / 2.302585092994046));

        currentZoomMul = lerp(currentZoomMul, targetZoomMul, alpha);
        currentBarsPct = lerp(currentBarsPct, targetBarsPct, alpha);

        if (Math.abs(currentZoomMul - targetZoomMul) < ZOOM_EPSILON) {
            currentZoomMul = targetZoomMul;
        }
        if (Math.abs(currentBarsPct - targetBarsPct) < BARS_EPSILON) {
            currentBarsPct = targetBarsPct;
        }
    }

    public static boolean isZoomHeld() { return zoomHeld; }
    public static double getCurrentFovMul() { return currentZoomMul; }

    public static boolean onWheel(double vertical) {
        if (!zoomHeld || !ZoomConfig.INSTANCE.mouseWheelEnabled || vertical == 0) {
            return false;
        }

        float step = ZoomConfig.INSTANCE.wheelStep;
        holdZoomMul = clamp(holdZoomMul + (vertical > 0 ? -step : step),
                ZoomConfig.INSTANCE.minZoomMultiplier,
                ZoomConfig.INSTANCE.maxZoomMultiplier);
        return true;
    }

    public static void renderBars(DrawContext ctx) {
        if (currentBarsPct <= BARS_EPSILON) return;

        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        int h = Math.round(sh * (currentBarsPct * 0.01f));

        if (h <= 0) return;

        int color = 0xFF000000;
        ctx.fill(0, 0, sw, h, color);
        ctx.fill(0, sh - h, sw, sh, color);
    }

    private static float lerp(float a, float b, double t) {
        if (t <= 0) return a;
        if (t >= 1) return b;
        return a + (float)((b - a) * t);
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : (Math.min(v, max));
    }
}
