package autoandshare.headvr.lib.controller;

import android.util.Log;
import android.view.MotionEvent;

import autoandshare.headvr.lib.Actions;
import autoandshare.headvr.lib.Event;

public class TouchControl {
    public static float Width;
    private static float savedX = 0;
    private static float savedY = 0;
    private static long savedT = 0;
    public static Event processEvent(MotionEvent mv) {
        Event e = new Event("motion");
        e.action = Actions.PartialAction;

        switch (mv.getAction()) {
            case MotionEvent.ACTION_DOWN:
                savedX = mv.getX();
                savedY = mv.getY();
                savedT = mv.getEventTime();
                break;
            case MotionEvent.ACTION_UP:
                compareXY(mv, e);
                break;
            default:
                break;
        }

        return e;
    }
    private static void compareXY(MotionEvent mv, Event e) {
        float deltaX = mv.getX() - savedX;
        float deltaY = mv.getY() - savedY;
        if ((deltaX*deltaX + deltaY*deltaY) < 5*5) {
            e.action = mv.getX() < (Width/3) ? Actions.StartOptionActivity : Actions.PlayOrPause;
            return;
        }

        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            // seek
            e.action = Actions.SingleSeek;
            e.offset = deltaX / 10000;
        } else {
            // next/prev file
            e.action = (deltaY > 0) ? Actions.PrevFile : Actions.NextFile;
        }
    }
}
