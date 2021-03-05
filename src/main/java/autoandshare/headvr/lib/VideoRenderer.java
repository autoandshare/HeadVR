package autoandshare.headvr.lib;

import android.app.Activity;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.google.vr.sdk.base.Eye;

import org.videolan.libvlc.interfaces.IMedia;
import org.videolan.libvlc.interfaces.IVLCVout;
import org.videolan.libvlc.interfaces.ILibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.medialibrary.interfaces.media.MediaWrapper;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import autoandshare.headvr.activity.VideoActivity;
import autoandshare.headvr.activity.VlcHelper;
import autoandshare.headvr.lib.rendering.Mesh;
import autoandshare.headvr.lib.rendering.MeshExt;
import autoandshare.headvr.lib.rendering.VRTexture2D;

import static java.lang.Math.min;

public class VideoRenderer {
    private static final String TAG = "VideoRenderer";

    public void stop() {
        if (mPlayer != null) {
            mPlayer.stop();
        }
    }

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

    private float newPosition(float current, float offset) {
        float pos = (current + offset);
        while (pos < 0) {
            pos += 1;
        }
        while (pos > 1) {
            pos -= 1;
        }
        return pos;

    }

    private float newPosition(float offset) {
        return newPosition(mPlayer.getPosition(), offset);
    }

    private int seekCount = 0;

    private String seekController;

    public void beginSeek(boolean forward, String seekController) {
        if (state.seeking) {
            return;
        }
        state.seeking = true;
        seekCount = 0;
        state.forward = forward;
        state.newPosition = newPosition(getOffset());
        this.seekController = seekController;
    }

    public void continueSeek(String seekController) {
        if ((!state.seeking) || (!seekController.equals(this.seekController))) {
            return;
        }
        seekCount += 1;
        state.newPosition = newPosition(state.newPosition,
                getOffset());
    }

    public void cancelSeek(String seekController) {
        if (!seekController.equals(this.seekController)) {
            return;
        }
        state.seeking = false;
    }

    public void singleSeek(float offset) {
        if (state.seeking) {
            return;
        }
        restartIfNeeded();
        state.newPosition = newPosition(offset);
        mPlayer.setPosition(state.newPosition);
    }

    public void confirmSeek(String seekController) {
        if (!state.seeking || (!seekController.equals(this.seekController))) {
            return;
        }
        restartIfNeeded();
        mPlayer.setPosition(state.newPosition);
        state.seeking = false;
    }

    private void restartIfNeeded() {
        if (ended) {
            ended = false;
            mPlayer.stop();
            mPlayer.play();
        }
    }

    private float getOffset() {
        if (state.videoLength == 0) {
            return getOffsetWithoutLength();
        }

        int offset = min(state.videoLength / (10 * 1000), 30); // at lease seek (length/10 <-> 30) seconds
        if (offset == 0) {
            offset = 1;
        }

        int max = state.videoLength / (25 * 1000); // at most finish in 25 seeks

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
        return (float) (offset * 1000) / state.videoLength;
    }

    private float getOffsetWithoutLength() {
        float offset = 0;
        switch (seekCount / 2) {
            case 0:
                offset = 0.01f;
                break;
            case 1:
                offset = 0.02f;
                break;
            default:
                offset = 0.04f;
                break;
        }
        if (!state.forward) {
            offset = -offset;
        }
        return offset;
    }

