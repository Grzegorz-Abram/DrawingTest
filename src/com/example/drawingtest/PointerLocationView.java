
package com.example.drawingtest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class PointerLocationView extends View {
    private final Paint mPaint;
    private final Paint mPathPaint;
    private boolean mCurDown;
    private int mCurNumPointers;
    private int mMaxNumPointers;
    private int mActivePointerId;
    private final ArrayList<PointerState> mPointers = new ArrayList<PointerState>();

    public PointerLocationView(Context c) {
        super(c);
        setFocusable(true);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setARGB(255, 255, 255, 255);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(2);
        mPathPaint = new Paint();
        mPathPaint.setAntiAlias(false);
        mPathPaint.setARGB(255, 0, 96, 255);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(1);

        PointerState ps = new PointerState();
        mPointers.add(ps);
        mActivePointerId = 0;
    }

    public void addTouchEvent(MotionEvent event) {
        synchronized (mPointers) {
            int action = event.getAction();

            int NP = mPointers.size();

            if (action == MotionEvent.ACTION_DOWN
                    || (action & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN) {
                final int index = (action & MotionEvent.ACTION_POINTER_INDEX_MASK)
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT; // will be 0
                                                                   // for down
                if (action == MotionEvent.ACTION_DOWN) {
                    for (int p = 0; p < NP; p++) {
                        final PointerState ps = mPointers.get(p);
                        ps.clearTrace();
                        ps.mCurDown = false;
                    }
                    mCurDown = true;
                    mMaxNumPointers = 0;
                }

                final int id = event.getPointerId(index);
                while (NP <= id) {
                    PointerState ps = new PointerState();
                    mPointers.add(ps);
                    NP++;
                }

                if (mActivePointerId < 0 ||
                        !mPointers.get(mActivePointerId).mCurDown) {
                    mActivePointerId = id;
                }

                final PointerState ps = mPointers.get(id);
                ps.mCurDown = true;
            }

            final int NI = event.getPointerCount();

            mCurDown = action != MotionEvent.ACTION_UP
                    && action != MotionEvent.ACTION_CANCEL;
            mCurNumPointers = mCurDown ? NI : 0;
            if (mMaxNumPointers < mCurNumPointers) {
                mMaxNumPointers = mCurNumPointers;
            }

            for (int i = 0; i < NI; i++) {
                final int id = event.getPointerId(i);
                final PointerState ps = mPointers.get(id);
                final int N = event.getHistorySize();
                for (int j = 0; j < N; j++) {
                    event.getHistoricalPointerCoords(i, j, ps.mCoords);
                    ps.addTrace(event.getHistoricalX(i, j), event.getHistoricalY(i, j));
                }
                event.getPointerCoords(i, ps.mCoords);
                ps.addTrace(ps.mCoords.x, ps.mCoords.y);
            }

            if (action == MotionEvent.ACTION_UP
                    || action == MotionEvent.ACTION_CANCEL
                    || (action & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_UP) {
                final int index = (action & MotionEvent.ACTION_POINTER_INDEX_MASK)
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT; // will be 0
                                                                   // for UP

                final int id = event.getPointerId(index);
                final PointerState ps = mPointers.get(id);
                ps.mCurDown = false;

                if (action == MotionEvent.ACTION_UP
                        || action == MotionEvent.ACTION_CANCEL) {
                    mCurDown = false;
                } else {
                    if (mActivePointerId == id) {
                        mActivePointerId = event.getPointerId(index == 0 ? 1 : 0);
                    }
                    ps.addTrace(Float.NaN, Float.NaN);
                }
            }

            postInvalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        synchronized (mPointers) {
            final int NP = mPointers.size();

            // Pointer trace.
            for (int p = 0; p < NP; p++) {
                final PointerState ps = mPointers.get(p);

                // Draw path.
                final int N = ps.mTraceCount;
                float lastX = 0, lastY = 0;
                boolean haveLast = false;
                mPaint.setARGB(255, 128, 255, 255);
                for (int i = 0; i < N; i++) {
                    float x = ps.mTraceX[i];
                    float y = ps.mTraceY[i];
                    if (Float.isNaN(x)) {
                        haveLast = false;
                        continue;
                    }
                    if (haveLast) {
                        canvas.drawLine(lastX, lastY, x, y, mPathPaint);
                        canvas.drawPoint(lastX, lastY, mPaint);
                    }
                    lastX = x;
                    lastY = y;
                    haveLast = true;
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        addTouchEvent(event);
        return true;
    }
}
