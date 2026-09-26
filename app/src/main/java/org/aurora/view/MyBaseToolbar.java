package com.aurora.manager.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.aurora.manager.R;

public class MyBaseToolbar extends LinearLayout {

    private TextView tvTitleLarge;
    private TextView tvTitleMedium;
    private TextView tvUserToken;

    public MyBaseToolbar(@NonNull Context context) {
        super(context);
        init(context);
    }

    public MyBaseToolbar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MyBaseToolbar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(@NonNull Context context) {
        setOrientation(HORIZONTAL);
        setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        LayoutInflater.from(context).inflate(R.layout.view_my_base_toolbar, this, true);

        tvTitleLarge = findViewById(R.id.title_large);
        tvTitleMedium = findViewById(R.id.title_medium);
        tvUserToken = findViewById(R.id.id_user_token);
    }

    public void setTitleLarge(@NonNull CharSequence title) {
        if (tvTitleLarge != null) {
            tvTitleLarge.setText(title);
        }
    }

    public void setTitleLarge(@StringRes int resId) {
        if (tvTitleLarge != null) {
            tvTitleLarge.setText(resId);
        }
    }

    public void setTitleMedium(@NonNull CharSequence subtitle) {
        if (tvTitleMedium != null) {
            tvTitleMedium.setText(subtitle);
        }
    }

    public void setTitleMedium(@StringRes int resId) {
        if (tvTitleMedium != null) {
            tvTitleMedium.setText(resId);
        }
    }

    public void setUserToken(@NonNull CharSequence token) {
        if (tvUserToken != null) {
            tvUserToken.setText(token);
        }
    }
}