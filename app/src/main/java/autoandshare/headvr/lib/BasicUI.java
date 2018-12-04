package autoandshare.headvr.lib;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.google.vr.sdk.base.Eye;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import autoandshare.headvr.lib.headcontrol.HeadControl;
import autoandshare.headvr.lib.headcontrol.HeadMotion.Motion;
import autoandshare.headvr.lib.rendering.VRSurface;

public class BasicUI {
    private VRSurface uiVRSurface;
    private Paint iconPaint;
    private Paint progressPaint;
    private Paint progressLinePaint1;
    private Paint progressLinePaint2;
    private Paint errorPaint;
    private static final float textSize = 30;
    private static final float strokeWidth = 16;
    private static final float margin = 10;
    private static final float textY = -6;
    private static final float lineY = 45;
    private static final float uiWidth = 1.2f;

    public BasicUI() {

        uiVRSurface = new VRSurface(uiWidth, 0.15f, 3f, null);

        iconPaint = new Paint();
        iconPaint.setColor(Color.LTGRAY);
        iconPaint.setTextSize(textSize);

        progressPaint = new Paint();
        progressPaint.setColor(Color.LTGRAY);
        progressPaint.setTextSize(textSize);
        progressPaint.setTextAlign(Paint.Align.RIGHT);

        progressLinePaint1 = new Paint();
        progressLinePaint1.setColor(Color.LTGRAY);
        progressLinePaint1.setStrokeWidth(strokeWidth);

        progressLinePaint2 = new Paint();
        progressLinePaint2.setColor(Color.DKGRAY);
        progressLinePaint2.setStrokeWidth(strokeWidth);

        errorPaint = new Paint();
        errorPaint.setColor(Color.LTGRAY);
        errorPaint.setTextSize(textSize);
        errorPaint.setTextAlign(Paint.Align.CENTER);


    }

    public void glDraw(Eye eye, VideoRenderer.State videoState, HeadControl control) {
        if (eye.getType() == 1) {
            Canvas canvas = uiVRSurface.getCanvas();

            canvas.drawColor(Color.BLACK);

            if (videoState.errorMessage != null) {
                drawMotions(canvas, control);
                drawString(canvas, videoState.errorMessage, canvas.getWidth() / 2, 18, errorPaint);
            } else {
                drawMotions(canvas, control);
                drawStateIcon(canvas, videoState);
                drawProgress(canvas, videoState);
            }
            uiVRSurface.releaseCanvas(canvas);
        }
        uiVRSurface.draw(eye);

    }

    private void drawStateIcon(Canvas canvas, VideoRenderer.State videoState) {

        if (videoState.seeking) {
            if (videoState.forward) {
                drawStateString(canvas, "\u23E9");
            } else {
                drawStateString(canvas, "\u23EA");
            }
        } else {
            if (!videoState.playing) {
                drawStateString(canvas, "\u25B6");
            } else {
                drawStateString(canvas, "\u23F8");
            }
        }
    }

    private void drawStateString(Canvas canvas, String s) {
        drawString(canvas, s, margin, textY, iconPaint);
    }

    private void drawString(Canvas canvas, String s, float x, float y, Paint paint) {

        canvas.drawText(s, x,
                y + canvas.getHeight() / 2 - (paint.descent() + paint.ascent()) / 2,
                paint);
    }

    private void drawMotions(Canvas canvas, HeadControl control) {
        String string = "";
        if (control.getWaitForIdle()) {
            string = motionChar.get(Motion.IDLE);
        } else if (control.notIdle()) {
            string = motionString(control.getMotions());
        }
        drawString(canvas, string, 45, textY, iconPaint);
    }

    private void drawProgress(Canvas canvas, VideoRenderer.State videoState) {
        int showPosition = videoState.seeking ? videoState.newPosition : videoState.currentPosition;
        drawString(canvas, formatTime(showPosition) + " / " +
                        formatTime(videoState.videoLength),
                canvas.getWidth() - margin, textY, progressPaint);

        float begin = margin;
        float end = canvas.getWidth() - begin;
        float middle = begin + (end - begin) * showPosition / videoState.videoLength;
        float y = lineY;

        canvas.drawLine(begin, y, middle, y, progressLinePaint1);
        canvas.drawLine(middle, y, end, y, progressLinePaint2);
    }

    private String formatTime(int ms) {
        int seconds = ms / 1000;
        int minutes = seconds / 60;
        int hours = minutes / 60;
        if (hours > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes % 60, seconds % 60);
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds % 60);
        }
    }

    private static final HashMap<Motion, String> motionChar = new HashMap<>();

    static {
        motionChar.put(Motion.LEFT, "\u2190");
        motionChar.put(Motion.UP, "\u2191");
        motionChar.put(Motion.RIGHT, "\u2192");
        motionChar.put(Motion.DOWN, "\u2193");
        motionChar.put(Motion.IDLE, "\u2022");
    }

    private String motionString(List<Motion> motions) {
        StringBuilder string = new StringBuilder();
        for (int i = 0; (i < motions.size()) && (i < 5); i++) {
            string.append(motionChar.get(motions.get(i)));
        }
        return string.toString();
    }
}