    public Boolean toggleForce2D() {
        if ((this.mw != null) && (state.videoType != null) && !state.videoType.isMono()) {
            state.force2D = !state.force2D;
            videoProperties.setForce2D(propertyKey, state.force2D);
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
        public int currentTime;
        public float currentPosition;
        public float newPosition;

        // optional info
        public String message;
        public String playerState;
    }

    private ILibVLC mILibVLC = null;
    public static State state = new State();
    public MediaPlayer mPlayer;
    private VRTexture2D videoScreen;
    private MeshExt mesh;

    public static boolean useRightTexture(int eyeType) {
        return eyeType == Eye.Type.RIGHT || VideoRenderer.state.force2D;
    }

    public static float getCurrentEyeDistance() {
        return Setting.EyeDistance;
    }

    public String propertyKey;

    private Pattern fileNamePattern3D =
            Pattern.compile("([^A-Za-z0-9]|^)(half|h|full|f|)[^A-Za-z0-9]?(3d)?(sbs|ou|tab)([^A-Za-z0-9]|$)",
                    Pattern.CASE_INSENSITIVE);

    private Pattern fileNamePatternVR =
            Pattern.compile("([^A-Za-z0-9]|^)(180|360)([^A-Za-z0-9]|$)",
                    Pattern.CASE_INSENSITIVE);

    private void getLayoutFromName(String name, VideoType videoType) {
        Matcher matcher = fileNamePattern3D.matcher(name);
        if (matcher.find()) {
            if (matcher.group(2).toLowerCase().startsWith("h") &&
                    (videoType.aspect == VideoType.Aspect.Auto)) {
                videoType.aspect = VideoType.Aspect.Half;
            } else if (matcher.group(2).toLowerCase().startsWith("f") &&
                    (videoType.aspect == VideoType.Aspect.Auto)) {
                videoType.aspect = VideoType.Aspect.Full;
            }
            if (matcher.group(4).toLowerCase().startsWith("s")) {
                videoType.layout = VideoType.Layout.SideBySide;
            } else {
                videoType.layout = VideoType.Layout.TopAndBottom;
            }
        }

    }

    private int getMediaFormatFromLayout(VideoType.Layout layout) {
        switch (layout) {
            case SideBySide:
                return Mesh.MEDIA_STEREO_LEFT_RIGHT;
            case TopAndBottom:
                return Mesh.MEDIA_STEREO_TOP_BOTTOM;
        }
        return Mesh.MEDIA_MONOSCOPIC;
    }

    private void getVRFromName(String name, VideoType videoType) {
        Matcher matcher = fileNamePatternVR.matcher(name);
        if (matcher.find()) {
            videoType.type = matcher.group(2).equals("180") ?
                    VideoType.Type.VR180 : VideoType.Type.VR360;
        }
    }

    private void getVideoType() {
        VideoType videoType = videoProperties.getVideoType(propertyKey);

        String title = mPlayer.getMedia().getMeta(IMedia.Meta.Title);

        if (videoType.layout == VideoType.Layout.Auto) {
            getLayoutFromName(state.fileName, videoType);
            if (videoType.layout == VideoType.Layout.Auto) {
                getLayoutFromName(title, videoType);
                if (videoType.layout == VideoType.Layout.Auto) {
                    videoType.layout = VideoType.Layout.Mono;
                }
            }
        }

        int mediaFormat = getMediaFormatFromLayout(videoType.layout);
        videoScreen.setMediaType(mediaFormat);

        if (videoType.type == VideoType.Type.Auto) {
            getVRFromName(state.fileName, videoType);
            if (videoType.type == VideoType.Type.Auto) {
                getVRFromName(title, videoType);
                if (videoType.type == VideoType.Type.Auto) {
                    videoType.type = VideoType.Type.Plane;
                }
            }
        }

        if (videoType.type != VideoType.Type.Plane) {
            mesh.setMediaType(videoType.type == VideoType.Type.VR180, mediaFormat);
        }

        state.videoType = videoType;
        state.force2D = videoProperties.getForce2D(propertyKey);
        Log.d(TAG, "Video Type: " + videoType);
    }

    private VideoProperties videoProperties;
    private MediaWrapper mw;
    private Activity activity;

    private String[] getLangKeywords(Setting.id id) {
        if (Setting.Instance.getBoolean(id)) {
            return Setting.Instance.getString(id).split(",|，");
        }
        return null;
    }

    private String[] getAudioKeywords() {
        return getLangKeywords(Setting.id.AudioLanguageKeywords);
    }

    private String[] getSubtitleKeyword() {
        return getLangKeywords(Setting.id.SubtitleLanguageKeywords);
    }

    private void setTrack(MediaPlayer.TrackDescription[] tracks, String[] keywords,
                          int pref,
                          VideoActivity.Consumer<Integer> setFunc) {

        if (tracks == null || keywords == null || keywords.length == 0) {
            return;
        }

        if (pref != VideoProperties.TrackAuto) {
            for (MediaPlayer.TrackDescription track : tracks) {
                if (track.id == pref) {
                    setFunc.accept(track.id);
                    return;
                }
            }
        }

        for (String keyword : keywords) {
            keyword = keyword.trim().toLowerCase();
            if (keyword.length() == 0) {
                continue;
            }
            for (MediaPlayer.TrackDescription track : tracks) {
                if (track.name.toLowerCase().contains(keyword)) {
                    setFunc.accept(track.id);
                    return;
                }
            }
        }
    }

    private void setAudioAndSubtitle() {
        setTrack(mPlayer.getAudioTracks(), getAudioKeywords(),
                videoProperties.getVideoAudio(propertyKey),
                i -> mPlayer.setAudioTrack(i));
        setTrack(mPlayer.getSpuTracks(), getSubtitleKeyword(),
                videoProperties.getVideoSubtitle(propertyKey),
                i -> mPlayer.setSpuTrack(i));
    }

    public VideoRenderer(Activity activity) {
        this.activity = activity;
        resetState();
        videoProperties = new VideoProperties();

        videoScreen = new VRTexture2D();
        videoScreen.getSurfaceTexture().setOnFrameAvailableListener((t) -> {
            if (switchingVideo) {
                return;
            }
            framesCount += 1;
            if (framesCount == 1) {
                updatePositionRequested = true;
            }
            state.videoLoaded = true;
        });

        mesh = new MeshExt();
        mesh.glInit(videoScreen.getTextureId());

        mILibVLC = VlcHelper.Instance;
    }

    private boolean switchingVideo = true;
    private boolean ended = false;
    private int framesCount = 0;
    private boolean updatePositionRequested = false;
    private int retry = 0;
    Media.VideoTrack vtrack;

    private ParcelFileDescriptor fd;

    public void playUri(MediaWrapper mw) {

        if (this.mw != null) {
            savePosition();
        }
        switchingVideo = true;
        ended = false;
        framesCount = 0;
        updatePositionRequested = false;
        retry = 0;
        vtrack = null;
        resetState();

        this.mw = mw;
        state.fileName = mw.getTitle();
        propertyKey = PathUtil.getKey(mw.getUri());

        if (mPlayer != null) {
            mPlayer.getVLCVout().detachViews();
            mPlayer.release();
        }

        if (fd != null) {
            try {
                fd.close();
            } catch (Exception e) {
            }
            fd = null;
        }
        videoScreen.getSurfaceTexture().setDefaultBufferSize(1, 1);

        mPlayer = new MediaPlayer(mILibVLC);

        mPlayer.setEventListener(this::onEvent);

        IVLCVout vlcVout = mPlayer.getVLCVout();
        vlcVout.setVideoSurface(videoScreen.getSurfaceTexture());
        vlcVout.attachViews();

        Media m;
        Uri uri = mw.getUri();
        if (uri.getScheme().equals("content")) {
            try {
                fd = activity.getContentResolver()
                        .openFileDescriptor(uri, "r");
                m = new Media(mILibVLC, fd.getFileDescriptor());
            } catch (Exception e) {
                state.errorMessage = e.getMessage();
                return;
            }
        } else {
            m = new Media(mILibVLC, uri);
        }

        // disable subtitle
        m.addOption(MessageFormat.format(":sub-track-id={0}", String.valueOf(Integer.MAX_VALUE)));

        mPlayer.setMedia(m);
        m.release();

        playAndSeek();

        switchingVideo = false;
    }

    private void playAndSeek() {
        mPlayer.play();
        mPlayer.setPosition(videoProperties.getPosition(propertyKey));
        state.playing = true;
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
        state.currentTime = 0;
        state.currentPosition = 0;
        state.newPosition = 0;

        state.message = null;
    }

    public void updateVideoPositionAndOthers() {
        if (framesCount == 0) {
            return;
        }

        getVideoType();
        setAudioAndSubtitle();

        if (vtrack == null) {
            vtrack = mPlayer.getCurrentVideoTrack();
        }
        if (vtrack == null) {
            state.errorMessage = "Unable to get video track info";
            return;
        }

        videoScreen.getSurfaceTexture().setDefaultBufferSize(vtrack.width, vtrack.height);
        mPlayer.getVLCVout().setWindowSize(vtrack.width, vtrack.height);
        mPlayer.setAspectRatio("" + vtrack.width + ":" + vtrack.height);
        mPlayer.setScale(0);

        if (!state.videoType.isVR()) {
            setScreenSize();
        }

        updatePositionRequested = false;
    }

    // guess half and full if not specified
    private boolean isSBSFullByGuess(float heightWidthRatio) {
        return heightWidthRatio < 1f / 3;
    }

    private boolean isTABFullByGuess(float heightWidthRatio) {
        return heightWidthRatio > 3.3f / 4;
    }

    private void setScreenSize() {
        float heightWidthRatio = ((float) vtrack.height) / vtrack.width;

        if (vtrack.sarDen != vtrack.sarNum) {
            heightWidthRatio *= (float) vtrack.sarDen / vtrack.sarNum;
        }


        if (state.videoType.isSBS() && !state.videoType.isHalf()) {
            if (state.videoType.isFull() || isSBSFullByGuess(heightWidthRatio)) {
                heightWidthRatio *= 2;
            }

        } else if (state.videoType.isTAB() && !state.videoType.isHalf()) {
            if (state.videoType.isFull() || isTABFullByGuess(heightWidthRatio)) {
                heightWidthRatio /= 2;
            }
        }

        float width = Setting.VideoSize;
        float height = width * heightWidthRatio;
        float minHeight = Setting.VideoSize * 9.0f / 16.0f;
        if (height < minHeight) {
            width = width * minHeight / height;
            height = minHeight;
        }
        videoScreen.updatePositions(width,
                height,
                3.1f,
                null);

    }

    boolean readyToDraw = false;

    private boolean enoughFrames() {
        return framesCount > 10;
    }

    public void glDraw(Eye eye) {
        updateStates();

        if (eye.getType() == 1) {
            readyToDraw = enoughFrames();
            videoScreen.getSurfaceTexture().updateTexImage();
        }

        if (readyToDraw) {
            if (state.videoType.isVR()) {
                mesh.draw(eye);
            } else {
                videoScreen.draw(eye);
            }
        }
    }

    private void updateStates() {
        if (ended && (mPlayer.getLength() != 0)) {
            restartIfNeeded();
        }
        if (updatePositionRequested) {
            updateVideoPositionAndOthers();
        }
    }

    public State getState() {
        if (state.videoLoaded) {
            state.currentTime = (int) mPlayer.getTime();
            state.currentPosition = mPlayer.getPosition();
            state.videoLength = (int) mPlayer.getLength();
        }
        return state;
    }

    public void pause() {
        mPlayer.pause();
        state.playing = false;
    }

    public void savePosition() {
        if (state.videoLoaded) {
            videoProperties.setPosition(propertyKey, ended ? 0 : mPlayer.getPosition());
        }
    }

    public boolean normalPlaying() {
        return state.videoLoaded &&
                state.playing && (!state.seeking);
    }

    public boolean frameVisibleAndPaused() {
        return state.videoLoaded && readyToDraw &&
                (!state.playing);
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
                    if (retry < 3) {
                        mPlayer.stop();
                        playAndSeek();
                        retry += 1;
                    } else
                        state.playerState = "Failed to open";
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
