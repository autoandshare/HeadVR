package autoandshare.headvr.lib;

import android.app.Activity;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.google.vr.sdk.base.Eye;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import autoandshare.headvr.lib.headcontrol.HeadControl;
import autoandshare.headvr.lib.headcontrol.HeadMotion.Motion;
import autoandshare.headvr.lib.rendering.VRTexture2D;

import static java.lang.Math.min;

public class VideoRenderer {

    public Boolean pauseOrPlay() {
        if (!state.videoLoaded) {
            return false;
        }

        if (state.playing) {
            mPlayer.pause();
        } else {
            if (mPlayer.getPlayerState() == Media.State.Ended) {
                mPlayer.stop();
            }
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
        if (!state.videoLoaded) {
            return false;
        }

        if (!state.playing) {
            return false;
        }

        if (motion.equals(Motion.ANY)) {
            if (state.seeking) {
                if (!cancelSeek(control)) {
                    restartIfNeeded();
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

    private void restartIfNeeded() {
        if (ended) {
            ended = false;
            mPlayer.stop();
            mPlayer.play();
        }
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
        if (!state.videoLoaded) {
            return false;
        }

        if ((this.uri != null) && is3D()) {
            state.force2D = !state.force2D;
            videoProperties.setForce2D(this.uri, state.force2D);
            updateVideoPosition();
            state.message = (state.force2D ? "Enable" : "Disable") + "  Force2D";
        }

        return true;
    }

    public static class State {
        // player ready
        public boolean videoLoaded;

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
        public String playerState;
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
        videoScreen.getSurfaceTexture().setOnFrameAvailableListener((t) -> {
            if (switchingVideo) {
                return;
            }
            state.videoLoaded = true;
        });

        mLibVLC = new LibVLC(activity);
    }

    private int videosPlayedCount = 0;
    private boolean switchingVideo = true;
    private boolean ended = false;
    private boolean positionUpdated = false;

    public void playUri(Uri uri) {

        videosPlayedCount += 1;
        switchingVideo = true;
        ended = false;
        positionUpdated = false;
        resetState();

        this.uri = uri;
        getVideoType(uri);

        if (mPlayer != null) {
            mPlayer.getVLCVout().detachViews();
            mPlayer.release();
        }

        videoScreen.getSurfaceTexture().setDefaultBufferSize(1, 1);

        mPlayer = new MediaPlayer(mLibVLC);

        mPlayer.setEventListener(this::onEvent);

        IVLCVout vlcVout = mPlayer.getVLCVout();
        vlcVout.setVideoSurface(videoScreen.getSurfaceTexture());
        vlcVout.attachViews();

        Media m = new Media(mLibVLC, uri);
        mPlayer.setMedia(m);
        m.release();

        mPlayer.play();

        int pos = videoProperties.getPosition(uri);
        mPlayer.setTime(pos);
        state.playing = true;

        switchingVideo = false;
    }

    private void resetState() {
        state.videoLoaded = false;
        state.errorMessage = null;

        state.fileName = "";
        state.force2D = false;

        state.playing = false;
        state.seeking = false;
        state.forward = false;
        state.videoLength = 0;
        state.currentPosition = 0;
        state.newPosition = 0;

        state.message = null;
    }

    public void updateVideoPosition() {
        if (!state.videoLoaded) {
            return;
        }

        Media.VideoTrack vtrack = mPlayer.getCurrentVideoTrack();
        if (vtrack == null) {
            state.errorMessage = "Unable to get video track info";
            return;
        }

        videoScreen.getSurfaceTexture().setDefaultBufferSize(vtrack.width, vtrack.height);
        mPlayer.getVLCVout().setWindowSize(vtrack.width, vtrack.height);
        mPlayer.setAspectRatio("" + vtrack.width + ":" + vtrack.height);
        mPlayer.setScale(0);

        float heightWidthRatio = ((float) vtrack.height) / vtrack.width;

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

    boolean readyToDraw = false;

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public void glDraw(Eye eye) {
        updateStates();

        if (eye.getType() == 1) {
            readyToDraw = state.videoLoaded && positionUpdated;
            videoScreen.getSurfaceTexture().updateTexImage();
        }

        if (readyToDraw) {
            videoScreen.draw(eye);
        }
    }

    private void updateStates() {
        if (ended && (!state.videoLoaded) && (mPlayer.getLength() != 0)) {
            restartIfNeeded();
        }
        if (state.videoLoaded && (!positionUpdated)) {
            state.videoLength = (int) mPlayer.getLength();
            updateVideoPosition();

            if (videosPlayedCount == 1) {
                pause();
            }

            positionUpdated = true;
            state.videoLoaded = false;
        }
    }

    public State getState() {
        if (state.videoLoaded) {
            state.currentPosition = (int) mPlayer.getTime();
        }
        return state;
    }

    public void pause() {
        if (state.videoLoaded) {
            if (state.playing) {
                pauseOrPlay();
            }
        }
    }

    public void savePosition() {
        if (state.videoLoaded) {
            videoProperties.setPosition(uri, ended ? 0 : (int) mPlayer.getTime());
        }
    }

    public boolean normalPlaying() {
        return state.videoLoaded &&
                state.playing && (!state.seeking);
    }

    public boolean is3D() {
        return state.videoType.sbs || state.videoType.tab;
    }



    public void onEvent(MediaPlayer.Event event) {

        switch (event.type) {
            case MediaPlayer.Event.EndReached:
                state.playerState = "Ended";
                ended = true;
                break;

            case MediaPlayer.Event.Buffering:
                state.playerState = "Buffering " + (int) event.getBuffering() + "%";
                break;

            case MediaPlayer.Event.Playing:
                state.playerState = "Playing";
                break;

            case MediaPlayer.Event.Paused:
                state.playerState = "Paused";
                break;

            case MediaPlayer.Event.Stopped:
                state.playerState = "Stopped";
                if ((!state.videoLoaded) && (mPlayer.getLength() == 0)) {
                    state.playerState = "Not supported";
                }
                break;

            case MediaPlayer.Event.Opening:
                state.playerState = "Opening";
                break;

            case MediaPlayer.Event.PositionChanged:
                break;

            case MediaPlayer.Event.EncounteredError:
                state.playerState = "Encountered error";
                break;

            default:
                break;
        }
    }
}
