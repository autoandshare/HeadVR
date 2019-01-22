package autoandshare.headvr.activity;

import android.net.Uri;
import android.opengl.GLES20;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;
import com.tencent.mmkv.MMKV;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;

import autoandshare.headvr.R;
import autoandshare.headvr.lib.BasicUI;
import autoandshare.headvr.lib.Setting;
import autoandshare.headvr.lib.VideoRenderer;
import autoandshare.headvr.lib.browse.PlayList;
import autoandshare.headvr.lib.headcontrol.HeadControl;
import autoandshare.headvr.lib.headcontrol.HeadMotion.Motion;
import autoandshare.headvr.lib.rendering.VRTexture2D;

public class VideoActivity extends GvrActivity implements
        GvrView.StereoRenderer {
    private static final String TAG = "VideoActivity";

    private BasicUI basicUI;

    private Setting setting;

    private Uri uri;
    private GvrView cardboardView;
    private PlayList.IPlayList playList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setting = new Setting(this);

        setContentView(R.layout.video_ui);

        MMKV.initialize(this);

        cardboardView = findViewById(R.id.cardboard_view);
        cardboardView.setRenderer(this);
        this.setGvrView(cardboardView);
        cardboardView.setDistortionCorrectionEnabled(false);

        Log.i("intent", "start");
        uri = this.getIntent().getData();
        if (uri != null) {
            Log.i("intent", uri.toString());
        }

        playList = PlayList.getPlayList(uri);
    }

    private HeadControl headControl = new HeadControl();
    private static final List<Motion> PlayPause = Arrays.asList(Motion.DOWN, Motion.UP);
    private static final List<Motion> Left = Arrays.asList(Motion.LEFT, Motion.IDLE);
    private static final List<Motion> Right = Arrays.asList(Motion.RIGHT, Motion.IDLE);
    private static final List<Motion> Idle = Collections.singletonList(Motion.IDLE);
    private static final List<Motion> Idles = Arrays.asList(Motion.IDLE, Motion.IDLE, Motion.IDLE);
    private static final List<Motion> Any = Collections.singletonList(Motion.ANY);
    private static final List<Motion> Return = Arrays.asList(Motion.UP, Motion.LEFT, Motion.RIGHT, Motion.DOWN);
    private static final List<Motion> Next = Arrays.asList(Motion.UP, Motion.DOWN, Motion.RIGHT, Motion.LEFT);
    private static final List<Motion> Prev = Arrays.asList(Motion.UP, Motion.DOWN, Motion.LEFT, Motion.RIGHT);
    private static final List<Motion> Round = Arrays.asList(Motion.RIGHT, Motion.DOWN, Motion.LEFT, Motion.UP);
    private static final List<Motion> ReverseRound = Arrays.asList(Motion.LEFT, Motion.DOWN, Motion.RIGHT, Motion.UP);
    private static final List<Motion> Force2D = Arrays.asList(Motion.RIGHT, Motion.LEFT, Motion.RIGHT, Motion.LEFT);

    private void setupMotionActionTable() {
        headControl.addMotionAction(Any, () -> {
            updateUIVisibility(Motion.ANY);
            return false;
        });
        headControl.addMotionAction(PlayPause, () -> videoRenderer.pauseOrPlay());
        headControl.addMotionAction(Left, () -> videoRenderer.handleSeeking(Motion.LEFT, headControl));
        headControl.addMotionAction(Right, () -> videoRenderer.handleSeeking(Motion.RIGHT, headControl));
        headControl.addMotionAction(Idle, () -> videoRenderer.handleSeeking(Motion.IDLE, headControl));
        headControl.addMotionAction(Any, () -> videoRenderer.handleSeeking(Motion.ANY, headControl));
        headControl.addMotionAction(Idles, () -> {
            updateUIVisibility(Motion.IDLE);
            return false;
        });
        headControl.addMotionAction(Return, this::returnHome);
        headControl.addMotionAction(Next, this::nextFile);
        headControl.addMotionAction(Prev, this::prevFile);
        headControl.addMotionAction(Round, () -> updateEyeDistance(3));
        headControl.addMotionAction(ReverseRound, () -> updateEyeDistance(-3));
        headControl.addMotionAction(Force2D, () -> videoRenderer.toggleForce2D());
    }

    private Boolean updateEyeDistance(int i) {
        Setting.id id = ((!videoRenderer.is3D()) || videoRenderer.getState().force2D) ?
                Setting.id.EyeDistance : Setting.id.EyeDistance3D;

        updateEyeDistanceWithId(i, id);
        return true;
    }

    private void updateEyeDistanceWithId(int i, Setting.id id) {
        int eyeDistance = setting.get(id) + i;
        if (eyeDistance > setting.getMax(id)) {
            eyeDistance = setting.getMax(id);
        }
        if (eyeDistance < setting.getMin(id)) {
            eyeDistance = setting.getMin(id);
        }

        setting.set(id, eyeDistance);

        videoRenderer.getState().message = "setting " + id + " to " + eyeDistance;
    }

    private Boolean nextFile(int offset) {
        if (playList != null) {
            if (!playList.isReady()) {
                videoRenderer.getState().message = "Loading play list";
            } else {
                Uri uri = playList.next(offset);
                if (uri == null) {
                    videoRenderer.getState().errorMessage = "Invalid play list";
                } else {
                    playUri(uri);
                }
            }
        } else {
            videoRenderer.getState().message = "No play list";
        }
        return true;
    }

    private Boolean prevFile() {
        return nextFile(-1);
    }

    private Boolean nextFile() {
        return nextFile(1);
    }


    private boolean loaded = false;

    private void loadFirstVideo() {
        if (loaded) {
            return;
        }

        if (!PlayList.isListFile(uri.getPath())) {
            loaded = true;
            playUri(this.uri);
        } else {
            if (!playList.isReady()) {
                videoRenderer.getState().message = "Loading play list";
            } else {
                loaded = true;
                Uri current = playList.current();
                if (current != null) {
                    playUri(current);
                } else {
                    videoRenderer.getState().errorMessage = "Invalid play list";
                }
            }
        }
    }

    private void playUri(Uri uri) {
        if (uri != null) {
            videoRenderer.savePosition();
            videoRenderer.playUri(uri);
        }
    }

    private Boolean returnHome() {
        finish();
        return true;
    }

    private void setBrightness() {
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        layout.screenBrightness = Setting.Brightness;
        getWindow().setAttributes(layout);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");
        if ((videoRenderer != null) && (!videoRenderer.hasError())) {
            videoRenderer.pause();
            videoRenderer.savePosition();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");

        setBrightness();
        if (videoRenderer != null) {
            videoRenderer.updateVideoPosition();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart()");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "onStop()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");
    }

    @Override
    public void onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown");
    }


    @Override
    public void onNewFrame(HeadTransform headTransform) {
        loadFirstVideo();

        float[] upVector = new float[3];
        headTransform.getUpVector(upVector, 0);

        float[] forwardVector = new float[3];
        headTransform.getForwardVector(forwardVector, 0);

        headControl.handleMotion(upVector, forwardVector);
    }

    @Override
    public void onDrawEye(Eye eye) {

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        videoRenderer.glDraw(eye);

        if (uiVisible) {
            basicUI.glDraw(eye, videoRenderer.getState(), headControl);
        }

    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, "onSurfaceChanged");
    }

    VideoRenderer videoRenderer;

    @Override
    public void onSurfaceCreated(EGLConfig config) {
        VRTexture2D.glInit();

        Log.i(TAG, "onSurfaceCreated");
        GLES20.glClearColor(0f, 0f, 0f, 0.5f); // Dark background so text shows up well.

        basicUI = new BasicUI();

        videoRenderer = new VideoRenderer(this);

        setupMotionActionTable();
    }

    private boolean uiVisible;

    private void updateUIVisibility(Motion motion) {
        if (videoRenderer.hasError()) {
            uiVisible = true;
            return;
        }
        if (motion == Motion.ANY) {
            if (headControl.notIdle()) {
                uiVisible = true;
            }
        } else if (motion == Motion.IDLE) {
            if (videoRenderer.normalPlaying()) {
                uiVisible = false;
                videoRenderer.getState().message = null;
            }
        }
    }
}