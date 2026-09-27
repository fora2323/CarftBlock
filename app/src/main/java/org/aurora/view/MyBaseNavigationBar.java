package com.aurora.manager.view;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.PathInterpolator;

import androidx.annotation.IdRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.aurora.R;

import java.util.ArrayList;
import java.util.List;

public class MyBaseNavigationBar extends View {

    private final float density;
    private final ArgbEvaluator colorEval = new ArgbEvaluator();
    private final PathInterpolator m3Interpolator = new PathInterpolator(0.2f, 0f, 0f, 1f);

    private final List<String> tabs = new ArrayList<>();
    private final List<Drawable> tabIcons = new ArrayList<>();
    private final List<Integer> tabIds = new ArrayList<>();

    private int barBackgroundColor;
    private int activeColor;
    private int inactiveColor;
    private int activeTextColor;
    private int inactiveTextColor;
    private int indicatorColor;

    private TextPaint textPaint;
    private Paint indicatorPaint;
    private Paint barBgPaint;

    private int selectedPosition = 0;
    private float scrollPosition = 0f;

    private int pressedTabIndex = -1;
    private ValueAnimator scrollAnimator;

    private float[] tabPositionsLeft;
    private float[] tabPositionsRight;

    private final Path cutPath = new Path();
    private float baseCornerCutSize;

    @Nullable
    private OnItemSelectedListener listener;

    public interface OnItemSelectedListener {
        void onItemSelected(int index, @IdRes int itemId);
    }

    public MyBaseNavigationBar(Context context) {
        this(context, null);
    }

    public MyBaseNavigationBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        setClickable(true);

        baseCornerCutSize = 10f * density;

        // Warna Default dari Resource App
        barBackgroundColor = ContextCompat.getColor(getContext(), R.color.surfaceContainerHigh);
        activeColor = ContextCompat.getColor(getContext(), R.color.onSurface);
        inactiveColor = ContextCompat.getColor(getContext(), R.color.surfaceBright);
        activeTextColor = activeColor;
        inactiveTextColor = inactiveColor;
        indicatorColor = ContextCompat.getColor(getContext(), R.color.onPrimary);

        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setFakeBoldText(true);

        indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        indicatorPaint.setStyle(Paint.Style.FILL);

        barBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barBgPaint.setStyle(Paint.Style.FILL);

        // BACA ATRIBUT FRAMEWORK android:background
        int[] androidAttrs = new int[]{android.R.attr.background};
        TypedArray sysA = getContext().obtainStyledAttributes(attrs, androidAttrs);
        if (sysA.hasValue(0)) {
            barBackgroundColor = sysA.getColor(0, barBackgroundColor);
            super.setBackground(null);
        }
        sysA.recycle();

