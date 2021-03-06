
package com.example.drawingtest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.RectF;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.ArrayList;

public class PointerLocationView extends View {
    private static final String TAG = "Pointer";

    private final ViewConfiguration mVC;
    private final Paint mTextPaint;
    private final Paint mTextBackgroundPaint;
    private final Paint mTextLevelPaint;
    private final Paint mPaint;
    private final Paint mTargetPaint;
    private final Paint mPathPaint;
    private final FontMetricsInt mTextMetrics = new FontMetricsInt();
    private int mHeaderBottom;
    private boolean mCurDown;
    private int mCurNumPointers;
    private int mMaxNumPointers;
    private int mActivePointerId;
    private final ArrayList<PointerState> mPointers = new ArrayList<PointerState>();

    private final VelocityTracker mVelocity;

    private final FasterStringBuilder mText = new FasterStringBuilder();

    private boolean mPrintCoords = true;

    // Draw an oval. When angle is 0 radians, orients the major axis vertically,
    // angles less than or greater than 0 radians rotate the major axis left or
    // right.
    private RectF mReusableOvalRect = new RectF();

    public PointerLocationView(Context c) {
        super(c);
        setFocusable(true);
        mVC = ViewConfiguration.get(c);
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(10
                * getResources().getDisplayMetrics().density);
        mTextPaint.setARGB(255, 0, 0, 0);
        mTextBackgroundPaint = new Paint();
        mTextBackgroundPaint.setAntiAlias(false);
        mTextBackgroundPaint.setARGB(128, 255, 255, 255);
        mTextLevelPaint = new Paint();
        mTextLevelPaint.setAntiAlias(false);
        mTextLevelPaint.setARGB(192, 255, 0, 0);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setARGB(255, 255, 255, 255);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(2);
        mTargetPaint = new Paint();
        mTargetPaint.setAntiAlias(false);
        mTargetPaint.setARGB(255, 0, 0, 192);
        mPathPaint = new Paint();
        mPathPaint.setAntiAlias(false);
        mPathPaint.setARGB(255, 0, 96, 255);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(1);

        PointerState ps = new PointerState();
        mPointers.add(ps);
        mActivePointerId = 0;

        mVelocity = VelocityTracker.obtain();

        logInputDeviceCapabilities();
    }

    public void addTouchEvent(MotionEvent event) {
        synchronized (mPointers) {
            int action = event.getAction();

            // Log.i(TAG, "Motion: action=0x" + Integer.toHexString(action)
            // + " pointers=" + event.getPointerCount());

            int NP = mPointers.size();

            // mRect.set(0, 0, getWidth(), mHeaderBottom+1);
            // invalidate(mRect);
            // if (mCurDown) {
            // mRect.set(mCurX-mCurWidth-3, mCurY-mCurWidth-3,
            // mCurX+mCurWidth+3, mCurY+mCurWidth+3);
            // } else {
            // mRect.setEmpty();
            // }
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
                    mVelocity.clear();
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
                if (mPrintCoords) {
                    Log.i(TAG, mText.clear().append("Pointer ")
                            .append(id + 1).append(": DOWN").toString());
                }
            }

            final int NI = event.getPointerCount();

            mCurDown = action != MotionEvent.ACTION_UP
                    && action != MotionEvent.ACTION_CANCEL;
            mCurNumPointers = mCurDown ? NI : 0;
            if (mMaxNumPointers < mCurNumPointers) {
                mMaxNumPointers = mCurNumPointers;
            }

            mVelocity.addMovement(event);
            mVelocity.computeCurrentVelocity(1);

            for (int i = 0; i < NI; i++) {
                final int id = event.getPointerId(i);
                final PointerState ps = mPointers.get(id);
                final int N = event.getHistorySize();
                for (int j = 0; j < N; j++) {
                    event.getHistoricalPointerCoords(i, j, ps.mCoords);
                    if (mPrintCoords) {
                        logPointerCoords(ps.mCoords, id);
                    }
                    ps.addTrace(event.getHistoricalX(i, j), event.getHistoricalY(i, j));
                }
                event.getPointerCoords(i, ps.mCoords);
                if (mPrintCoords) {
                    logPointerCoords(ps.mCoords, id);
                }
                ps.addTrace(ps.mCoords.x, ps.mCoords.y);
                ps.mXVelocity = mVelocity.getXVelocity(id);
                ps.mYVelocity = mVelocity.getYVelocity(id);
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
                if (mPrintCoords) {
                    Log.i(TAG, mText.clear().append("Pointer ")
                            .append(id + 1).append(": UP").toString());
                }

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

            // if (mCurDown) {
            // mRect.union(mCurX-mCurWidth-3, mCurY-mCurWidth-3,
            // mCurX+mCurWidth+3, mCurY+mCurWidth+3);
            // }
            // invalidate(mRect);
            postInvalidate();
        }
    }

