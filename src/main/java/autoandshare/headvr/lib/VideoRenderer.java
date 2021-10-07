package autoandshare.headvr.lib;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.google.vr.sdk.base.Eye;

import org.jetbrains.annotations.Nullable;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.ILibVLC;
import org.videolan.libvlc.interfaces.IMedia;
import org.videolan.libvlc.interfaces.IMediaList;
import org.videolan.libvlc.interfaces.IVLCVout;
import org.videolan.medialibrary.interfaces.media.MediaWrapper;
import org.videolan.resources.VLCInstance;
import org.videolan.resources.VLCOptions;
import org.videolan.tools.Settings;

import java.text.MessageFormat;

import autoandshare.headvr.activity.VideoActivity;
import autoandshare.headvr.lib.browse.PlayItem;
import autoandshare.headvr.lib.rendering.ContentForTwoEyes;
import autoandshare.headvr.lib.rendering.MeshExt;
import autoandshare.headvr.lib.rendering.VRTexture2D;

import static java.lang.Math.min;

public class VideoRenderer implements IMedia.EventListener {
    private static final String TAG = "VideoRenderer";

    public void stop() {
        if (mPlayer != null) {
            mPlayer.setEventListener(null);  // prevent autoplay
            mPlayer.stop();
            mPlayer.setEventListener(this::onEvent);
        }
        state.playing = false;
    }

    public Boolean pauseOrPlay() {
        if (mPlayer == null) {
            return false;
        }

        if (state.playing) {
            pause();
        } else {
            play();
        }
        return true;
    }

    private void play() {
        if (ended()) {
            stop();
        }
        mPlayer.play();
        state.playing = true;
    }

    private float newPosition(float current, float offset) {
        float pos = (current + offset);
        if (pos < 0) {
            pos = 0;
        }
        if (pos > 1) {
            pos = 1;
        }
        return pos;

    }

    private float newPosition(float offset) {
        return newPosition(mPlayer.getPosition(), offset);
    }

    private int seekCount = 0;

    private String seekController;

    private boolean seekable() {
        return state.videoLoaded && (!state.seeking) && (!ended());
    }

    public void beginSeek(boolean forward, String seekController) {
        if (!seekable()) {
            return;
        }
        state.seeking = true;
        seekCount = 0;
        state.forward = forward;
        state.newPosition = newPosition(getOffset());
        this.seekController = seekController;
    }

    private boolean seekingStartedByController(String seekController) {
        return state.seeking && seekController.equals(this.seekController);
    }

    public void continueSeek(String seekController) {
        if (!seekingStartedByController(seekController)) {
            return;
        }
        seekCount += 1;
        state.newPosition = newPosition(state.newPosition,
                getOffset());
    }

    public void cancelSeek(String seekController) {
        if (!seekingStartedByController(seekController)) {
            return;
        }
        state.seeking = false;
    }

    public void singleSeek(float offset) {
        if (!seekable()) {
            return;
        }
        state.newPosition = newPosition(offset);
        mPlayer.setPosition(state.newPosition);
    }

    public void confirmSeek(String seekController) {
        if (!state.videoLoaded) {
            return;
        }
        if (!state.seeking || (!seekController.equals(this.seekController))) {
            return;
        }
        mPlayer.setPosition(state.newPosition);
        state.seeking = false;
    }

