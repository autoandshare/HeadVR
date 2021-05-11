package autoandshare.headvr.lib;

import android.net.Uri;
import android.util.Log;

import com.google.vr.sdk.base.Eye;

import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IMedia;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import autoandshare.headvr.lib.rendering.ContentForTwoEyes;
import autoandshare.headvr.lib.rendering.Mesh;

public class State {
    private static final String TAG = "VideoState";

    // list info
    public int count;
    public int currentIndex;

    // player ready
    public boolean videoLoaded;
    public boolean readyToDraw;

    // error info
    public String errorMessage;

    public boolean isFileProtocol;

    // basic video info
    public String fileName;

    // loaded info
    public String title;
    public MediaPlayer.TrackDescription[] audioTracks;
    public MediaPlayer.TrackDescription[] subtitleTracks;

    // playing and seek info
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

    // motions
    public String motions;

    // Properties
    public String propertyKey;
    public boolean force2D;
    public VideoType videoType;

    //
    public int mediaType;

    public void setForce2D(boolean force2D) {
        this.force2D = force2D;
        VideoProperties.setForce2D(propertyKey, force2D);
        ContentForTwoEyes.force2D = force2D;
    }

    public void loadValues() {
        this.force2D = VideoProperties.getForce2D(propertyKey);
        ContentForTwoEyes.force2D = force2D;

        ContentForTwoEyes.EyeDistance3D = VideoProperties.getVideoEyeDistanceFloat(propertyKey);

        getVideoType();
        mediaType = getMediaFormatFromLayout(videoType.layout);
    }

    public void toggleForce2D() {
        if (this.videoLoaded && (this.videoType != null) && !this.videoType.isMono()) {
            this.force2D = !this.force2D;
            setForce2D(this.force2D);
        }
    }

    public boolean is2DContent() {
        return this.force2D || this.videoType.isMono();
    }

    public int getVideoEyeDistance() {
        if (is2DContent()) {
            return Setting.Instance.get(Setting.id.EyeDistance);
        } else {
            return VideoProperties.getVideoEyeDistance(propertyKey);
        }
    }

    public void setVideoEyeDistance(int newVal) {
        if (force2D || videoType.isMono()) {
            Setting.Instance.set(Setting.id.EyeDistance, newVal);
        } else {
            VideoProperties.setVideoEyeDistance(propertyKey, newVal);
            ContentForTwoEyes.EyeDistance3D = VideoProperties.getVideoEyeDistanceFloat(propertyKey);
        }
    }

    public int updateVideoEyeDistance(int offset) {
        int newVal = offset + VideoProperties.getVideoEyeDistance(propertyKey);
        setVideoEyeDistance(newVal);
        return newVal;
    }

    private Setting setting;

    public State(Setting setting) {
        this.setting = setting;
    }

    public void reset() {
        this.videoLoaded = false;
        this.readyToDraw = false;

        this.errorMessage = null;

        this.fileName = "";

        this.title = "";
        this.audioTracks = null;
        this.subtitleTracks = null;

        this.videoType = null;
        this.force2D = false;

        this.playing = false;
        this.seeking = false;
        this.forward = false;
        this.videoLength = 0;
        this.currentTime = 0;
        this.currentPosition = 0;
        this.newPosition = 0;

        this.message = null;
        this.playerState = null;
    }

    public boolean normalPlaying() {
        return this.videoLoaded &&
                this.playing && (!this.seeking);
    }

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
        videoType = VideoProperties.getVideoType(propertyKey);

        if (videoType.layout == VideoType.Layout.Auto) {
            getLayoutFromName(fileName, videoType);
            if (videoType.layout == VideoType.Layout.Auto) {
                getLayoutFromName(title, videoType);
                if (videoType.layout == VideoType.Layout.Auto) {
                    videoType.layout = VideoType.Layout.Mono;
                }
            }
        }

        if (videoType.type == VideoType.Type.Auto) {
            getVRFromName(fileName, videoType);
            if (videoType.type == VideoType.Type.Auto) {
                getVRFromName(title, videoType);
                if (videoType.type == VideoType.Type.Auto) {
                    videoType.type = VideoType.Type.Plane;
                }
            }
        }

        Log.d(TAG, "Video Type: " + videoType);
    }

    public int getSubtitle() {
        return VideoProperties.getVideoSubtitle(propertyKey);
    }

    public int getAudio() {
        return VideoProperties.getVideoAudio(propertyKey);
    }

    public float getPosition() {
        return VideoProperties.getPosition(propertyKey);
    }

    public void setPosition(float v) {
        VideoProperties.setPosition(propertyKey, v);
    }
}
