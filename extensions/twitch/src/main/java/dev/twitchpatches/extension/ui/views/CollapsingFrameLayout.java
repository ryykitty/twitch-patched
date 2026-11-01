package dev.twitchpatches.extension.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public final class CollapsingFrameLayout extends android.widget.FrameLayout {
    public CollapsingFrameLayout(Context context, AttributeSet attributes) {
        super(context, attributes);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        if (hasNoContent(this)) setMeasuredDimension(getMeasuredWidth(), 0);
    }

    private static boolean hasNoContent(View view) {
        if (view.getMeasuredHeight() == 0) return true;
        if (!(view instanceof ViewGroup)) return false;
        ViewGroup group = (ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); index++) {
            if (!hasNoContent(group.getChildAt(index))) return false;
        }
        return true;
    }
}
