package autoandshare.headvr.lib.controller;

import android.util.Log;
import android.view.KeyEvent;

import autoandshare.headvr.lib.Actions;
import autoandshare.headvr.lib.Event;
import autoandshare.headvr.lib.VideoRenderer;

public class KeyControl {
    public static Event processKeyEvent(KeyEvent event) {
        Event e = new Event(Actions.NoAction, false, "key");
        if (VideoRenderer.state.playing) {
            normalFunctions(event, e);
        } else {
            extraFunction(event, e);
        }
        return e;
    }

    private static void extraFunction(KeyEvent event, Event e) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_BUTTON_START:
                setIfFirstDown(event, e, Actions.PlayOrPause);
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_DPAD_UP:
                setIfFirstDown(event, e, Actions.IncreaseScreenSize);
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                setIfFirstDown(event, e, Actions.DecreaseScreenSize);
                break;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                setIfFirstDown(event, e, Actions.IncreaseEyeDistance);
                break;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_DPAD_LEFT:
                setIfFirstDown(event, e, Actions.DecreaseEyeDistance);
                break;
            default:
                break;
        }
    }

    private static void setIfFirstDown(KeyEvent event, Event e, Actions action) {
        if (event.getAction() == KeyEvent.ACTION_DOWN
                && event.getRepeatCount() == 0) {
            e.action = action;
        }
    }

    private static void normalFunctions(KeyEvent event, Event e) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_BUTTON_START:
                setIfFirstDown(event, e, Actions.PlayOrPause);
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_DPAD_UP:
                setIfFirstDown(event, e, Actions.PrevFile);
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                setIfFirstDown(event, e, Actions.NextFile);
                break;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                e.seekForward = true;
                e.action = handleSeekByKey(event);
                break;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_DPAD_LEFT:
                e.seekForward = false;
                e.action = handleSeekByKey(event);
                break;
            default:
                break;
        }
    }


    private static int keyRepeatCountForOneSecond = 20;

    private static Actions handleSeekByKey(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            return Actions.ConfirmSeek;
        }

        int repeatCount = event.getRepeatCount();
        if (repeatCount == 0) {
            return Actions.BeginSeek;
        } else if ((repeatCount % keyRepeatCountForOneSecond) == 0) {
            return Actions.ContinueSeek;
        }

        return Actions.PartialAction;
    }

}
