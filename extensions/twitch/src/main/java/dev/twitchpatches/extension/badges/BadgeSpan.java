package dev.twitchpatches.extension.badges;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.ReplacementSpan;
import java.lang.ref.WeakReference;

final class BadgeSpan extends ReplacementSpan {
    private static final float SIZE_TO_TEXT = 1.3f;
    private static final float CORNER_TO_SIZE = 0.12f;
    private final long user;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private WeakReference<CharSequence> layoutText = new WeakReference<>(null);
    private Drawable image;
    private String imageUrl;

    BadgeSpan(long user) { this.user = user; }

    long user() { return user; }

    boolean shows(String url) {
        Badge badge = BadgeRuntime.badgeFor(user);
        return badge != null && badge.url.equals(url);
    }

    void relayout() {
        CharSequence text = layoutText.get();
        if (!(text instanceof Spannable)) return;
        Spannable spannable = (Spannable) text;
        int start = spannable.getSpanStart(this);
        if (start >= 0) spannable.setSpan(this, start, spannable.getSpanEnd(this), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    @Override public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt metrics) {
        layoutText = new WeakReference<>(text);
        Paint.FontMetricsInt original = paint.getFontMetricsInt();
        Badge badge = BadgeRuntime.badgeFor(user);
        int size = badge == null ? 0 : size(paint);
        if (metrics != null) {
            int center = (original.ascent + original.descent) / 2;
            metrics.ascent = Math.min(original.ascent, center - size / 2);
            metrics.descent = Math.max(original.descent, center + (size + 1) / 2);
            metrics.top = Math.min(original.top, metrics.ascent);
            metrics.bottom = Math.max(original.bottom, metrics.descent);
        }
        return badge == null ? 0 : size + Math.round(paint.measureText(" "));
    }

    @Override public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        Badge badge = BadgeRuntime.badgeFor(user);
        if (badge == null) return;
        Drawable drawable = drawable(badge);
        int size = size(paint);
        Paint.FontMetricsInt metrics = paint.getFontMetricsInt();
        int saved = canvas.save();
        canvas.translate(x, y + (metrics.ascent + metrics.descent - size) / 2f);
        if (badge.background != 0 && drawable != null) {
            fill.setColor(badge.background);
            bounds.set(0, 0, size, size);
            canvas.drawRoundRect(bounds, size * CORNER_TO_SIZE, size * CORNER_TO_SIZE, fill);
        }
        if (drawable != null) {
            drawable.setBounds(0, 0, size, size);
            drawable.draw(canvas);
        }
        canvas.restoreToCount(saved);
    }

    private Drawable drawable(Badge badge) {
        if (badge.url.equals(imageUrl) && image != null) return image;
        image = BadgeRuntime.image(Resources.getSystem(), badge.url);
        imageUrl = image == null ? null : badge.url;
        return image;
    }

    private static int size(Paint paint) {
        return Math.max(1, Math.round(paint.getTextSize() * SIZE_TO_TEXT));
    }
}