    private void drawOval(Canvas canvas, float x, float y, float major, float minor,
            float angle, Paint paint) {
        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        canvas.rotate((float) (angle * 180 / Math.PI), x, y);
        mReusableOvalRect.left = x - minor / 2;
        mReusableOvalRect.right = x + minor / 2;
        mReusableOvalRect.top = y - major / 2;
        mReusableOvalRect.bottom = y + major / 2;
        canvas.drawOval(mReusableOvalRect, paint);
        canvas.restore();
    }

    private void logInputDeviceCapabilities() {
        int[] deviceIds = InputDevice.getDeviceIds();
        for (int i = 0; i < deviceIds.length; i++) {
            InputDevice device = InputDevice.getDevice(deviceIds[i]);
            if (device != null) {
                Log.i(TAG, device.toString());
            }
        }
    }

    private void logPointerCoords(MotionEvent.PointerCoords coords, int id) {
        Log.i(TAG, mText.clear()
                .append("Pointer ").append(id + 1)
                .append(": (").append(coords.x, 3).append(", ").append(coords.y, 3)
                .append(") Pressure=").append(coords.pressure, 3)
                .append(" Size=").append(coords.size, 3)
                .append(" TouchMajor=").append(coords.touchMajor, 3)
                .append(" TouchMinor=").append(coords.touchMinor, 3)
                .append(" ToolMajor=").append(coords.toolMajor, 3)
                .append(" ToolMinor=").append(coords.toolMinor, 3)
                .append(" Orientation=").append((float) (coords.orientation * 180 / Math.PI), 1)
                .append("deg").toString());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        synchronized (mPointers) {
            final int w = getWidth();
            final int itemW = w / 7;
            final int base = -mTextMetrics.ascent + 1;
            final int bottom = mHeaderBottom;

            final int NP = mPointers.size();

            // Labels
            if (mActivePointerId >= 0) {
                final PointerState ps = mPointers.get(mActivePointerId);

                canvas.drawRect(0, 0, itemW - 1, bottom, mTextBackgroundPaint);
                canvas.drawText(mText.clear()
                        .append("P: ").append(mCurNumPointers)
                        .append(" / ").append(mMaxNumPointers)
                        .toString(), 1, base, mTextPaint);

                final int N = ps.mTraceCount;
                if ((mCurDown && ps.mCurDown) || N == 0) {
                    canvas.drawRect(itemW, 0, (itemW * 2) - 1, bottom, mTextBackgroundPaint);
                    canvas.drawText(mText.clear()
                            .append("X: ").append(ps.mCoords.x, 1)
                            .toString(), 1 + itemW, base, mTextPaint);
                    canvas.drawRect(itemW * 2, 0, (itemW * 3) - 1, bottom, mTextBackgroundPaint);
                    canvas.drawText(mText.clear()
                            .append("Y: ").append(ps.mCoords.y, 1)
                            .toString(), 1 + itemW * 2, base, mTextPaint);
                } else {
                    float dx = ps.mTraceX[N - 1] - ps.mTraceX[0];
                    float dy = ps.mTraceY[N - 1] - ps.mTraceY[0];
                    canvas.drawRect(itemW, 0, (itemW * 2) - 1, bottom,
                            Math.abs(dx) < mVC.getScaledTouchSlop()
                                    ? mTextBackgroundPaint : mTextLevelPaint);
                    canvas.drawText(mText.clear()
                            .append("dX: ").append(dx, 1)
                            .toString(), 1 + itemW, base, mTextPaint);
                    canvas.drawRect(itemW * 2, 0, (itemW * 3) - 1, bottom,
                            Math.abs(dy) < mVC.getScaledTouchSlop()
                                    ? mTextBackgroundPaint : mTextLevelPaint);
                    canvas.drawText(mText.clear()
                            .append("dY: ").append(dy, 1)
                            .toString(), 1 + itemW * 2, base, mTextPaint);
                }

                canvas.drawRect(itemW * 3, 0, (itemW * 4) - 1, bottom, mTextBackgroundPaint);
                canvas.drawText(mText.clear()
                        .append("Xv: ").append(ps.mXVelocity, 3)
                        .toString(), 1 + itemW * 3, base, mTextPaint);

                canvas.drawRect(itemW * 4, 0, (itemW * 5) - 1, bottom, mTextBackgroundPaint);
                canvas.drawText(mText.clear()
                        .append("Yv: ").append(ps.mYVelocity, 3)
                        .toString(), 1 + itemW * 4, base, mTextPaint);

                canvas.drawRect(itemW * 5, 0, (itemW * 6) - 1, bottom, mTextBackgroundPaint);
                canvas.drawRect(itemW * 5, 0, (itemW * 5) + (ps.mCoords.pressure * itemW) - 1,
                        bottom, mTextLevelPaint);
                canvas.drawText(mText.clear()
                        .append("Prs: ").append(ps.mCoords.pressure, 2)
                        .toString(), 1 + itemW * 5, base, mTextPaint);

                canvas.drawRect(itemW * 6, 0, w, bottom, mTextBackgroundPaint);
                canvas.drawRect(itemW * 6, 0, (itemW * 6) + (ps.mCoords.size * itemW) - 1,
                        bottom, mTextLevelPaint);
                canvas.drawText(mText.clear()
                        .append("Size: ").append(ps.mCoords.size, 2)
                        .toString(), 1 + itemW * 6, base, mTextPaint);
            }

            // Pointer trace.
            for (int p = 0; p < NP; p++) {
                final PointerState ps = mPointers.get(p);

                // Draw path.
                final int N = ps.mTraceCount;
                float lastX = 0, lastY = 0;
                boolean haveLast = false;
                boolean drawn = false;
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
                        drawn = true;
                    }
                    lastX = x;
                    lastY = y;
                    haveLast = true;
                }

                // Draw velocity vector.
                if (drawn) {
                    mPaint.setARGB(255, 255, 64, 128);
                    float xVel = ps.mXVelocity * (1000 / 60);
                    float yVel = ps.mYVelocity * (1000 / 60);
                    canvas.drawLine(lastX, lastY, lastX + xVel, lastY + yVel, mPaint);
                }

                if (mCurDown && ps.mCurDown) {
                    // Draw crosshairs.
                    canvas.drawLine(0, ps.mCoords.y, getWidth(), ps.mCoords.y, mTargetPaint);
                    canvas.drawLine(ps.mCoords.x, 0, ps.mCoords.x, getHeight(), mTargetPaint);

                    // Draw current point.
                    int pressureLevel = (int) (ps.mCoords.pressure * 255);
                    mPaint.setARGB(255, pressureLevel, 255, 255 - pressureLevel);
                    canvas.drawPoint(ps.mCoords.x, ps.mCoords.y, mPaint);

                    // Draw current touch ellipse.
                    mPaint.setARGB(255, pressureLevel, 255 - pressureLevel, 128);
                    drawOval(canvas, ps.mCoords.x, ps.mCoords.y, ps.mCoords.touchMajor,
                            ps.mCoords.touchMinor, ps.mCoords.orientation, mPaint);

                    // Draw current tool ellipse.
                    mPaint.setARGB(255, pressureLevel, 128, 255 - pressureLevel);
                    drawOval(canvas, ps.mCoords.x, ps.mCoords.y, ps.mCoords.toolMajor,
                            ps.mCoords.toolMinor, ps.mCoords.orientation, mPaint);
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mTextPaint.getFontMetricsInt(mTextMetrics);
        mHeaderBottom = -mTextMetrics.ascent + mTextMetrics.descent + 2;
        if (false) {
            Log.i("foo", "Metrics: ascent=" + mTextMetrics.ascent
                    + " descent=" + mTextMetrics.descent
                    + " leading=" + mTextMetrics.leading
                    + " top=" + mTextMetrics.top
                    + " bottom=" + mTextMetrics.bottom);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        addTouchEvent(event);
        return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        Log.i(TAG, "Trackball: " + event);
        return super.onTrackballEvent(event);
    }

    public void setPrintCoords(boolean state) {
        mPrintCoords = state;
    }
}
