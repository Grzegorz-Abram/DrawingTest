
package com.example.drawingtest;

import android.view.MotionEvent;

public class PointerState {
    // Trace of previous points.
    float[] mTraceX = new float[32];
    float[] mTraceY = new float[32];
    int mTraceCount;

    // True if the pointer is down.
    boolean mCurDown;

    // Most recent coordinates.
    MotionEvent.PointerCoords mCoords = new MotionEvent.PointerCoords();

    public void addTrace(float x, float y) {
        int traceCapacity = mTraceX.length;
        if (mTraceCount == traceCapacity) {
            traceCapacity *= 2;
            float[] newTraceX = new float[traceCapacity];
            System.arraycopy(mTraceX, 0, newTraceX, 0, mTraceCount);
            mTraceX = newTraceX;

            float[] newTraceY = new float[traceCapacity];
            System.arraycopy(mTraceY, 0, newTraceY, 0, mTraceCount);
            mTraceY = newTraceY;
        }

        mTraceX[mTraceCount] = x;
        mTraceY[mTraceCount] = y;
        mTraceCount += 1;
    }

    public void clearTrace() {
        mTraceCount = 0;
    }
}