    private boolean retryIfNeeded() {
        if (!state.videoLoaded && (retry < 1)) {
            playAndSeek();
            retry += 1;
            return true;
        }
        return false;
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
    private VideoActivity activity;

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

    public VideoRenderer(VideoActivity activity, State state) {
        this.activity = activity;
        this.state = state;

        videoScreen = new VRTexture2D();
        videoScreen.getSurfaceTexture().setOnFrameAvailableListener((t) -> {
            synchronized (this) {
                if (switchingVideo) {
                    return;
                }
                framesCount += 1;
                if (framesCount == 1) {
                    onVideoLoaded();
                }
            }
        });

        mesh = new MeshExt();
        mesh.glInit(videoScreen.getTextureId());

        mILibVLC = VLCInstance.INSTANCE.getInstance(this.activity);
    }

    private void onVideoLoaded() {

        Log.d(TAG, "onVideoLoaded");
        IMedia m = mPlayer.getMedia();
        state.title = m.getMeta(IMedia.Meta.Title);
        m.release();

        state.audioTracks = mPlayer.getAudioTracks();
        state.subtitleTracks = mPlayer.getSpuTracks();
        state.loadValues();

        state.videoLoaded = true;
        updatePositionRequested = true;
    }

    private boolean switchingVideo = true;
    private int framesCount = 0;
    private boolean updatePositionRequested = false;
    private int retry = 0;
    Media.VideoTrack vtrack;

    private ParcelFileDescriptor fd;

    public void playUri(PlayItem item) {
        if (this.mw != null) {
            savePosition();
        }

        resetAll();

        this.mw = item.mw;
        state.layoutInfo = item.layoutInfo;
        state.isFileProtocol = PathUtil.isFileAccessProtocol(mw.getUri().getScheme());
        state.fileName = mw.getFileName();
        state.title = mw.getTitle();

        state.propertyKey = PathUtil.getKey(mw.getUri());

        if (mPlayer != null) {
            try {
                mPlayer.setEventListener(null);
                mPlayer.getVLCVout().detachViews();
                mPlayer.release();
            } catch (Exception e) {
            }
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

        IMedia m = getiMedia(mw);
        if (m == null) {
            return;
        }
        playMedia(m);
        m.release();
    }

    private synchronized void resetAll() {
        switchingVideo = true;
        framesCount = 0;
        updatePositionRequested = false;
        retry = 0;
        vtrack = null;
        state.reset();
    }

    @Nullable
    private IMedia getiMedia(MediaWrapper mw) {
        IMedia m;
        Uri uri = mw.getUri();
        if (uri.getScheme().equals("content")) {
            try {
                fd = activity.getContentResolver()
                        .openFileDescriptor(uri, "r");
                m = new Media(mILibVLC, fd.getFileDescriptor());
            } catch (Exception e) {
                state.errorMessage = e.getMessage();
                return null;
            }
        } else {
            m = new Media(mILibVLC, uri);
        }
        return m;
    }

    private void setNetworkCaching(IMedia m) {
        SharedPreferences vlcPrefs = Settings.INSTANCE.getInstance(activity.getApplicationContext());
        int networkCaching = vlcPrefs.getInt("network_caching_value", 0);
        if (networkCaching > 0) {
            m.addOption(":network-caching=" + networkCaching);
        }

    }
    private void playMedia(IMedia m) {
        VLCOptions.INSTANCE.setMediaOptions(m, activity,
                MediaWrapper.MEDIA_VIDEO, false);

        setNetworkCaching(m);

        // disable subtitle
        m.addOption(MessageFormat.format(":sub-track-id={0}", String.valueOf(Integer.MAX_VALUE)));

        m.setEventListener(this);
        mPlayer.setMedia(m);
        playAndSeek();

        switchingVideo = false;
    }

    private void playAndSeek() {
        play();
        mPlayer.setPosition(state.getPosition());
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

    private boolean enoughFrames() {
        return framesCount > 10;
    }

    public void glDraw(Eye eye) {
        updateStates();

        if (eye.getType() == 1) {
            state.readyToDraw = enoughFrames();
            videoScreen.getSurfaceTexture().updateTexImage();
        }

        if (state.readyToDraw) {
            if (state.videoType.isVR()) {
                mesh.draw(eye);
            } else {
                videoScreen.draw(eye);
            }
        }
    }

    private void updateStates() {
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
        if (mPlayer != null) {
            mPlayer.pause();
            Log.d(TAG, "pause called");
        }
        state.playing = false;
    }

    boolean isCloseToEnd() {
        if (mPlayer.getLength() != 0) {
            return (mPlayer.getLength() - mPlayer.getTime()) < 3000;
        }
        else {
            return mPlayer.getPosition() > 0.95;
        }
    }

    public void savePosition() {
        if (state.videoLoaded) {
            Log.d(TAG, "saving position, length " + mPlayer.getLength()
            + " time " + mPlayer.getTime()
            + " position " +         mPlayer.getPosition()
                    + " ended " + ended());
            state.setPosition(isCloseToEnd() && ended() ?
                    0 : mPlayer.getPosition());
        }
    }

    private boolean ended() {
        return mPlayer.getPlayerState() == Media.State.Ended;
    }

    public void onEvent(MediaPlayer.Event event) {
        Log.d(TAG, "got media player event 0x" + Integer.toHexString(event.type)
                + " mplayer state " + mPlayer.getPlayerState()
                + " state playing " + state.playing);

        if (mPlayer.getPlayerState() == IMedia.State.Playing && state.playing == false) {
            pause();
        }

        switch (event.type) {
            case MediaPlayer.Event.EndReached:
                state.playerState = "Ended";
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
                if (retryIfNeeded()) {
                    return;
                }
                state.playerState = "Stopped";
                state.playing = false;
                activity.showUI();
                waitAndTryPlayNext();
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

    private Uri getCurrentMediaUri() {
        IMedia m = mPlayer.getMedia();
        Uri uri = m.getUri();
        m.release();
        return uri;
    }

    private void waitAndTryPlayNext() {
        if (mPlayer.getPlayerState() != Media.State.Ended) {
            Log.d(TAG, "got stop event but player state is " + mPlayer.getPlayerState());
            return;
        }

        Uri savedUri = getCurrentMediaUri();
        Log.d(TAG, " saved Uri: " + savedUri);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mPlayer.getPlayerState() !=  Media.State.Ended) {
                    Log.d(TAG, "try to play next video but player state is " + mPlayer.getPlayerState());
                    return;
                }
                Uri currentUri = getCurrentMediaUri();
                Log.d(TAG, "saved Uri: " + savedUri + " current Uri: " + currentUri);
                if (!savedUri.equals(currentUri)) {
                    Log.d(TAG, "try to play next video but uri has changed");
                    return;
                }

                activity.appendEvent(new Event("player", Actions.NextFile));
            }
        }, 6000);
    }

    @Override
    public void onEvent(IMedia.Event event) {
        Log.d(TAG, "got imedia event " + event.type);
        if (event.type != IMedia.Event.SubItemAdded) {
            return;
        }

        playSubItem();
    }

    private void playSubItem() {
        if (!state.playing) {
            return;
        }

        IMedia m = mPlayer.getMedia();
        if (m != null) {

            IMediaList subItems = m.subItems();
            if (subItems != null && subItems.getCount() > 0) {

                IMedia subItem = subItems.getMediaAt(0);
                if (subItem != null) {
                    Log.d(TAG, "try to play sub item " + subItem.getUri());
                    playMedia(subItem);
                    subItem.release();
                }

                subItems.release();
            }

            m.release();
        }
    }
}
