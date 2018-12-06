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
import java.util.regex.Pattern;

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

    private boolean cancelSeek(HeadControl control) {
        Motion motion = control.getMotions().get(0);
        return ((state.forward && !motion.equals(Motion.LEFT))) ||
                (((!state.forward) && !motion.equals(Motion.RIGHT)));
    }

    private int seekCount = 0;

    public Boolean handleSeeking(Motion motion, HeadControl control) {

        if (motion.equals(Motion.ANY)) {
            if (state.seeking) {
                if (!cancelSeek(control)) {
                    mPlayer.seekTo(state.newPosition);
                }
                state.seeking = false;
                control.waitForIdle();
                return true;
            }
        } else if (motion.equals(Motion.IDLE)) {
            if (state.seeking) {
                seekCount += 1;
                state.newPosition = newPosition(state.newPosition,
                        getOffset());
                return true;
            }
        } else {
            if (!state.seeking) {
                state.seeking = true;
                seekCount = 0;
                state.forward = motion.equals(Motion.RIGHT);
                state.newPosition = newPosition(getOffset());
                return true;
            }
        }

        return false;
    }

    private int getOffset() {
        int offset = 30; // at lease seek 30 seconds

        int max = state.videoLength / (30 * 1000); // at most finish in 30 seeks

        if (max > offset) {
            int stage = (seekCount / 3);
            if (stage > 2) {
                stage = 2;
            }
            offset = offset + (max - offset) * stage/2;
        }

        if (!state.forward) {
            offset = -offset;
        }
        return offset;
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

    private Pattern sideBySidePattern =
            Pattern.compile("[^A-Za-z0-9](h?)sbs[^A-Za-z0-9]",
                    Pattern.CASE_INSENSITIVE);

    private Pattern overUnderPattern =
            Pattern.compile("[^A-Za-z0-9](h?)ou[^A-Za-z0-9]",
                    Pattern.CASE_INSENSITIVE);

    private Pattern topAndBottomPattern =
            Pattern.compile("[^A-Za-z0-9](h?)tab[^A-Za-z0-9]",
                    Pattern.CASE_INSENSITIVE);

    private boolean isSideBySide(String path) {
        return sideBySidePattern.matcher(path).find();
    }

    private boolean isOverUnder(String path) {
        return overUnderPattern.matcher(path).find() ||
                topAndBottomPattern.matcher(path).find();
    }

    private VideoProperties videoProperties;
    private Uri uri;
    private Activity activity;
    private float videoSize;

    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public VideoRenderer(Activity activity, Uri uri, float videoSize) {
        this.activity = activity;
        this.videoSize = videoSize;

        videoProperties = new VideoProperties(activity);

        videoScreen = new VRTexture2D();
        mPlayer = new MediaPlayer();
        mPlayer.setSurface(new Surface(videoScreen.getSurfaceTexture()));
        mPlayer.setLooping(false);

        playUri(uri);

    }

    public void playUri(Uri uri) {

        this.uri = uri;
        mPlayer.reset();

        if (uri == null) {
            this.state.errorMessage = "No url provided";
            return;
        }

        try {

            mPlayer.setDataSource(activity.getApplicationContext(), uri);
            mPlayer.prepare();
            mPlayer.seekTo(videoProperties.getPosition(uri));
            mPlayer.start();
            state.playing = true;
            state.currentPosition = mPlayer.getCurrentPosition();
            state.videoLength = mPlayer.getDuration();

            float heightWidthRatio = (float) mPlayer.getVideoHeight() / mPlayer.getVideoWidth();

            if (isSideBySide(uri.getPath())) {
                // auto detect half and full
                // 4:3 - 21:9 | (4*2):3 - (21*2):9
                if (heightWidthRatio < (3f / 8 + 9f / 21) / 2) {
                    heightWidthRatio *= 2;
                }
                videoScreen.updatePositions(videoSize,
                        videoSize * heightWidthRatio,
                        3.1f,
                        null,
                        new PointF(0, 1), new PointF(0.5f, 0),
                        new PointF(0.5f, 1), new PointF(1, 0)
                );

            } else if (isOverUnder(uri.getPath())) {
                // auto detect half and full
                // 21:9 - 4:3 | 21:(9*2) - 4:(3*2)
                if (heightWidthRatio > (3f / 4 + 18f / 21) / 2) {
                    heightWidthRatio /= 2;
                }

                videoScreen.updatePositions(videoSize,
                        videoSize * heightWidthRatio,
                        3.1f,
                        null,
                        new PointF(0, 1), new PointF(1, 0.5f),
                        new PointF(0, 0.5f), new PointF(1, 0)
                );

            } else {

                videoScreen.updatePositions(videoSize,
                        videoSize * heightWidthRatio,
                        3.1f,
                        null);

            }

            state.errorMessage = null;

        } catch (IOException ex) {
            String url = uri.getPath().toString();
            state.errorMessage = "can't play: " + url.substring(url.lastIndexOf('/') + 1, url.length());
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

    public void savePosition() {
        if (state.errorMessage == null) {
            videoProperties.setPosition(uri, mPlayer.getCurrentPosition());
        }
    }

    public boolean normalPlaying() {
        return state.playing && (!state.seeking);
    }
}