        // BACA CUSTOM ATRIBUT
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MyBaseNavigationBar);

            ColorStateList iconTint = a.getColorStateList(R.styleable.MyBaseNavigationBar_ax_itemIconTint);
            if (iconTint != null) {
                activeColor = iconTint.getColorForState(new int[]{android.R.attr.state_checked}, activeColor);
                inactiveColor = iconTint.getColorForState(new int[]{-android.R.attr.state_checked}, inactiveColor);
                activeTextColor = activeColor;
                inactiveTextColor = inactiveColor;
            }

            ColorStateList textTint = a.getColorStateList(R.styleable.MyBaseNavigationBar_ax_itemTextColor);
            if (textTint != null) {
                activeTextColor = textTint.getColorForState(new int[]{android.R.attr.state_checked}, activeColor);
                inactiveTextColor = textTint.getColorForState(new int[]{-android.R.attr.state_checked}, inactiveColor);
            }

            indicatorColor = a.getColor(R.styleable.MyBaseNavigationBar_ax_indicatorColor, indicatorColor);
            baseCornerCutSize = a.getDimension(R.styleable.MyBaseNavigationBar_ax_cornerCutSize, baseCornerCutSize);

            float elevation = a.getDimension(R.styleable.MyBaseNavigationBar_ax_elevation, 0f);
            if (elevation > 0) {
                setElevation(elevation);
            }

            int menuRes = a.getResourceId(R.styleable.MyBaseNavigationBar_ax_menu, 0);
            if (menuRes != 0) {
                inflateMenu(menuRes);
            }

            a.recycle();
        }
    }

    @Override
    public void setBackgroundColor(int color) {
        this.barBackgroundColor = color;
        invalidate();
    }

    @Override
    public void setBackground(Drawable background) {
        if (background instanceof ColorDrawable) {
            this.barBackgroundColor = ((ColorDrawable) background).getColor();
            super.setBackground(null);
            invalidate();
        } else {
            super.setBackground(background);
        }
    }

    public void setOnItemSelectedListener(@Nullable OnItemSelectedListener listener) {
        this.listener = listener;
    }

    public MyBaseNavigationBar tab(String title, Drawable icon, @IdRes int id) {
        tabs.add(title != null ? title : "");
        tabIcons.add(icon);
        tabIds.add(id);
        updateArrays();
        requestLayout();
        return this;
    }

    public void inflateMenu(@MenuRes int menuRes) {
        clearTabs();
        PopupMenu p = new PopupMenu(getContext(), null);
        p.inflate(menuRes);
        Menu menu = p.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            tab(item.getTitle() != null ? item.getTitle().toString() : "", item.getIcon(), item.getItemId());
        }
    }

    public void clearTabs() {
        tabs.clear();
        tabIcons.clear();
        tabIds.clear();
        selectedPosition = 0;
        scrollPosition = 0f;
        updateArrays();
        requestLayout();
    }

    private void updateArrays() {
        tabPositionsLeft = new float[tabs.size()];
        tabPositionsRight = new float[tabs.size()];
    }

    public void setChecked(int index) {
        if (index >= 0 && index < tabs.size() && index != selectedPosition) {
            selectedPosition = index;
            animateToTab(selectedPosition);
        }
    }

    public void setupWithViewPager2(@NonNull ViewPager2 viewPager) {
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                scrollPosition = position + positionOffset;
                invalidate();
            }

            @Override
            public void onPageSelected(int position) {
                selectedPosition = position;
                invalidate();
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // [UBAH] Baseline tinggi dinaikkan dari 56dp ke 64dp
        int desiredHeight = (int) (64f * density);
        int tabCount = Math.max(1, tabs.size());
        int desiredWidth = (int) ((tabCount * 70f + 24f) * density);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int resolvedWidth;

        if (widthMode == MeasureSpec.EXACTLY) {
            resolvedWidth = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            resolvedWidth = Math.min(desiredWidth, widthSize);
        } else {
            resolvedWidth = desiredWidth;
        }

        setMeasuredDimension(resolvedWidth, resolveSize(desiredHeight, heightMeasureSpec));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (tabs.isEmpty() || tabPositionsLeft == null) return false;

        float touchX = event.getX();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pressedTabIndex = -1;
                for (int i = 0; i < tabs.size(); i++) {
                    if (touchX >= tabPositionsLeft[i] && touchX <= tabPositionsRight[i]) {
                        pressedTabIndex = i;
                        break;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                if (pressedTabIndex != -1) {
                    int upIndex = -1;
                    for (int i = 0; i < tabs.size(); i++) {
                        if (touchX >= tabPositionsLeft[i] && touchX <= tabPositionsRight[i]) {
                            upIndex = i;
                            break;
                        }
                    }

                    if (upIndex == pressedTabIndex && upIndex >= 0 && upIndex < tabs.size()) {
                        selectedPosition = upIndex;
                        animateToTab(selectedPosition);

                        if (listener != null) {
                            int itemId = (selectedPosition < tabIds.size()) ? tabIds.get(selectedPosition) : selectedPosition;
                            listener.onItemSelected(selectedPosition, itemId);
                        }
                    }
                }
                pressedTabIndex = -1;
                break;

            case MotionEvent.ACTION_CANCEL:
                pressedTabIndex = -1;
                break;
        }
        return true;
    }

    private void animateToTab(int targetPosition) {
        if (scrollAnimator != null && scrollAnimator.isRunning()) {
            scrollAnimator.cancel();
        }
        scrollAnimator = ValueAnimator.ofFloat(scrollPosition, targetPosition);
        scrollAnimator.setDuration(300);
        scrollAnimator.setInterpolator(m3Interpolator);
        scrollAnimator.addUpdateListener(animation -> {
            scrollPosition = (float) animation.getAnimatedValue();
            invalidate();
        });
        scrollAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (tabs.isEmpty() || tabPositionsLeft == null) return;

        float w = getWidth();
        float h = getHeight();

        // [UBAH] Base Height disesuaikan dengan onMeasure (64dp)
        float baseHeight = 64f * density;
        float scale = (h > 0) ? (h / baseHeight) : 1f;

        // [UBAH] Parameter ukuran utama (Bisa dicustom di sini jika ingin lebih besar/kecil lagi)
        float iconSize = 30f * density * scale;       // Semula 22f -> Diperbesar ke 26f
        float spacing = 6f * density * scale;         // Semula 6f -> Diperbesar ke 8f
        float textSize = 18f * density * scale;        // Semula 13f -> Diperbesar ke 14f
        float currentCornerCut = baseCornerCutSize * scale;

        textPaint.setTextSize(textSize);

        // 1. Gambar Background Bar Utama
        barBgPaint.setColor(barBackgroundColor);
        drawCutCornerRect(canvas, 0, 0, w, h, currentCornerCut, barBgPaint);

        // 2. Hitung Bobot Lebar Tab
        float totalWeights = 0f;
        float[] weights = new float[tabs.size()];

        for (int i = 0; i < tabs.size(); i++) {
            float distance = Math.abs(scrollPosition - i);
            float activeWeight = Math.max(0f, Math.min(1f, 1f - distance));
            float interpWeight = m3Interpolator.getInterpolation(activeWeight);

            weights[i] = 1.0f + (1.0f * interpWeight);
            totalWeights += weights[i];
        }

        float currentX = 0f;
        for (int i = 0; i < tabs.size(); i++) {
            float calculatedTabWidth = (weights[i] / totalWeights) * w;

            tabPositionsLeft[i] = currentX;
            tabPositionsRight[i] = currentX + calculatedTabWidth;
            currentX += calculatedTabWidth;
        }

        // 3. Morphing Indikator Pemilih (Kotak Cut-Corner)
        int leftIndex = (int) Math.floor(scrollPosition);
        int rightIndex = (int) Math.ceil(scrollPosition);
        leftIndex = Math.max(0, Math.min(leftIndex, tabs.size() - 1));
        rightIndex = Math.max(0, Math.min(rightIndex, tabs.size() - 1));

        float fraction = scrollPosition - leftIndex;
        float segmentWeight = m3Interpolator.getInterpolation(fraction);

        float finalLeft = tabPositionsLeft[leftIndex] + ((tabPositionsLeft[rightIndex] - tabPositionsLeft[leftIndex]) * segmentWeight);
        float finalRight = tabPositionsRight[leftIndex] + ((tabPositionsRight[rightIndex] - tabPositionsRight[leftIndex]) * segmentWeight);

        indicatorPaint.setColor(indicatorColor);
        drawCutCornerRect(canvas, finalLeft, 0, finalRight, h, currentCornerCut, indicatorPaint);

        // 4. Render Ikon dan Teks
        for (int i = 0; i < tabs.size(); i++) {
            float tLeft = tabPositionsLeft[i];
            float tRight = tabPositionsRight[i];
            float cx = (tLeft + tRight) / 2f;
            float cy = h / 2f;

            float distance = Math.abs(scrollPosition - i);
            float activeWeight = Math.max(0f, Math.min(1f, 1f - distance));

            int currentIconColor = (Integer) colorEval.evaluate(activeWeight, inactiveColor, activeColor);
            int currentTextColor = (Integer) colorEval.evaluate(activeWeight, inactiveTextColor, activeTextColor);

            Drawable icon = (i < tabIcons.size()) ? tabIcons.get(i) : null;

            if (icon != null) {
                icon.setTint(currentIconColor);

                String title = tabs.get(i);
                float textWidth = textPaint.measureText(title);

                float shiftDistance = (textWidth + spacing) / 2f;

                float iconLeft = cx - (iconSize / 2f) - (shiftDistance * activeWeight);
                float textLeft = iconLeft + iconSize + spacing;

                int finalIconTop = (int) (cy - (iconSize / 2f));
                icon.setBounds((int) iconLeft, finalIconTop, (int) (iconLeft + iconSize), (int) (finalIconTop + iconSize));
                icon.draw(canvas);

                textPaint.setColor(currentTextColor);

                float textAlphaProgress = Math.max(0f, (activeWeight - 0.3f) / 0.7f);
                textPaint.setAlpha((int) (255 * textAlphaProgress));

                Paint.FontMetrics fm = textPaint.getFontMetrics();
                float textY = cy - ((fm.ascent + fm.descent) / 2f);
                canvas.drawText(title, textLeft, textY, textPaint);
            }
        }
    }

    private void drawCutCornerRect(Canvas canvas, float left, float top, float right, float bottom, float cut, Paint paint) {
        float safeCut = Math.min(cut, Math.min((right - left) / 2f, (bottom - top) / 2f));

        cutPath.reset();
        cutPath.moveTo(left + safeCut, top);
        cutPath.lineTo(right - safeCut, top);
        cutPath.lineTo(right, top + safeCut);
        cutPath.lineTo(right, bottom - safeCut);
        cutPath.lineTo(right - safeCut, bottom);
        cutPath.lineTo(left + safeCut, bottom);
        cutPath.lineTo(left, bottom - safeCut);
        cutPath.lineTo(left, top + safeCut);
        cutPath.close();

        canvas.drawPath(cutPath, paint);
    }
}