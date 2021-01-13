package autoandshare.headvr.activity;

import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;
import com.tencent.mmkv.MMKV;

import org.videolan.medialibrary.media.MediaWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import javax.microedition.khronos.egl.EGLConfig;

import autoandshare.headvr.R;
import autoandshare.headvr.lib.Actions;
import autoandshare.headvr.lib.BasicUI;
import autoandshare.headvr.lib.Event;
import autoandshare.headvr.lib.NoDistortionProvider;
import autoandshare.headvr.lib.Setting;
import autoandshare.headvr.lib.VideoRenderer;
import autoandshare.headvr.lib.browse.PlayList;
import autoandshare.headvr.lib.controller.KeyControl;
import autoandshare.headvr.lib.controller.headcontrol.HeadControl;
import autoandshare.headvr.lib.rendering.Mesh;
import autoandshare.headvr.lib.rendering.VRTexture2D;

public class VideoActivity extends GvrActivity implements
        GvrView.StereoRenderer {

    private static final String TAG = "VideoActivity";

    private BasicUI basicUI;

    private Setting setting;

    private GvrView cardboardView;
    private PlayList playList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupActionTable();
        setting = new Setting(this);

        if (setting.getBoolean(Setting.id.DisableDistortionCorrection)) {
            NoDistortionProvider.setupProvider(this);
        }

        setContentView(R.layout.video_ui);

        MMKV.initialize(this);

        cardboardView = findViewById(R.id.cardboard_view);
        cardboardView.setRenderer(this);
        this.setGvrView(cardboardView);

        Log.i("intent", "start");
        Uri uri = this.getIntent().getData();
        if (uri == null) {
            finish();
            return;
        }

        Log.i("intent", uri.toString());
        playList = PlayList.getPlayList(uri, this);
    }

    private HeadControl headControl = new HeadControl();

    private boolean resetRotationMatrix = false;

    private Boolean recenter() {
        if (!videoRenderer.getState().isVR()) {
            return false;
        }

        hideAll = true;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            resetRotationMatrix = true;
            hideAll = false;
        }, 1000);
        return true;
    }

    private Boolean updateEyeDistance(int i) {
        Setting.id id = videoRenderer.getState().drawAs2D() ?
                Setting.id.EyeDistance : Setting.id.EyeDistance3D;

        updateEyeDistanceWithId(i, id);
        return true;
    }

    private void updateEyeDistanceWithId(int i, Setting.id id) {
        int eyeDistance = setting.update(id, i);
        videoRenderer.getState().message = "setting " + id + " to " + eyeDistance;
    }

    private void updateScreenSize(int i) {
        int newScreenSize = setting.update(Setting.id.VideoSize, i);
        videoRenderer.getState().message = "setting " + Setting.id.VideoSize
                + " to " + newScreenSize;
        videoRenderer.updateVideoPosition();
    }

    private Boolean playMediaFromList(int offset) {
        if (!playList.isReady()) {
            videoRenderer.getState().message = "Loading play list";
        } else {
            loaded = true;

            MediaWrapper mw = playList.next(offset);
            if (mw == null) {
                videoRenderer.getState().errorMessage = "Invalid play list";
            } else {
                hideAll = true;
                videoRenderer.playUri(mw);
                new Handler(Looper.getMainLooper()).postDelayed(() -> hideAll = false, 1500);
            }
        }
        return true;
    }

    private boolean hideAll = false;

    private Boolean prevFile() {
        return playMediaFromList(-1);
    }

    private Boolean nextFile() {
        return playMediaFromList(1);
    }


    private boolean loaded = false;

    private void loadFirstVideo() {
        if (loaded) {
            return;
        }

        playMediaFromList(0);

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

        if (videoRenderer != null) {
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

        updateUIVisibility();

        processHeadMotion(headTransform);

        handleEvents();

        if (resetRotationMatrix) {
            if (Mesh.recenterMatrix == null) {
                Mesh.recenterMatrix = new float[16];
            }
            Matrix.transposeM(Mesh.recenterMatrix, 0, headTransform.getHeadView(), 0);
            Mesh.recenterMatrix[3] = Mesh.recenterMatrix[7] = Mesh.recenterMatrix[11] = 0;
            resetRotationMatrix = false;
        }
    }

    private void processHeadMotion(HeadTransform headTransform) {
        if (setting.getBoolean(Setting.id.HeadControl)) {
            float[] upVector = new float[3];
            headTransform.getUpVector(upVector, 0);

            float[] forwardVector = new float[3];
            headTransform.getForwardVector(forwardVector, 0);

            appendEvent(headControl.handleMotion(upVector, forwardVector));
        }
    }

    @Override
    public void onDrawEye(Eye eye) {

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (hideAll) {
            return;
        }

        videoRenderer.glDraw(eye);

        if (uiVisible) {
            basicUI.glDraw(eye, videoRenderer.getState(), headControl,
                    (playList != null) ? playList.currentIndex() : "");
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
    }


    private boolean uiVisible = true;

    private long lastEventTime = 0;

    private void updateUIVisibility() {
        if (!videoRenderer.normalPlaying()) {
            uiVisible = true;
            return;
        }
        uiVisible = (System.currentTimeMillis() - lastEventTime) < 6000;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Event e = KeyControl.processKeyEvent(event);
        if (e.action != Actions.NoAction) {
            appendEvent(e);
            return true;
        }

        return super.dispatchKeyEvent(event);
    }

    List<Event> events;

    void appendEvent(Event e) {
        if (e == null || e.action == Actions.NoAction) {
            return;
        }

        lastEventTime = System.currentTimeMillis();

        if (e.action == Actions.PartialAction) {
            return;
        }

        synchronized (this) {
            if (events == null) {
                events = new ArrayList<>();
            }
            events.add(e);
        }

    }

    void handleEvents() {
        List<Event> eventsCopy;
        synchronized (this) {
            if (events == null) {
                return;
            }
            eventsCopy = events;
            events = null;
        }
        for (Event e : eventsCopy) {
            processEvent(e);
        }
    }

    private HashMap<Actions, Consumer<Event>> actionTable;

    private void setupActionTable() {
        actionTable = new HashMap<>();
        actionTable.put(Actions.PartialAction, (e) -> {});
        actionTable.put(Actions.PlayOrPause, (e) -> videoRenderer.pauseOrPlay());
        actionTable.put(Actions.NextFile, (e) -> nextFile());
        actionTable.put(Actions.PrevFile, (e) -> prevFile());
        actionTable.put(Actions.BeginSeek, (e) -> videoRenderer.beginSeek(e.seekForward, e.sender));
        actionTable.put(Actions.ContinueSeek, (e) -> videoRenderer.continueSeek(e.sender));
        actionTable.put(Actions.CancelSeek, (e) -> videoRenderer.cancelSeek(e.sender));
        actionTable.put(Actions.ConfirmSeek, (e) -> videoRenderer.confirmSeek(e.sender));
        actionTable.put(Actions.IncreaseEyeDistance, (e) -> updateEyeDistance(3));
        actionTable.put(Actions.DecreaseEyeDistance, (e) -> updateEyeDistance(-3));
        actionTable.put(Actions.Force2D, (e) -> videoRenderer.toggleForce2D());
        actionTable.put(Actions.Recenter, (e) -> recenter());
        actionTable.put(Actions.Back, (e) -> returnHome());
        actionTable.put(Actions.IncreaseScreenSize, (e) -> updateScreenSize(3));
        actionTable.put(Actions.DecreaseScreenSize, (e) -> updateScreenSize(-3));
    }

    private void processEvent(Event e) {
        actionTable.get(e.action).accept(e);
    }
}