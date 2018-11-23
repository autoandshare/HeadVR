package autoandshare.headvr.activity;

import android.content.Intent;
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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;

import autoandshare.headvr.R;
import autoandshare.headvr.lib.BasicUI;
import autoandshare.headvr.lib.Setting;
import autoandshare.headvr.lib.VideoRenderer;
import autoandshare.headvr.lib.headcontrol.HeadControl;
import autoandshare.headvr.lib.headcontrol.HeadMotion;
import autoandshare.headvr.lib.headcontrol.HeadMotion.Motion;
import autoandshare.headvr.lib.rendering.VRTexture2D;


public class VideoActivity extends GvrActivity implements
        GvrView.StereoRenderer {
    private static final String TAG = "VideoActivity";

    private BasicUI basicUI;

    private Setting setting;

    Uri uri;
    GvrView cardboardView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.video_ui);
        cardboardView = findViewById(R.id.cardboard_view);
        cardboardView.setRenderer(this);
        this.setGvrView(cardboardView);
        cardboardView.setDistortionCorrectionEnabled(false);

        setting = new Setting(this);

        updateSettings();

        Log.i("intent", "start");
        uri = this.getIntent().getData();
        if (uri != null) {
            Log.i("intent", uri.toString());
        }


    }

    private HeadControl headControl = new HeadControl();
    private static final List<Motion> PlayPause = Arrays.asList(Motion.DOWN, Motion.UP);
    private static final List<Motion> Left = Arrays.asList(Motion.LEFT, Motion.IDLE);
    private static final List<Motion> Right = Arrays.asList(Motion.RIGHT, Motion.IDLE);
    private static final List<Motion> Idle = Collections.singletonList(Motion.IDLE);
    private static final List<Motion> Idles = Arrays.asList(Motion.IDLE, Motion.IDLE, Motion.IDLE);
    private static final List<Motion> Any = Collections.singletonList(Motion.ANY);
    private static final List<Motion> Home = Arrays.asList(Motion.UP, Motion.LEFT, Motion.RIGHT, Motion.DOWN);

    private void setupMotionActionTable() {
        headControl.addMotionAction(Any, () -> {
            checkUIVisibility(Motion.ANY);
            return false;
        });
        headControl.addMotionAction(PlayPause, () -> videoRenderer.pauseOrPlay());
        headControl.addMotionAction(Left, () -> videoRenderer.handleSeeking(Motion.LEFT, headControl));
        headControl.addMotionAction(Right, () -> videoRenderer.handleSeeking(Motion.RIGHT, headControl));
        headControl.addMotionAction(Idle, () -> videoRenderer.handleSeeking(Motion.IDLE, headControl));
        headControl.addMotionAction(Any, () -> videoRenderer.handleSeeking(Motion.ANY, headControl));
        headControl.addMotionAction(Idles, () -> {
            checkUIVisibility(Motion.IDLE);
            return false;
        });
        headControl.addMotionAction(Home, this::returnHome);
    }

    private Boolean returnHome() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
        return true;
    }

    private void setBrightness() {
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        layout.screenBrightness = setting.getFloat(Setting.id.Brightness);
        getWindow().setAttributes(layout);
    }

    private void updateSettings() {
        HeadMotion.setMotionSensitivity(setting.getFloat(Setting.id.MotionSensitivity));

        VRTexture2D.setParameters(
                setting.getFloat(Setting.id.EyeDistance),
                setting.getFloat(Setting.id.EyeDistance3D),
                setting.getFloat(Setting.id.VerticalDistance));

        setBrightness();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");
        videoRenderer.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");
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

        videoRenderer = new VideoRenderer(this, uri, setting.getFloat(Setting.id.VideoSize));

        setupMotionActionTable();
    }

    private boolean uiVisible;

    private void checkUIVisibility(Motion motion) {
        if (motion == Motion.ANY) {
            if (headControl.notIdle()) {
                uiVisible = true;
            }
        } else if (motion == Motion.IDLE) {
            if (videoRenderer.normalPlaying()) {
                uiVisible = false;
            }
        }
    }
}