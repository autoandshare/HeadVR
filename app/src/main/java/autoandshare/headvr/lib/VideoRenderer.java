package autoandshare.headvr.lib;

import android.app.Activity;
import android.graphics.PointF;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.Surface;

import com.google.vr.sdk.base.Eye;

import java.io.IOException;

import autoandshare.headvr.lib.headcontrol.HeadControl;
import autoandshare.headvr.lib.headcontrol.HeadMotion.Motion;
import autoandshare.headvr.lib.rendering.VRTexture2D;

public class VideoRenderer {

    public Boolean pauseOrPlay() {
        if (state.playing) {
            mPlayer.pause();
        } else {
            mPlayer.start();
        }
        state.playing = !state.playing;

        return true;
    }

    private int newPosition(int current, int offset) {
        int pos = (current + offset * 1000) % state.videoLength;
        if (pos < 0) {
            pos += state.videoLength;
        }
        return pos;

    }

    private int newPosition(int offset) {
        return newPosition(mPlayer.getCurrentPosition(), offset);
    }

    public Boolean seek(int offset) {
        mPlayer.seekTo(newPosition(offset));
        return true;
    }

    public Boolean handleSeeking(Motion motion, HeadControl control) {
        int offset = 60;

        if (motion.equals(Motion.ANY)) {
            if (state.seeking) {
                mPlayer.seekTo(state.newPosition);
                state.seeking = false;
                control.waitForIdle();
                return true;
            }
        } else if (motion.equals(Motion.IDLE)) {
            if (state.seeking) {
                state.newPosition = newPosition(state.newPosition,
                        state.forward ? offset : -offset);
                return true;
            }
        } else {
            if (!state.seeking) {
                state.seeking = true;
                state.forward = motion.equals(Motion.RIGHT);
                state.newPosition = newPosition((state.forward ? offset : -offset) / 2);
                return true;
            }
        }

        return false;
    }

    public static class State {
        public boolean playing;
        public boolean seeking;
        public boolean forward;
        public int videoLength;
        public int currentPosition;
        public int newPosition;
        public String errorMessage;
    }

    private State state = new State();
    private MediaPlayer mPlayer;
    private VRTexture2D videoScreen;

    private boolean isSideBySide(String path) {
        return path.matches(".*\\bsbs\\b.*") ||
                path.matches(".*\\bSBS\\b.*");
    }

    private boolean isOverUnder(String path) {
        return path.matches(".*\\bou\\b.*") ||
                path.matches(".*\\bOU\\b.*");
    }

    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public VideoRenderer(Activity activity, Uri uri, float videoSize) {
        if (uri == null) {
            this.state.errorMessage = "No url provided";
            return;
        }
        try {
            videoScreen = new VRTexture2D();

            mPlayer = new MediaPlayer();

            mPlayer.setDataSource(activity.getApplicationContext(), uri);
            mPlayer.setSurface(new Surface(videoScreen.getSurfaceTexture()));
            mPlayer.setLooping(true);
            mPlayer.prepare();
            mPlayer.start();
            state.playing = true;
            state.currentPosition = mPlayer.getCurrentPosition();
            state.videoLength = mPlayer.getDuration();

            // TODO: support side by side, over under
            if (isSideBySide(uri.getPath())) {

                videoScreen.updatePositions(videoSize,
                        videoSize * mPlayer.getVideoHeight() / mPlayer.getVideoWidth(),
                        3.1f,
                        null,
                        new PointF(0, 1), new PointF(0.5f, 0),
                        new PointF(0.5f, 1), new PointF(1, 0)
                );

            } else if (isOverUnder(uri.getPath())) {

                videoScreen.updatePositions(videoSize,
                        videoSize * mPlayer.getVideoHeight() / mPlayer.getVideoWidth(),
                        3.1f,
                        null,
                        new PointF(0, 1), new PointF(1, 0.5f),
                        new PointF(0, 0.5f), new PointF(1, 0)
                );

            } else {

                videoScreen.updatePositions(videoSize,
                        videoSize * mPlayer.getVideoHeight() / mPlayer.getVideoWidth(),
                        3.1f,
                        null);

            }

        } catch (IOException ex) {
            state.errorMessage = "Unable to play the file";
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public void glDraw(Eye eye) {
        if (state.errorMessage != null) {
            return;
        }
        if ((eye.getType() == 1) && state.playing) {
            videoScreen.getSurfaceTexture().updateTexImage();
        }
        videoScreen.draw(eye);
    }

    public State getState() {
        state.currentPosition = mPlayer.getCurrentPosition();
        return state;
    }

    public void pause() {
        if (state.playing) {
            pauseOrPlay();
        }
    }

    public boolean normalPlaying() {
        return state.playing && (!state.seeking);
    }
}