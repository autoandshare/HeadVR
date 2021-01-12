package autoandshare.headvr.lib;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.text.TextPaint;
import android.text.TextUtils;

import com.google.vr.sdk.base.Eye;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import autoandshare.headvr.lib.controller.headcontrol.HeadControl;
import autoandshare.headvr.lib.controller.headcontrol.HeadMotion.Motion;
import autoandshare.headvr.lib.rendering.VRSurface;

public class BasicUI {
    private VRSurface uiVRSurface;
    private TextPaint leftAlignTextPaint;
    private Paint rightAlignTextPaint;
    private Paint centerAlignTextPaint;
    private Paint progressLinePaint1;
    private Paint progressLinePaint2;
    private Paint progressLinePaint3;

    // UI has 3 rows
    private float row0Y;
    private float row1Y;
    private float row2Y;
    private float beginX;
    private float endX;
    private float motionsX;

    public BasicUI() {

        float widthPixel = 920;
        float heightPixel = 138;

        initSurface(widthPixel, heightPixel);

        float textSize = 30;
        float strokeWidth = 16;
        float margin = 10;


        row0Y = heightPixel / 6;
        row1Y = heightPixel * 3 / 6;
        row2Y = heightPixel * 5 / 6;

        beginX = margin;
        endX = widthPixel - margin;
        motionsX = margin + (endX - beginX) / 3;

        leftAlignTextPaint = new TextPaint();
        leftAlignTextPaint.setColor(Color.LTGRAY);
        leftAlignTextPaint.setTextSize(textSize);
        leftAlignTextPaint.setTextAlign(Paint.Align.LEFT);

        rightAlignTextPaint = new Paint();
        rightAlignTextPaint.setColor(Color.LTGRAY);
        rightAlignTextPaint.setTextSize(textSize);
        rightAlignTextPaint.setTextAlign(Paint.Align.RIGHT);

        centerAlignTextPaint = new Paint();
        centerAlignTextPaint.setColor(Color.LTGRAY);
        centerAlignTextPaint.setTextSize(textSize);
        centerAlignTextPaint.setTextAlign(Paint.Align.LEFT);

        progressLinePaint1 = new Paint();
        progressLinePaint1.setColor(Color.LTGRAY);
        progressLinePaint1.setStrokeWidth(strokeWidth);

        progressLinePaint2 = new Paint();
        progressLinePaint2.setColor(Color.DKGRAY);
        progressLinePaint2.setStrokeWidth(strokeWidth);

        progressLinePaint3 = new Paint();
        progressLinePaint3.setColor(Color.GREEN);
        progressLinePaint3.setStrokeWidth(strokeWidth / 2);

    }

    private void initSurface(float widthPixel, float heightPixel) {
        float uiWidth = widthPixel / 360;
        float uiHeight = heightPixel / 360;
        uiVRSurface = new VRSurface(
                uiWidth, uiHeight, 3f,
                new PointF(-uiWidth / 2, -1.55f),
                (int) widthPixel, (int) heightPixel);
    }

    public void glDraw(Eye eye, VideoRenderer.State videoState, HeadControl control, String currentIndex) {
        if (eye.getType() == 1) {
            Canvas canvas = uiVRSurface.getCanvas();

            canvas.drawColor(Color.BLACK);

            drawFileNameOrMessage(canvas, videoState, currentIndex);

            drawDatetime(canvas);

            drawMotions(canvas, control);

            if (videoState.errorMessage != null) {
                drawString(canvas, "Error", beginX, row1Y, leftAlignTextPaint);
                drawString(canvas, videoState.errorMessage, beginX, row2Y, leftAlignTextPaint);
            } else if (videoState.videoLoaded) {
                drawStateIcon(canvas, videoState);
                drawTagAndTime(canvas, videoState);
                drawProgress(canvas, videoState);
            } else {
                drawString(canvas,
                        (videoState.playerState != null) ? videoState.playerState : "Loading",
                        beginX, row1Y, leftAlignTextPaint);
            }

            uiVRSurface.releaseCanvas(canvas);
        }
        uiVRSurface.draw(eye);

    }

    private void drawFileNameOrMessage(Canvas canvas, VideoRenderer.State videoState, String currentIndex) {
        String fullTxt = (videoState.message != null) ?
                videoState.message :
                currentIndex + videoState.fileName;

        drawString(canvas,
                TextUtils.ellipsize(fullTxt, leftAlignTextPaint, (endX - beginX) * 3 / 4, TextUtils.TruncateAt.END).toString(),
                beginX, row0Y, leftAlignTextPaint);
    }

    private SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aa", Locale.US);

    private float drawDatetime(Canvas canvas) {
        String string = sdf.format(Calendar.getInstance().getTime());
        if (((System.currentTimeMillis() / 1000) % 2) == 0) {
            string = string.replace(':', ' ');
        }
        drawString(canvas, string, endX, row0Y, rightAlignTextPaint);
        return rightAlignTextPaint.measureText(string);
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
        drawString(canvas, s, beginX, row1Y, leftAlignTextPaint);
    }

    private void drawString(Canvas canvas, String s, float x, float y, Paint paint) {

        canvas.drawText(s, x,
                y - (paint.descent() + paint.ascent()) / 2,
                paint);
    }

    private void drawMotions(Canvas canvas, HeadControl control) {
        String string = "";
        if (control.notIdle()) {
            string = motionString(control.getMotions());
        }
        if (HeadControl.HeadControlLocked) {
            string = "\uD83D\uDD12" + " " + string;
        }
        drawString(canvas, string, motionsX, row1Y, leftAlignTextPaint);
    }

    private void drawTagAndTime(Canvas canvas, VideoRenderer.State videoState) {
        drawString(canvas, (videoState.force2D ? "2D   " : "") +
                        formatTime(videoState.currentTime) + " / " +
                        formatTime(videoState.videoLength),
                endX, row1Y, rightAlignTextPaint);
    }

    private void drawProgress(Canvas canvas, VideoRenderer.State videoState) {
        float middle = beginX + (endX - beginX) * videoState.currentPosition;

        canvas.drawLine(beginX, row2Y, middle, row2Y, progressLinePaint1);
        canvas.drawLine(middle, row2Y, endX, row2Y, progressLinePaint2);

        if (videoState.seeking) {
            float newMiddle = beginX + (endX - beginX) * videoState.newPosition;
            canvas.drawLine(beginX, row2Y, newMiddle, row2Y, progressLinePaint3);
        }
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