package autoandshare.headvr.lib;

import android.app.Activity;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.Surface;

import com.google.vr.sdk.base.Eye;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import autoandshare.headvr.lib.headcontrol.HeadControl;
import autoandshare.headvr.lib.headcontrol.HeadMotion.Motion;
import autoandshare.headvr.lib.rendering.VRTexture2D;

import static java.lang.Math.min;

public class VideoRenderer implements IVLCVout.OnNewVideoLayoutListener {


    public Boolean pauseOrPlay() {
        if (hasError()) {
            return true;
        }

        if (state.playing) {
            mPlayer.pause();
        } else {
            mPlayer.play();
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
        return newPosition((int) mPlayer.getTime(), offset);
    }

    private boolean cancelSeek(HeadControl control) {
        Motion motion = control.getMotions().get(0);
        return ((state.forward && !motion.equals(Motion.LEFT))) ||
                (((!state.forward) && !motion.equals(Motion.RIGHT)));
    }

    private int seekCount = 0;

    public Boolean handleSeeking(Motion motion, HeadControl control) {
        if (!state.playing) {
            return false;
        }

        if (motion.equals(Motion.ANY)) {
            if (state.seeking) {
                if (!cancelSeek(control)) {
                    mPlayer.setTime(state.newPosition);
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
        int offset = min(state.videoLength / (10 * 1000), 30); // at lease seek (length/10 <-> 30) seconds
        if (offset == 0) {
            offset = 1;
        }

        int max = state.videoLength / (30 * 1000); // at most finish in 30 seeks

        if (max > offset) {
            int stage = (seekCount / 3);
            if (stage > 2) {
                stage = 2;
            }
            offset = offset + (max - offset) * stage / 2;
        }

        if (!state.forward) {
            offset = -offset;
        }
        return offset;
    }

    public Boolean toggleForce2D() {
        if ((this.uri != null) && (!hasError()) && is3D()) {
            state.force2D = !state.force2D;
            videoProperties.setForce2D(this.uri, state.force2D);
            updateVideoPosition();
            state.message = (state.force2D ? "Enable" : "Disable") + "  Force2D";
        }

        return true;
    }

    @Override
    public void onNewVideoLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        return;
    }

    public static class State {
        // error info
        public String errorMessage;

        // basic info
        public String fileName;
        public VideoType videoType;
        public boolean force2D;

        // playing info
        public boolean playing;
        public boolean seeking;
        public boolean forward;
        public int videoLength;
        public int currentPosition;
        public int newPosition;

        // optional info
        public String message;
    }

    private LibVLC mLibVLC = null;
    private State state = new State();
    private MediaPlayer mPlayer;
    private VRTexture2D videoScreen;

    class VideoType {
        boolean half;
        boolean full;
        boolean sbs;
        boolean tab;
    }

    private Pattern fileNamePattern =
            Pattern.compile("([^A-Za-z0-9]|^)(half|h|full|f|)[^A-Za-z0-9]?(3d)?(sbs|ou|tab)([^A-Za-z0-9]|$)",
                    Pattern.CASE_INSENSITIVE);

    private void getVideoType(Uri uri) {
        VideoType videoType = new VideoType();

        String[] path = uri.getPath().split("/");

        String fileName = path[path.length - 1];
        state.fileName = fileName;

        Matcher matcher = fileNamePattern.matcher(fileName);
        if (matcher.find()) {
            if (matcher.group(2).toLowerCase().startsWith("h")) {
                videoType.half = true;
            } else if (matcher.group(2).toLowerCase().startsWith("f")) {
                videoType.full = true;
            }
            if (matcher.group(4).toLowerCase().startsWith("s")) {
                videoType.sbs = true;
            } else {
                videoType.tab = true;
            }
        }
        state.videoType = videoType;
        state.force2D = videoProperties.getForce2D(uri);

    }

    private VideoProperties videoProperties;
    private Uri uri;
    private Activity activity;

    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public VideoRenderer(Activity activity) {
        this.activity = activity;

        videoProperties = new VideoProperties();

        videoScreen = new VRTexture2D();

        mLibVLC = new LibVLC(activity);
        mPlayer = new MediaPlayer(mLibVLC);

        IVLCVout mVlcVout = mPlayer.getVLCVout();

        mVlcVout.setVideoSurface(videoScreen.getSurfaceTexture());
        mVlcVout.attachViews(this);

        videoScreen.getSurfaceTexture().setOnFrameAvailableListener((t) -> {
            if (loading) {
                return;
            }
            preparing = false;
        });
    }

    private int videosPlayedCount = 0;
    private boolean loading = true;
    private boolean preparing = true;
    private boolean firstFrame = true;

    public void playUri(Uri uri) {

        videosPlayedCount += 1;

        loading = true;
        preparing = true;
        firstFrame = true;

        this.uri = uri;
        //mPlayer.reset();
        resetState();

        getVideoType(uri);

        try {
            Media m = new Media(mLibVLC, uri);
            mPlayer.setMedia(m);
            m.release();

            mPlayer.play();
            int pos = videoProperties.getPosition(uri);
            mPlayer.setTime(pos);
            state.playing = true;

        } catch (Exception ex) {
            state.errorMessage = ex.getMessage();
        }
        loading = false;
    }

    private void resetState() {
        state.errorMessage = null;

        state.fileName = "";
        state.force2D = false;

        state.playing = false;
        state.seeking = false;
        state.forward = false;
        state.videoLength = 1000;
        state.currentPosition = 0;
        state.newPosition = 0;

        state.message = null;
    }

    public void updateVideoPosition() {
        if (hasError()) {
            return;
        }
        Media.VideoTrack vtrack = mPlayer.getCurrentVideoTrack();
        if (vtrack == null) {
            state.errorMessage = "Unable to get video track info";
            return;
        }

        videoScreen.getSurfaceTexture().setDefaultBufferSize(vtrack.width, vtrack.height);
        float heightWidthRatio = ((float)vtrack.height)/vtrack.width;
        state.videoType.sbs = state.videoType.tab = false;

        if (state.videoType.sbs) {
            // auto detect half and full if not specified
            // 4:3 - 21:9 | (4*2):3 - (21*2):9
            if (state.videoType.full ||
                    ((!state.videoType.half)
                            && (heightWidthRatio < (3f / 8 + 9f / 21) / 2))) {
                heightWidthRatio *= 2;
            }
            PointF texture2TopLeft = state.force2D ? null : new PointF(0.5f, 1);
            PointF texture2BottomRight = state.force2D ? null : new PointF(1, 0);
            videoScreen.updatePositions(Setting.VideoSize,
                    Setting.VideoSize * heightWidthRatio,
                    3.1f,
                    null,
                    new PointF(0, 1), new PointF(0.5f, 0),
                    texture2TopLeft, texture2BottomRight
            );

        } else if (state.videoType.tab) {
            // auto detect half and full if not specified
            // 21:9 - 4:3 | 21:(9*2) - 4:(3*2)
            if (state.videoType.full ||
                    ((!state.videoType.half)
                            && (heightWidthRatio > (3f / 4 + 18f / 21) / 2))) {
                heightWidthRatio /= 2;
            }
            PointF texture2TopLeft = state.force2D ? null : new PointF(0, 0.5f);
            PointF texture2BottomRight = state.force2D ? null : new PointF(1, 0);
            videoScreen.updatePositions(Setting.VideoSize,
                    Setting.VideoSize * heightWidthRatio,
                    3.1f,
                    null,
                    new PointF(0, 1), new PointF(1, 0.5f),
                    texture2TopLeft, texture2BottomRight
            );

        } else {

            videoScreen.updatePositions(Setting.VideoSize,
                    Setting.VideoSize * heightWidthRatio,
                    3.1f,
                    null);

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public void glDraw(Eye eye) {
        videoScreen.getSurfaceTexture().updateTexImage();

        if (preparing || hasError()) {
            return;
        }

        if (firstFrame && (eye.getType() != 1)) {
            return;
        }

        if (firstFrame) {
            state.videoLength = (int) mPlayer.getLength();
            updateVideoPosition();
            firstFrame = false;

            if (videosPlayedCount == 1) {
                pause();
            }
        }

        videoScreen.draw(eye);
    }

    private boolean videoLoaded() {
        return (!hasError()) && (!preparing);
    }

    public State getState() {
        if (videoLoaded()) {
            state.currentPosition = (int) mPlayer.getTime();
        }
        return state;
    }

    public void pause() {
        if (state.playing) {
            pauseOrPlay();
        }
    }

    public void savePosition() {
        if (videoLoaded()) {
            videoProperties.setPosition(uri, (int) mPlayer.getTime());
        }
    }

    public boolean normalPlaying() {
        return (state.errorMessage == null) &&
                state.playing && (!state.seeking);
    }

    public boolean hasError() {
        return state.errorMessage != null;
    }

    public boolean is3D() {
        return state.videoType.sbs || state.videoType.tab;
    }
}
