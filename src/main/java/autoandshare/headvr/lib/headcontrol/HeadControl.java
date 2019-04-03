package autoandshare.headvr.lib.headcontrol;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import autoandshare.headvr.lib.headcontrol.HeadMotion.Motion;

public class HeadControl {
    private static final String TAG = "HeadControl";

    private HeadMotion headMotion = new HeadMotion();

    private ArrayList<List<Motion>> motionTable = new ArrayList<>();
    private ArrayList<Callable<Boolean>> actionTable = new ArrayList<>();

    private ArrayList<Motion> motions = new ArrayList<>();

    private Motion lastProcessedMotion = null; // prevent overshoot
    private boolean waitForIdle = false;

    public void waitForIdle() {
        waitForIdle = true;
    }

    public boolean getWaitForIdle() {
        return waitForIdle;
    }

    private boolean processMatchingMotions() {
        for (int i = 0; i < motionTable.size(); i++) {
            List<Motion> actionMotion = motionTable.get(i);
            if ((actionMotion.get(0) == Motion.ANY) || (actionMotion.equals(motions))) {
                try {
                    if (actionTable.get(i).call()) {
                        Log.d(TAG, "action called: " + i);
                        setLastMotion();
                        return true;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return false;
    }

    private void setLastMotion() {
        if (motions.size() <= 0) {
            return;
        }
        Motion lastOne = motions.get(motions.size() - 1);
        if (lastOne == Motion.IDLE) {
            return;
        }
        lastProcessedMotion = lastOne;

    }

    private boolean isBeginning(List<Motion> a, List<Motion> b) {
        if (a.size() >= b.size()) {
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
        for (int i = 0; i < motionTable.size(); i++) {
            if (isBeginning(motions, motionTable.get(i))) {
                return true;
            }
        }
        return false;
    }

    public List<Motion> getMotions() {
        return motions;
    }

    public void addMotionAction(List<Motion> motions, Callable<Boolean> callable) {
        motionTable.add(motions);
        actionTable.add(callable);
    }

    public void handleMotion(float[] upVector, float[] forwardVector) {

        Motion motion = headMotion.check(upVector, forwardVector);
        if (motion == null) {
            return;
        }

        if (motion == lastProcessedMotion) {
            return;
        }
        lastProcessedMotion = null;

        if (waitForIdle && (motion != Motion.IDLE)) {
            return;
        }
        waitForIdle = false;

        // skip same motion except IDLE
        if ((motion != Motion.IDLE) &&
                (motions.size() > 0) && (motion == motions.get(motions.size() - 1))) {
            return;
        }

        if ((motions.size() > 0) && (motions.get(0) == Motion.IDLE)
                && (motion != Motion.IDLE)) {
            motions.clear();
        }

        motions.add(motion);

        if (processMatchingMotions()) {
            motions.clear();
        } else {
            if ((motion == Motion.IDLE) && (!isPartOfAction())) {
                motions.clear();
            }
        }
    }

    public boolean notIdle() {
        return (motions.size() > 0) && (motions.get(0) != Motion.IDLE);
    }
}
