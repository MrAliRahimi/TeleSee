package com.example.ali.udpvideochat.sample;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

@SuppressLint("AppCompatCustomView")
public class Font extends TextView {


    public Font(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public Font(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public Font(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        Typeface tf = Typeface.createFromAsset(context.getAssets(), "Material-Design-Iconic-Font.ttf");
        setTypeface(tf);
    }
}