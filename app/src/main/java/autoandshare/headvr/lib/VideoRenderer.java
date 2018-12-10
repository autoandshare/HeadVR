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
import java.util.regex.Matcher;
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
            offset = offset + (max - offset) * stage / 2;
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
        public String fileName;
        public String message;
    }

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

    private VideoType getVideoType(Uri uri) {
        VideoType videoType = new VideoType();

        String[] path = uri.getPath().split("/");
        if (path != null) {
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
        }
        return videoType;
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
        mPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    // video started; hide the placeholder.
                    preparing = false;
                    return true;
                }
                return false;
            }
        });

        playUri(uri);

    }

    private boolean preparing = true;

    public void playUri(Uri uri) {
        preparing = true;

        this.state.fileName = "";

        this.uri = uri;
        mPlayer.reset();

        if (uri == null) {
            this.state.errorMessage = "No url provided";
            return;
        }

        try {
            VideoType videoType = getVideoType(uri);

            mPlayer.setDataSource(activity.getApplicationContext(), uri);
            mPlayer.prepare();
            mPlayer.seekTo(videoProperties.getPosition(uri));
            mPlayer.start();
            state.currentPosition = mPlayer.getCurrentPosition();
            state.videoLength = mPlayer.getDuration();

            float heightWidthRatio = (float) mPlayer.getVideoHeight() / mPlayer.getVideoWidth();

            if (videoType.sbs) {
                // auto detect half and full if not specified
                // 4:3 - 21:9 | (4*2):3 - (21*2):9
                if (videoType.full ||
                        ((!videoType.half) && (!videoType.full)
                                && (heightWidthRatio < (3f / 8 + 9f / 21) / 2))) {
                    heightWidthRatio *= 2;
                }
                videoScreen.updatePositions(videoSize,
                        videoSize * heightWidthRatio,
                        3.1f,
                        null,
                        new PointF(0, 1), new PointF(0.5f, 0),
                        new PointF(0.5f, 1), new PointF(1, 0)
                );

            } else if (videoType.tab) {
                // auto detect half and full if not specified
                // 21:9 - 4:3 | 21:(9*2) - 4:(3*2)
                if (videoType.full ||
                        ((!videoType.half) && (!videoType.full)
                                && (heightWidthRatio > (3f / 4 + 18f / 21) / 2))) {
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
            state.playing = true;

        } catch (IOException ex) {
            state.errorMessage = ex.getMessage();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public void glDraw(Eye eye) {
        if (preparing) {
            return;
        }

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
        return (state.errorMessage == null) &&
                state.playing && (!state.seeking);
    }
}
