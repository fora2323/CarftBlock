package org.aurora.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

@SuppressWarnings("deprecation")
public class BaseCompatActivity extends AppCompatActivity {
	
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
	}
	
	public void applySystemBarInsets(@NonNull View rootView, @Nullable View topView, @Nullable View bottomView) {
		final int initialTopMargin = (topView != null && topView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) ? ((ViewGroup.MarginLayoutParams) topView.getLayoutParams()).topMargin : 0;
		
		final int initialBottomMargin = (bottomView != null && bottomView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) ? ((ViewGroup.MarginLayoutParams) bottomView.getLayoutParams()).bottomMargin : 0;
		
		ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
			Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
			
			if (topView != null && topView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
				ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) topView.getLayoutParams();
				params.topMargin = initialTopMargin + systemBars.top;
				topView.setLayoutParams(params);
			}
			
			if (bottomView != null && bottomView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
				ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bottomView.getLayoutParams();
				params.bottomMargin = initialBottomMargin + systemBars.bottom;
				bottomView.setLayoutParams(params);
			}
			
			// Menyesuaikan padding ViewPager2 secara dinamis berdasarkan tinggi Toolbar & Navigation Bar + Insets
			v.post(() -> {
				View viewPager = v.findViewById(com.aurora.R.id.viewpager1);
				if (viewPager != null) {
					int topOffset = (topView != null) ? (systemBars.top + initialTopMargin + topView.getHeight()) : systemBars.top;
					int bottomOffset = (bottomView != null) ? (systemBars.bottom + initialBottomMargin + bottomView.getHeight()) : systemBars.bottom;
					
					// Reset margins agar ViewPager2 memenuhi seluruh layar
					if (viewPager.getLayoutParams() instanceof ViewGroup.MarginLayoutParams vpParams) {
						vpParams.topMargin = 0;
						vpParams.bottomMargin = 0;
						viewPager.setLayoutParams(vpParams);
					}

					// Terapkan Padding & Matikan ClipToPadding agar konten bisa tembus saat discroll
					viewPager.setPadding(0, topOffset, 0, bottomOffset);
					if (viewPager instanceof ViewGroup vg) {
						vg.setClipToPadding(false);
						vg.setClipChildren(false);
						if (vg.getChildCount() > 0) {
							View child = vg.getChildAt(0);
							if (child instanceof ViewGroup childVg) {
								childVg.setClipToPadding(false);
								childVg.setClipChildren(false);
							}
						}
					}
				}
			});
			
			return insets;
		});
	}
	
	@Override
	protected void attachBaseContext(@NonNull Context newBase) {
		String langCode = getSavedLanguage(newBase);
		super.attachBaseContext(updateResources(newBase, langCode));
	}
	
	@NonNull
	private Context updateResources(@NonNull Context context, @NonNull String language) {
		Locale locale = new Locale(language);
		Locale.setDefault(locale);
		
		Configuration config = new Configuration(context.getResources().getConfiguration());
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			config.setLocale(locale);
			LocaleList localeList = new LocaleList(locale);
			LocaleList.setDefault(localeList);
			config.setLocales(localeList);
			return context.createConfigurationContext(config);
		} else {
			config.locale = locale;
			context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
			return context;
		}
	}
	
	@NonNull
	private String getSavedLanguage(@NonNull Context context) {
		SharedPreferences prefs = context.getSharedPreferences("app_settings", MODE_PRIVATE);
		String lang = prefs.getString("app_language", "id");
		return (lang != null) ? lang : "id";
	}
	
	public void setAppLanguage(@NonNull String langCode) {
		SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
		prefs.edit().putString("app_language", langCode).apply();
		recreate();
	}
}