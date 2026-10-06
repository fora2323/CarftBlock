package com.aurora.main.activities;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.widget.*;

import androidx.annotation.*;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import com.aurora.adapter.MyBaseFragmentAdapter;
import com.aurora.main.fragments.HomeFragment;
import com.aurora.main.fragments.MenuFragment;
import com.aurora.main.fragments.SettingsFragment;
import com.aurora.R;

import org.aurora.view.MyBaseNavigationBar;
import org.aurora.view.MyBaseToolbar;
import org.aurora.core.BaseCompatActivity;



public class MainActivity extends BaseCompatActivity {
	
	private ViewPager2 viewpager1;
	private MyBaseToolbar my_toolbar;
	private MyBaseNavigationBar nav_bar;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.main);
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		viewpager1 = findViewById(R.id.viewpager1);
		my_toolbar = findViewById(R.id.my_toolbar);
		nav_bar = findViewById(R.id.nav_bar);
	}
	
	private void initializeLogic() {
		applySystemBarInsets(findViewById(R.id._main), my_toolbar, nav_bar);
		MyBaseFragmentAdapter adapter = new MyBaseFragmentAdapter(this);
		
		adapter.addFragment(new HomeFragment());
		adapter.addFragment(new MenuFragment());
		adapter.addFragment(new SettingsFragment());
		
		viewpager1.setAdapter(adapter);
		
		nav_bar.setupWithViewPager2(viewpager1);
		
		nav_bar.setOnItemSelectedListener((index, itemId) -> {
			viewpager1.setCurrentItem(index, true);
		});
	}
	
}