package autoandshare.headvr.lib.controller;

import android.util.Log;
import android.view.KeyEvent;

import java.util.HashSet;

import autoandshare.headvr.lib.Actions;
import autoandshare.headvr.lib.Event;
import autoandshare.headvr.lib.State;

public class KeyControl {

    private final State state;
    private boolean extraFunction = false;

    public KeyControl(State state) {
        this.state = state;
    }

    public Event processKeyEvent(KeyEvent event) {

        Event e = new Event("key");
        e.action = Actions.PartialAction;

        if (extraFunction && state.readyToDraw && !state.playing) {
            extraFunction(event, e);
        } else {
            normalFunctions(event, e);
        }
        return e;
    }

    private void handlePlayPauseKey(KeyEvent event, Event e) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            e.action = Actions.PlayOrPause;
        }
        if (event.getRepeatCount() == keyRepeatCountForOneSecond*3) {
            extraFunction = state.readyToDraw && state.normalPlaying();
            if (extraFunction) {
                state.message = "video option control enabled";
            }
        }
    }

    private void extraFunction(KeyEvent event, Event e) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_BUTTON_START:
                setActionForPressAndLongPress(event, e,
                        Actions.PlayOrPause, Actions.Force2D);
                if (e.action == Actions.PlayOrPause) {
                    extraFunction = false;
                    state.message = null;
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_DPAD_UP:
                setActionForPressAndLongPress(event, e,
                        Actions.MoveScreenUp, Actions.IncreaseScreenSize);
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                setActionForPressAndLongPress(event, e,
                        Actions.MoveScreenDown, Actions.DecreaseScreenSize);
                break;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                setActionForPressAndLongPress(event, e,
                        Actions.IncreaseEyeDistance, Actions.IncreaseVolume);
                break;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_DPAD_LEFT:
                setActionForPressAndLongPress(event, e,
                        Actions.DecreaseEyeDistance, Actions.DecreaseVolume);
                break;
            default:
                e.action = Actions.NoAction;
                break;
        }
    }

    private HashSet<Integer> keyTracking = new HashSet<Integer>();

    private void setActionForPressAndLongPress(
            KeyEvent event, Event e, Actions pressAction, Actions longPressAction) {
        if ((event.getRepeatCount() + 1) % keyRepeatCountForOneSecond == 0) {
            keyTracking.add(event.getKeyCode());
            e.action = longPressAction;
        }
        if (event.getAction() == KeyEvent.ACTION_UP) {
            if (!keyTracking.remove(event.getKeyCode())) {
                e.action = pressAction;
            }
        }
    }

    private static void setIfFirstDown(KeyEvent event, Event e, Actions action) {
        if (event.getAction() == KeyEvent.ACTION_DOWN
                && event.getRepeatCount() == 0) {
            e.action = action;
        }
    }

    private void normalFunctions(KeyEvent event, Event e) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_BUTTON_START:
                handlePlayPauseKey(event, e);
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
                e.action = Actions.NoAction;
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
