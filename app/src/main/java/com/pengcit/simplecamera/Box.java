package com.pengcit.simplecamera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/**
 * Created by Satria on 7/20/2016.
 */
public class Box extends View {
    private Paint paint = new Paint();
    Box(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) { // Override the onDraw() Method
        super.onDraw(canvas);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(10);

        //center
        int x0 = canvas.getWidth()/2;
        int y0 = canvas.getHeight()/2;
        int dx = canvas.getWidth()/2;
        int dy = canvas.getHeight()/3;
        //draw guide box
        canvas.drawRect(x0-dx+10, y0-dy, x0+dx-10, y0+80, paint);
    }
}