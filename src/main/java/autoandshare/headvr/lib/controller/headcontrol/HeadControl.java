package autoandshare.headvr.lib.controller.headcontrol;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import autoandshare.headvr.BuildConfig;
import autoandshare.headvr.lib.Actions;
import autoandshare.headvr.lib.Event;
import autoandshare.headvr.lib.VideoRenderer;
import autoandshare.headvr.lib.controller.headcontrol.HeadMotion.Motion;

public class HeadControl {
    private static final String TAG = "HeadControl";

    private static final List<Motion> LockMotion = Arrays.asList(Motion.UP, Motion.RIGHT, Motion.LEFT, Motion.DOWN);

    private static final List<Motion> PlayPause = Arrays.asList(Motion.DOWN, Motion.UP);
    private static final List<Motion> Return = Arrays.asList(Motion.UP, Motion.LEFT, Motion.RIGHT, Motion.DOWN);
    private static final List<Motion> Next = Arrays.asList(Motion.UP, Motion.DOWN, Motion.RIGHT, Motion.LEFT);
    private static final List<Motion> Prev = Arrays.asList(Motion.UP, Motion.DOWN, Motion.LEFT, Motion.RIGHT);
    private static final List<Motion> Round = Arrays.asList(Motion.RIGHT, Motion.DOWN, Motion.LEFT, Motion.UP);
    private static final List<Motion> ReverseRound = Arrays.asList(Motion.LEFT, Motion.DOWN, Motion.RIGHT, Motion.UP);
    private static final List<Motion> Force2D = Arrays.asList(Motion.RIGHT, Motion.LEFT, Motion.RIGHT, Motion.LEFT);
    private static final List<Motion> Recenter = Arrays.asList(Motion.LEFT, Motion.RIGHT, Motion.LEFT, Motion.RIGHT);

    private static final List<Motion> Left = Arrays.asList(Motion.LEFT, Motion.IDLE);
    private static final List<Motion> Right = Arrays.asList(Motion.RIGHT, Motion.IDLE);
    private static final List<Motion> Idle = Collections.singletonList(Motion.IDLE);

    private HeadMotion headMotion = new HeadMotion();

    private ArrayList<Motion> motions = new ArrayList<>();

    private boolean isSeekCancelled() {
        Motion motion = getMotions().get(0);
        return ((VideoRenderer.state.forward && !motion.equals(Motion.LEFT))) ||
                (((!VideoRenderer.state.forward) && !motion.equals(Motion.RIGHT)));
    }

    private static String controllerName = "head";

    private Boolean handleSeeking(Event e) {
        if (!VideoRenderer.state.videoLoaded) {
            return false;
        }

        if (VideoRenderer.state.videoType.isVR() && VideoRenderer.state.playing) {
            return false;
        }

        if (Left.equals(motions) || Right.equals(motions)) {
            e.action = Actions.BeginSeek;
            e.seekForward = Right.equals(motions);
            return true;
        }

        if (VideoRenderer.state.seeking) {
            if (Idle.equals(motions)) {
                e.action = Actions.ContinueSeek;
            } else {
                if (isSeekCancelled()) {
                    e.action = Actions.CancelSeek;
                } else {
                    e.action = Actions.ConfirmSeek;
                }
            }
            return true;
        }

        return false;
    }

    public static boolean HeadControlLocked = false;

    private static HashMap<List<Motion>, Actions> actionMap;

    {
        actionMap = new HashMap<>();
        actionMap.put(LockMotion, Actions.NoAction);
        actionMap.put(PlayPause, Actions.PlayOrPause);
        actionMap.put(Return, Actions.Back);
        actionMap.put(Next, Actions.NextFile);
        actionMap.put(Prev, Actions.PrevFile);
        actionMap.put(Round, Actions.IncreaseEyeDistance);
        actionMap.put(ReverseRound, Actions.DecreaseEyeDistance);
        actionMap.put(Force2D, Actions.Force2D);
        actionMap.put(Recenter, Actions.Recenter);
        actionMap.put(Left, Actions.NoAction);
        actionMap.put(Right, Actions.NoAction);
        actionMap.put(Idle, Actions.NoAction);
    }

    private boolean processMatchingMotions(Event e) {
        if (LockMotion.equals(motions)) {
            HeadControlLocked = !HeadControlLocked;
            return true;
        }

        if (HeadControlLocked) {
            if (!Idle.equals(motions)) {
                e.action = Actions.PartialAction;
            }
            return false;
        }

        if (handleSeeking(e)) {
            return true;
        } else if (actionMap.containsKey(motions)) {
            e.action = actionMap.get(motions);
            return true;
        }

        e.action = Actions.PartialAction;
        return false;
    }

    private boolean isBeginning(List<Motion> a, List<Motion> b) {
        if (a.size() > b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i) != b.get(i)) {
                return false;
            }
        }
        return true;
    }

    private boolean isPartOfAction() {
        for (List<Motion> action : actionMap.keySet()) {
            if (isBeginning(motions, action)) {
                return true;
            }
        }
        return false;
    }

    public List<Motion> getMotions() {
        return motions;
    }

    public Event handleMotion(float[] upVector, float[] forwardVector) {

        Event e = new Event("head");

        Motion motion = headMotion.check(upVector, forwardVector);
        if (motion == null) {
            return e;
        }

        motions.add(motion);

        if ((motion == Motion.IDLE) && (!isPartOfAction())) {
            motions.clear();
            return e;
        }

        if (processMatchingMotions(e)) {
            motions.clear();
        }

        return e;
    }

    public boolean notIdle() {
        return (motions.size() > 0) && (motions.get(0) != Motion.IDLE);
    }
}
