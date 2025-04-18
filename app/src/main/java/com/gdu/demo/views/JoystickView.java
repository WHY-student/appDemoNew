package com.gdu.demo.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.gdu.demo.R;

public class JoystickView  extends SurfaceView implements
        SurfaceHolder.Callback, View.OnTouchListener {

    private Bitmap mJoystick;
    private SurfaceHolder mHolder;
    private Rect mKnobBounds;

    private JoystickThread mThread;

    private int mKnobX, mKnobY;
    private int mKnobSize;
    private int mBackgroundSize;
    private float mRadius;

    private OnJoystickListener mJoystickListener;

    private boolean mAutoCentering = true;

    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initGraphics(attrs);
        init();
    }

    private void initGraphics(AttributeSet attrs) {
        Resources res = getContext().getResources();
        mJoystick = BitmapFactory
                .decodeResource(
                        res, R.mipmap.joystick);

    }

    private void initBounds(final Canvas pCanvas) {
        mBackgroundSize = pCanvas.getHeight();
        mKnobSize = Math.round(mBackgroundSize * 0.6f);

        mKnobBounds = new Rect();

        mRadius = mBackgroundSize * 0.5f;
        mKnobX = Math.round((mBackgroundSize - mKnobSize) * 0.5f);
        mKnobY = Math.round((mBackgroundSize - mKnobSize) * 0.5f);

    }

    private void init() {
        mHolder = getHolder();
        mHolder.addCallback(this);

        mThread = new JoystickThread();

        setZOrderOnTop(true);
        mHolder.setFormat(PixelFormat.TRANSPARENT);
        setOnTouchListener(this);
        setEnabled(true);
        setAutoCentering(true);
    }

    public void setAutoCentering(final boolean pAutoCentering) {
        mAutoCentering = pAutoCentering;
    }

    public boolean isAutoCentering() {
        return mAutoCentering;
    }

    public void setJoystickListener( final OnJoystickListener pJoystickListener) {
        mJoystickListener = pJoystickListener;
    }

    @Override
    public void surfaceChanged(final SurfaceHolder arg0, final int arg1,
                               final int arg2, final int arg3) {

//		mThread.setRunning(false);
    }

    @Override
    public void surfaceCreated(final SurfaceHolder arg0) {
        mThread.start();

    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder arg0) {
        boolean retry = true;
        mThread.setRunning(false);

        while (retry) {
            try {
                // code to kill Thread
                mThread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }

    }

    public void doDraw(final Canvas pCanvas) {
        if (mKnobBounds == null) {
            initBounds(pCanvas);
        }

        // pCanvas.drawBitmap(mJoystickBg, null, mBgBounds, null);

        mKnobBounds.set(mKnobX, mKnobY, mKnobX + mKnobSize, mKnobY + mKnobSize);
        pCanvas.drawBitmap(mJoystick, null, mKnobBounds, null);
    }

    @Override
    public boolean onTouch(final View arg0, final MotionEvent pEvent) {
        final float x = pEvent.getX();
        final float y = pEvent.getY();

        switch (pEvent.getAction()) {

            case MotionEvent.ACTION_UP:
                if (isAutoCentering()) {
                    mKnobX = Math.round((mBackgroundSize - mKnobSize) * 0.5f);
                    mKnobY = Math.round((mBackgroundSize - mKnobSize) * 0.5f);
                }
                break;
            default:
                // Check if coordinates are in bounds. If they aren't move the knob
                // to the closest coordinate inbounds.
                if (checkBounds(x, y)) {
                    mKnobX = Math.round(x - mKnobSize * 0.5f);
                    mKnobY = Math.round(y - mKnobSize * 0.5f);
                } else {
                    final double angle = Math.atan2(y - mRadius, x - mRadius);
                    mKnobX = (int) (Math.round(mRadius
                            + (mRadius - mKnobSize * 0.5f) * Math.cos(angle)) - mKnobSize * 0.5f);
                    mKnobY = (int) (Math.round(mRadius
                            + (mRadius - mKnobSize * 0.5f) * Math.sin(angle)) - mKnobSize * 0.5f);
                }
        }

        if (mJoystickListener != null) {
            mJoystickListener.onTouch(this,
                    (0.5f - (mKnobX / (mRadius * 2 - mKnobSize))) * -2,
                    (0.5f - (mKnobY / (mRadius * 2 - mKnobSize))) * 2);

        }

        return true;
    }

    private boolean checkBounds(final float pX, final float pY) {
        return Math.pow(mRadius - pX, 2) + Math.pow(mRadius - pY, 2) <= Math
                .pow(mRadius - mKnobSize * 0.5f, 2);
    }

    private void pushTouchEvent(){

        if (mJoystickListener != null) {
            mJoystickListener.onTouch(this,
                    (0.5f - (mKnobX / (mRadius * 2 - mKnobSize))) * -2,
                    (0.5f - (mKnobY / (mRadius * 2 - mKnobSize))) * 2);

        }
    }

    private class JoystickThread extends Thread {

        private boolean running = false;

        @Override
        public synchronized void start() {
            if (!running && this.getState() == State.NEW) {
                running = true;
                super.start();
            }
        }

        public void setRunning(final boolean pRunning) {
            running = pRunning;
        }

        @Override
        public void run() {
            while (running) {
                // draw everything to the canvas
                Canvas canvas = null;
                try {
                    canvas = mHolder.lockCanvas(null);
                    synchronized (mHolder) {
                        // reset canvas
                        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        doDraw(canvas);
                    }
                }
                catch(Exception e){}
                finally {
                    if (canvas != null) {
                        mHolder.unlockCanvasAndPost(canvas);
                    }
                }

                pushTouchEvent();

                try
                {
                    Thread.sleep(100);
                } catch (InterruptedException ignored)
                {
                }
            }
        }
    }

}
