package pt.molduratv;

import android.graphics.*;
import android.os.Handler;
import android.view.*;
import android.widget.*;
import androidx.media3.ui.PlayerView;

/** Temporary SDR frame, capped at 960x540 per pane; never allocates another decoder. */
@androidx.media3.common.util.UnstableApi
final class PresentationCover {
    final FrameLayout layer;
    private final Handler handler;
    private int generation, pending, expected;
    private boolean capturing;
    private Runnable captureDeadline;
    PresentationCover(android.content.Context context, Handler handler) {
        this.handler = handler; layer = new FrameLayout(context);
        layer.setBackgroundColor(Color.BLACK); layer.setVisibility(View.GONE);
        layer.setClickable(false); layer.setFocusable(false);
    }
    void prepare(PlayerView left, boolean leftPhoto, PlayerView right, boolean rightPhoto, Runnable change) {
        if (visible()) { change.run(); return; }
        clear(); int token = generation; capturing = true; pending = right == null ? 1 : 2; expected = pending;
        captureDeadline = () -> finish(token, change);
        handler.postDelayed(captureDeadline, 250);
        capture(left, leftPhoto, token, change);
        if (right != null) capture(right, rightPhoto, token, change);
    }
    private void capture(PlayerView view, boolean photo, int token, Runnable change) {
        if (photo) {
            View image = view.findViewById(androidx.media3.ui.R.id.exo_image);
            if (image instanceof ImageView && ((ImageView)image).getDrawable() instanceof android.graphics.drawable.BitmapDrawable) {
                Bitmap original = ((android.graphics.drawable.BitmapDrawable)((ImageView)image).getDrawable()).getBitmap();
                if (original.getColorSpace() != null && original.getColorSpace().isWideGamut()
                        || android.os.Build.VERSION.SDK_INT >= 34 && original.hasGainmap()) {
                    complete(view, null, token, change); return;
                }
            }
        }
        View surface = view.getVideoSurfaceView();
        View source = !photo && surface instanceof SurfaceView ? surface : view;
        int width = source.getWidth(), height = source.getHeight();
        if (width <= 0 || height <= 0) { complete(view, null, token, change); return; }
        double scale = Math.min(1, Math.min(960d / width, 540d / height));
        final Bitmap bitmap;
        try { bitmap = Bitmap.createBitmap(Math.max(1, (int)(width * scale)), Math.max(1, (int)(height * scale)), Bitmap.Config.ARGB_8888); }
        catch (OutOfMemoryError error) { complete(view, null, token, change); return; }
        if (!photo && source instanceof SurfaceView) {
            try {
                PixelCopy.request((SurfaceView)source, bitmap, result -> {
                    if (result == PixelCopy.SUCCESS) complete(view, bitmap, token, change);
                    else { bitmap.recycle(); complete(view, null, token, change); }
                }, handler);
            } catch (IllegalArgumentException error) { bitmap.recycle(); complete(view, null, token, change); }
        } else {
            try {
                Canvas canvas = new Canvas(bitmap); canvas.scale((float)scale, (float)scale); view.draw(canvas);
                complete(view, bitmap, token, change);
            } catch (RuntimeException error) { bitmap.recycle(); complete(view, null, token, change); }
        }
    }
    private void complete(View view, Bitmap bitmap, int token, Runnable change) {
        if (token != generation || !capturing) { if (bitmap != null) bitmap.recycle(); return; }
        if (bitmap != null) {
            ImageView image = new ImageView(layer.getContext()); image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setImageBitmap(bitmap);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(view.getWidth(), view.getHeight(), Gravity.TOP | Gravity.START);
            int start = layer.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL
                ? ((View)view.getParent()).getWidth() - view.getRight() : view.getLeft();
            params.setMarginStart(start); params.topMargin = view.getTop(); layer.addView(image, params);
        }
        if (--pending == 0) finish(token, change);
    }
    private void finish(int token, Runnable change) {
        if (token != generation || !capturing) return;
        capturing = false; handler.removeCallbacks(captureDeadline);
        boolean complete = layer.getChildCount() == expected;
        layer.setVisibility(complete ? View.VISIBLE : View.GONE);
        if (!complete) layer.removeAllViews();
        change.run();
    }
    boolean visible() { return layer.getVisibility() == View.VISIBLE; }
    void clear() {
        generation++; capturing = false;
        if (captureDeadline != null) handler.removeCallbacks(captureDeadline);
        layer.setVisibility(View.GONE); layer.removeAllViews();
    }
}
