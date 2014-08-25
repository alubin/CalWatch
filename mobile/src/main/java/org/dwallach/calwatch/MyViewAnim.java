package org.dwallach.calwatch;

import android.animation.Animator;
import android.animation.TimeAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by dwallach on 8/12/14.
 *
 * Useful source for understanding this crap:
 * http://danielnadeau.blogspot.com/2012/01/android-canvas-beginners-tutorial.html
 *
 * Also useful:
 * http://www.compiletimeerror.com/2013/09/introduction-to-2d-drawing-in-android.html
 *
 * TODO: eventually we're going to want to have multiple instances of this running
 * at the same time (e.g., three buttons to select the clockface type, all displaying
 * running clocks). If we instantiate this three times, it's not going to play correctly
 * with PhoneActivity, which is currently built to assume there's only one clockface. The
 * necessary change is going to be having each face displayed its own way, having the
 * activity just interact with the radio-button preferences, etc.
 */
public class MyViewAnim extends SurfaceView implements SurfaceHolder.Callback {
    private PanelThread drawThread;
    private static final float freqUpdate = 5;  // 5 Hz, or 0.20sec for second hand
    private ClockFace clockFace;
    private TimeAnimator animator;

    public MyViewAnim(Context context) {
        super(context);
        setup(context);
    }

    public MyViewAnim(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    private void setup(Context ctx) {
        PhoneActivity.textOut("setup!");
        getHolder().addCallback(this);
        clockFace = new ClockFace(ctx);
        PhoneActivity.getSingletonActivity().setClockFace(clockFace);
        PhoneActivity.getSingletonActivity().loadPreferences();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // clockFace.drawEverything(canvas);
        // For now, we're doing *nothing* here. Instead, all the drawing is going
        // to happen on the PanelThread or Animator (whichever we're doing in the end)
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        PhoneActivity.textOut("(MyViewAnim) Drawing surface changed!");
        clockFace.setSize(width, height);
        resume();

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        PhoneActivity.textOut("Drawing surface created!");
        resume();
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        PhoneActivity.textOut("Drawing surface destroyed!");
        stop();
    }

    public void pause() {
        PhoneActivity.textOut("pausing animation");
        // animator.pause();
        stop();
    }

    public void resume() {
        if(animator != null) {
            PhoneActivity.textOut("resuming old animator!");
            animator.resume();
        } else {
            PhoneActivity.textOut("new animator starting");
            animator = new TimeAnimator();
            animator.setTimeListener(new MyTimeListener(getHolder(), this));
            // animator.setFrameDelay(1000);  // doesn't actually work?

            if(drawThread == null) {
                drawThread = new PanelThread(animator); // will start the animator
                drawThread.start();
            } else {
                // animator.start() needs to happen on the PanelThread, not this one
                Handler handler = drawThread.getHandler();
                final Animator fa = animator;

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        fa.start();
                    }
                });
            }
        }
        PhoneActivity.getSingletonActivity().loadPreferences();
    }

    public void stop() {
        PhoneActivity.textOut("stopping animation!");
        if(animator != null) {
            // new experimental ways to maybe quit things
            if(drawThread == null) {
                Log.v("MyViewAnim", "no draw thread around to kill ?!");
            } else {
                // animator.start() needs to happen on the PanelThread, not this one
                Handler handler = drawThread.getHandler();

                /*
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        fa.start();
                    }
                });
                */

                Looper looper = handler.getLooper();
                if(looper != null)
                    looper.quitSafely();
                animator = null;
                drawThread = null;
            }
        }
    }

    class MyTimeListener implements TimeAnimator.TimeListener {
        private SurfaceHolder surfaceHolder;
        // private double fps = 0.0;

        public MyTimeListener(SurfaceHolder surfaceHolder, MyViewAnim panel) {
            this.surfaceHolder = surfaceHolder;

            Log.v("MyViewAnim", "Time listener is up!");
        }

        private int ticks = 0;
        private long lastFPSTime = 0;

        @Override
        public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
            Canvas c = null;

            ticks++;

            try {
                // Old technique: deriving FPS from totalTime and deltaTime
                // http://cogitolearning.co.uk/?p=1555

                // New technique: measure the system clock every 500 frames, compute
                // average FPS over the interval. Assuming we're blazing away at 50Hz,
                // this should log/print something every ten seconds. Eventually,
                // we'll just comment this out altogether or stretch it out to a much
                // longer time interval.
                if(lastFPSTime == 0)
                    lastFPSTime = System.currentTimeMillis();
                else {
                    if(ticks % 500 == 0) {
                        long currentTime = System.currentTimeMillis();
                        float fps = 500000.0f / (currentTime - lastFPSTime); // 500 frame * 1000 ms/s / elapsed ms
                        lastFPSTime = currentTime;
                        Log.v("FPS", Float.toString(fps));
                    }
                }
                /*
                double currentFps;
                if (deltaTime != 0)
                    currentFps = 1000.0 / (double) deltaTime;
                else
                    currentFps = 0.9 * fps;
                if (fps<0.0)
                    fps = currentFps;
                else
                    fps = 0.9*fps + 0.1*currentFps;

                if(ticks % 500 == 0)
                    PhoneActivity.textOut("Fps: "+ Double.toString(fps));
                */




                try {
                    c = surfaceHolder.lockCanvas(null);
                } catch (IllegalStateException e) {
                    c = null;
                    // the canvas is gone; we can't draw anything; bail!
                }

                if(c == null) {
                    if(ticks % 1000 == 0) Log.w("MyViewAnim", "Failed to get a canvas for drawing!");
                    return;
                }

                c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                clockFace.drawEverything(c);



            } finally {
                if (c != null) {
                    surfaceHolder.unlockCanvasAndPost(c);
                }
            }
        }
    }
    /**
     * Understanding a Looper: http://stackoverflow.com/questions/7597742/what-is-the-purpose-of-looper-and-how-to-use-it
     */
    class PanelThread extends Thread {
        Handler handler = null;
        Animator animator;

        public PanelThread(Animator animator) {
            this.animator = animator;
        }

        public Handler getHandler() {
            return handler;
        }

        @Override
        public void run() {
            try {
                // preparing a looper on current thread
                // the current thread is being detected implicitly
                Looper.prepare();

                // this needs to happen on the same thread
                animator.start();

                // now, the handler will automatically bind to the
                // Looper that is attached to the current thread
                // You don't need to specify the Looper explicitly
                handler = new Handler();

                // After the following line the thread will start
                // running the message loop and will not normally
                // exit the loop unless a problem happens or you
                // quit() the looper (see below)
                Looper.loop();

                Log.v("MyViewAnim", "looper finished!");
            } catch (Throwable t) {
                Log.e("MyViewAnim", "looper halted due to an error", t);
            }
        }
    }
}


