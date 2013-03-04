
package com.example.drawingtest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.view.MotionEvent;
import android.view.View;

public class PointerLocationView extends View {
    private final Paint mPathPaint;
    private PointerState ps;

    public PointerLocationView(Context c) {
        super(c);
        setFocusable(true);
        mPathPaint = new Paint();
        mPathPaint.setAntiAlias(true);
        mPathPaint.setColor(Color.RED);
        mPathPaint.setStrokeWidth(4);
        mPathPaint.setStrokeCap(Cap.ROUND);

        ps = new PointerState();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int N = ps.mTraceCount;
        float lastX = 0, lastY = 0;
        boolean haveLast = false;
        for (int i = 0; i < N; i++) {
            float x = ps.mTraceX[i];
            float y = ps.mTraceY[i];
            if (Float.isNaN(x)) {
                haveLast = false;
                continue;
            }
            if (haveLast) {
                canvas.drawLine(lastX, lastY, x, y, mPathPaint);
            }
            lastX = x;
            lastY = y;
            haveLast = true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            ps.clearTrace();
        }

        for (int j = 0; j < event.getHistorySize(); j++) {
            ps.addTrace(event.getHistoricalX(j), event.getHistoricalY(j));
        }
        ps.addTrace(event.getX(), event.getY());

        if (action == MotionEvent.ACTION_UP) {
            ps.addTrace(Float.NaN, Float.NaN);
        }

        postInvalidate();

        return true;
    }
}
