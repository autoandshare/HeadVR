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
import autoandshare.headvr.lib.rendering.ContentForTwoEyes;
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
        if (mPlayer == null) {
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
        state.newPosition = newPosition(offset);
        mPlayer.setPosition(state.newPosition);
    }

    public void confirmSeek(String seekController) {
        if (!state.seeking || (!seekController.equals(this.seekController))) {
            return;
        }
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

    private ILibVLC mILibVLC = null;
    private State state;
    public MediaPlayer mPlayer;
    private VRTexture2D videoScreen;
    private MeshExt mesh;

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
                state.getAudio(),
                i -> mPlayer.setAudioTrack(i));
        setTrack(mPlayer.getSpuTracks(), getSubtitleKeyword(),
                state.getSubtitle(),
                i -> mPlayer.setSpuTrack(i));
    }

    public VideoRenderer(Activity activity, State state) {
        this.activity = activity;
        this.state = state;

        videoScreen = new VRTexture2D();
        videoScreen.getSurfaceTexture().setOnFrameAvailableListener((t) -> {
            if (switchingVideo) {
                return;
            }
            framesCount += 1;
            if (framesCount == 1) {
                onVideoLoaded();
            }
        });

        mesh = new MeshExt();
        mesh.glInit(videoScreen.getTextureId());

        mILibVLC = VlcHelper.Instance;
    }

    private void onVideoLoaded() {
        state.title = mPlayer.getMedia().getMeta(IMedia.Meta.Title);
        state.audioTracks = mPlayer.getAudioTracks();
        state.subtitleTracks = mPlayer.getSpuTracks();
        state.loadValues();

        state.videoLoaded = true;
        updatePositionRequested = true;
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
        state.reset();

        this.mw = mw;
        state.fileName = mw.getTitle();
        state.propertyKey = PathUtil.getKey(mw.getUri());

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
        mPlayer.setPosition(state.getPosition());
        state.playing = true;
    }

    public void updateVideoPositionAndOthers() {
        if (framesCount == 0) {
            return;
        }

        state.loadValues();

        videoScreen.setMediaType(state.mediaType);
        mesh.setMediaType(state.videoType.type == VideoType.Type.VR180, state.mediaType);

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

        float width = ContentForTwoEyes.VideoSize;
        float height = width * heightWidthRatio;
        float minHeight = ContentForTwoEyes.VideoSize * 9.0f / 16.0f;
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

    public void fillStateTime() {
        if (state.videoLoaded) {
            state.currentTime = (int) mPlayer.getTime();
            state.currentPosition = mPlayer.getPosition();
            state.videoLength = (int) mPlayer.getLength();
        }
    }

    public void pause() {
        mPlayer.pause();
        state.playing = false;
    }

    public void savePosition() {
        if (state.videoLoaded) {
            state.setPosition(ended ? 0 : mPlayer.getPosition());
        }
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