/*
Notes: I once got this exception after switching from home back to the app.  Hmm. Can't repro.

08-15 12:50:27.318    3262-3262/edu.rice.dwallach.test2 E/HardwareRenderer﹕ An error has occurred while drawing:
    java.lang.IllegalStateException: The display list is not valid.
            at android.view.GLES20DisplayList.getNativeDisplayList(GLES20DisplayList.java:49)
            at android.view.GLES20Canvas.drawDisplayList(GLES20Canvas.java:420)
            at android.view.HardwareRenderer$GlRenderer.drawDisplayList(HardwareRenderer.java:1646)
            at android.view.HardwareRenderer$GlRenderer.draw(HardwareRenderer.java:1469)
            at android.view.ViewRootImpl.draw(ViewRootImpl.java:2381)
            at android.view.ViewRootImpl.performDraw(ViewRootImpl.java:2253)
            at android.view.ViewRootImpl.performTraversals(ViewRootImpl.java:1883)
            at android.view.ViewRootImpl.doTraversal(ViewRootImpl.java:1000)
            at android.view.ViewRootImpl$TraversalRunnable.run(ViewRootImpl.java:5670)
            at android.view.Choreographer$CallbackRecord.run(Choreographer.java:761)
            at android.view.Choreographer.doCallbacks(Choreographer.java:574)
            at android.view.Choreographer.doFrame(Choreographer.java:544)
            at android.view.Choreographer$FrameDisplayEventReceiver.run(Choreographer.java:747)
            at android.os.Handler.handleCallback(Handler.java:733)
            at android.os.Handler.dispatchMessage(Handler.java:95)
            at android.os.Looper.loop(Looper.java:136)
            at android.app.ActivityThread.main(ActivityThread.java:5017)
            at java.lang.reflect.Method.invokeNative(Native Method)
            at java.lang.reflect.Method.invoke(Method.java:515)
            at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:779)
            at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:595)
            at dalvik.system.NativeStart.main(Native Method)
 */
