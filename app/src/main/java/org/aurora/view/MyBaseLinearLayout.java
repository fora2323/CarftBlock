package org.aurora.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aurora.R;

public class MyBaseLinearLayout extends LinearLayout {
	
	public static final int ROUND = 0;
	public static final int HEXAGON = 1;
	
	private int shapeType = ROUND;
	
	private float tl = 0f, tr = 0f, br = 0f, bl = 0f;
	private final Path path = new Path();
	private final RectF rect = new RectF();
	
	public MyBaseLinearLayout(@NonNull Context context) {
		super(context);
		init(context, null);
	}
	
	public MyBaseLinearLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}
	
	public MyBaseLinearLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context, attrs);
	}
	
	private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
		// Matikan Hardware Acceleration khusus View ini supaya clipPath motong background
		setLayerType(LAYER_TYPE_SOFTWARE, null);
		
		if (attrs != null) {
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MyBaseLinearLayout);
			
			shapeType = a.getInt(R.styleable.MyBaseLinearLayout_ax_shapeType, ROUND);
			
			float defaultRadius = a.getDimension(R.styleable.MyBaseLinearLayout_ax_radius, 0f);
			
			tl = a.getDimension(R.styleable.MyBaseLinearLayout_ax_radiusTopLeft, defaultRadius);
			tr = a.getDimension(R.styleable.MyBaseLinearLayout_ax_radiusTopRight, defaultRadius);
			br = a.getDimension(R.styleable.MyBaseLinearLayout_ax_radiusBottomRight, defaultRadius);
			bl = a.getDimension(R.styleable.MyBaseLinearLayout_ax_radiusBottomLeft, defaultRadius);
			
			a.recycle();
		}
	}
	
	public void setRadius(float topLeft, float topRight, float bottomRight, float bottomLeft) {
		this.tl = topLeft;
		this.tr = topRight;
		this.br = bottomRight;
		this.bl = bottomLeft;
		updatePath();
		invalidate();
	}
	
	public void setRadius(float radius) {
		this.tl = radius;
		this.tr = radius;
		this.br = radius;
		this.bl = radius;
		updatePath();
		invalidate();
	}
	
	public void setShapeType(int type) {
		this.shapeType = type;
		updatePath();
		invalidate();
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		updatePath();
	}
	
	private void updatePath() {
		float w = getWidth();
		float h = getHeight();
		
		if (w == 0 || h == 0) return;
		
		path.reset();
		
		if (shapeType == HEXAGON) {
			path.moveTo(0, tl);
			path.lineTo(tl, 0);
			
			path.lineTo(w - tr, 0);
			path.lineTo(w, tr);
			
			path.lineTo(w, h - br);
			path.lineTo(w - br, h);
			
			path.lineTo(bl, h);
			path.lineTo(0, h - bl);
			
			path.close();
		} else {
			rect.set(0, 0, w, h);
			path.addRoundRect(rect, new float[]{tl, tl, tr, tr, br, br, bl, bl}, Path.Direction.CW);
		}
	}
	
	@Override
	public void draw(@NonNull Canvas canvas) {
		int save = canvas.save();
		canvas.clipPath(path);
		super.draw(canvas);
		canvas.restoreToCount(save);
	}
}