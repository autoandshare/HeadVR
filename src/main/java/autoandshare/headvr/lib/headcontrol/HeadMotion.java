package autoandshare.headvr.lib.headcontrol;

import androidx.collection.CircularArray;
import android.util.Log;

import autoandshare.headvr.lib.Setting;

public class HeadMotion {
    private static final String TAG = "HeadMotion";

    public enum Motion {
        IDLE,
        UP,
        DOWN,
        LEFT,
        RIGHT,
        ANY,
    }

    private int maxSize = 128;
    private CircularArray<float[]> upVectors = new CircularArray<>(maxSize);
    private CircularArray<float[]> forwardVectors = new CircularArray<>(maxSize);

    private long lastActionTime = 0; // millisecond
    private long idleInterval = 1000; // millisecond

    private void appendVector(CircularArray<float[]> vectors, float[] vector) {
        if (vectors.size() >= maxSize) {
            vectors.removeFromStart(1);
        }
        vectors.addLast(vector);
    }

    private void motionDetected(Motion motion) {
        Log.d(TAG, "motion detected: " + motion);
        lastActionTime = System.nanoTime() / 1000000;
        if (motion != Motion.IDLE) {
            upVectors.clear();
            forwardVectors.clear();
        }
    }

    public Motion check(float[] upVector, float[] forwardVector) {
        appendVector(upVectors, upVector);
        appendVector(forwardVectors, forwardVector);

        Motion detectedMotion = null;
        if (upVectors.size() > 0) {
            float[] leftVector = vec3CrossProduct(upVector, forwardVector);

            float[] delta = vec3Sub(forwardVector, forwardVectors.getFirst());

            float upDistance = vec3Dot(delta, upVector);
            float leftDistance = vec3Dot(delta, leftVector);

            if ((Math.abs(upDistance) > (Setting.MotionSensitivity*0.7)) &&
                    (Math.abs(upDistance) > Math.abs(leftDistance))) {
                detectedMotion = (upDistance > 0 ? Motion.UP : Motion.DOWN);
            } else if (Math.abs(leftDistance) > Setting.MotionSensitivity) {
                detectedMotion = (leftDistance > 0 ? Motion.LEFT : Motion.RIGHT);
            } else {
                long curTime = System.nanoTime() / 1000000;
                if ((curTime - lastActionTime) >= idleInterval) {
                    detectedMotion = (Motion.IDLE);
                }
            }
        }

        if (detectedMotion != null) {
            motionDetected(detectedMotion);
        }

        return detectedMotion;
    }

    private void vec3Scale(float[] a, float scale) {
        for (int i = 0; i < 3; i++) {
            a[i] *= scale;
        }
    }

    private float[] vec3CrossProduct(float[] a, float[] b) {
        float[] result = new float[3];
        result[0] = a[1] * b[2] - a[2] * b[1];
        result[1] = a[2] * b[0] - a[0] * b[2];
        result[2] = a[0] * b[1] - a[1] * b[0];
        return result;
    }

    private float vec3Dot(float[] a, float[] b) {
        float sum = 0;
        for (int i = 0; i < 3; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    private float[] vec3Sub(float[] a, float[] b) {
        float[] result = new float[3];
        for (int i = 0; i < 3; i++) {
            result[i] = a[i] - b[i];
        }
        return result;
    }
}