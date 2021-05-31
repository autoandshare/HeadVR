package autoandshare.headvr.activity;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;
import com.tencent.mmkv.MMKV;

import org.videolan.medialibrary.interfaces.media.MediaWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;

import autoandshare.headvr.R;
import autoandshare.headvr.lib.Actions;
import autoandshare.headvr.lib.BasicUI;
import autoandshare.headvr.lib.Event;
import autoandshare.headvr.lib.NoDistortionProvider;
import autoandshare.headvr.lib.Setting;
import autoandshare.headvr.lib.State;
import autoandshare.headvr.lib.VideoRenderer;
import autoandshare.headvr.lib.browse.IPlayList;
import autoandshare.headvr.lib.browse.PlayList;
import autoandshare.headvr.lib.controller.KeyControl;
import autoandshare.headvr.lib.controller.TouchControl;
import autoandshare.headvr.lib.controller.headcontrol.HeadControl;
import autoandshare.headvr.lib.rendering.Mesh;
import autoandshare.headvr.lib.rendering.VRTexture2D;

public class VideoActivity extends GvrActivity implements
        GvrView.StereoRenderer {
    public static IPlayList playListS;

    private static final String TAG = "VideoActivity";

    public static float Brightness;

    private State state; // the state shared by all parts

    private BasicUI basicUI;
    private Setting setting;
    private IPlayList playList;
    private HeadControl headControl;
    private KeyControl keyControl;
    private VideoRenderer videoRenderer;

    private long lastEventTime = 0;
    private boolean resetRotationMatrix = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupActionTable();
        setting = new Setting(this);
        if (!setting.getBoolean(Setting.id.EnableDistortionCorrection)) {
            NoDistortionProvider.setupProvider(this);
        }

        state = new State(setting);
        headControl = new HeadControl(state);
        keyControl = new KeyControl(state);

        setContentView(R.layout.video_ui);

        MMKV.initialize(this);

        GvrView cardboardView = findViewById(R.id.cardboard_view);

        cardboardView.setRenderer(this);
        this.setGvrView(cardboardView);

        // use static variable to pass parameter to new activiy
        playList = playListS;
        playListS = null;

        Log.i("intent", "start");
        Uri uri = this.getIntent().getData();
        if (uri == null && playList == null) {
            finish();
            return;
        }

        if (playList == null) {
            playList = new PlayList(uri);
        }
        playList.setActivity(this);
    }


    private Boolean recenter() {
        if (!state.videoLoaded || !state.videoType.isVR()) {
            return false;
        }
        resetRotationMatrix = true;
        return true;
    }

    private void updateSettingWithId(Setting.id id, int i) {
        int newValue = setting.update(id, i);
        state.message = "setting " + id + " to " + newValue;
    }

    private Boolean updateEyeDistance(int i) {
        if (!state.videoLoaded) {
            return false;
        }

        int newVal = state.updateVideoEyeDistance(i);
        state.message = "setting video eye distance " +
                (state.is2DContent() ? "(default) " : "for this video ") +
                "to " + newVal;
        return true;
    }

    private void updateScreenSize(int i) {
        updateSettingWithId(Setting.id.VideoSize, i);
        videoRenderer.updateVideoPositionAndOthers();
    }

    private void updateScreenVertical(int i) {
        updateSettingWithId(Setting.id.VerticalDistance, i);
    }

    private void playMediaFromList(int offset) {
        if (videoRenderer == null) {
            return;
        }

        lastEventTime = System.currentTimeMillis();

        state.message = "Loading play list";

        new Thread(() -> {
            synchronized (playList) {
                MediaWrapper mw = playList.getMediaAtOffset(offset);
                if (mw == null) {
                    state.errorMessage = "Invalid play list";
                } else {
                    state.indexString = playList.getIndexString();
                    videoRenderer.playUri(mw);
                }
            }
        }).start();
        return;
    }

    private Boolean prevFile() {
        playMediaFromList(-1);
        return true;
    }

    private Boolean nextFile() {
        playMediaFromList(1);
        return true;
    }

    private Boolean returnHome() {
        finish();
        return true;
    }

    private void setBrightness() {
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        layout.screenBrightness = Brightness;
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
            videoRenderer.updateVideoPositionAndOthers();
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

        if (videoRenderer != null) {
            videoRenderer.stop();
        }
    }

    @Override
    public void onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown");
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {

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

        try {
            if (isUiVisible()) {
                videoRenderer.fillStateTime();
                basicUI.glDraw(eye);
            }

            videoRenderer.glDraw(eye);
        } catch (Exception ex) {
            Log.e(TAG, "glDraw got exception ", ex);
        }
    }

    @Override
    public void onFinishFrame(Viewport viewport) {
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, "onSurfaceChanged");
    }


    @Override
    public void onSurfaceCreated(EGLConfig config) {
        VRTexture2D.glInit();

        Log.i(TAG, "onSurfaceCreated");
        GLES20.glClearColor(0f, 0f, 0f, 0.5f); // Dark background so text shows up well.

        TouchControl.Width = this.getWindow().getDecorView().getWidth();
        Log.i(TAG, "width " + TouchControl.Width);

        basicUI = new BasicUI(state);

        videoRenderer = new VideoRenderer(this, state);
        playMediaFromList(0); // play first video
    }


    private boolean isUiVisible() {
        if (!state.normalPlaying()) {
            return true;
        }
        return (System.currentTimeMillis() - lastEventTime) < 9000;
    }

    public void showUI() {
        lastEventTime = System.currentTimeMillis();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Log.d(TAG, "got touch event " + ev.toString());
        appendEvent(TouchControl.processEvent(ev));
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d(TAG, "got key event " + event.toString());
        Event e = keyControl.processKeyEvent(event);
        if (e.action != Actions.NoAction) {
            appendEvent(e);
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    List<Event> events;

    public void appendEvent(Event e) {
        if (e == null || e.action == Actions.NoAction) {
            return;
        }

        lastEventTime = System.currentTimeMillis();

        if (e.action == Actions.PartialAction) {
            return;
        }

        synchronized (this) {
            Log.d(TAG, "append event " + e.toString());
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

    public interface Consumer<T> {
        void accept(T t);
    }

    private HashMap<Actions, Consumer<Event>> actionTable;

    private void setupActionTable() {
        actionTable = new HashMap<>();
        actionTable.put(Actions.NoAction, (e) -> {
        });
        actionTable.put(Actions.PartialAction, (e) -> {
        });
        actionTable.put(Actions.PlayOrPause, (e) -> videoRenderer.pauseOrPlay());
        actionTable.put(Actions.NextFile, (e) -> nextFile());
        actionTable.put(Actions.PrevFile, (e) -> prevFile());
        actionTable.put(Actions.BeginSeek, (e) -> videoRenderer.beginSeek(e.seekForward, e.sender));
        actionTable.put(Actions.ContinueSeek, (e) -> videoRenderer.continueSeek(e.sender));
        actionTable.put(Actions.CancelSeek, (e) -> videoRenderer.cancelSeek(e.sender));
        actionTable.put(Actions.ConfirmSeek, (e) -> videoRenderer.confirmSeek(e.sender));
        actionTable.put(Actions.IncreaseEyeDistance, (e) -> updateEyeDistance(1));
        actionTable.put(Actions.DecreaseEyeDistance, (e) -> updateEyeDistance(-1));
        actionTable.put(Actions.Force2D, (e) -> state.toggleForce2D());
        actionTable.put(Actions.Recenter, (e) -> recenter());
        actionTable.put(Actions.Back, (e) -> returnHome());
        actionTable.put(Actions.IncreaseScreenSize, (e) -> updateScreenSize(3));
        actionTable.put(Actions.DecreaseScreenSize, (e) -> updateScreenSize(-3));
        actionTable.put(Actions.MoveScreenUp, (e) -> updateScreenVertical(3));
        actionTable.put(Actions.MoveScreenDown, (e) -> updateScreenVertical(-3));
        actionTable.put(Actions.IncreaseVolume, (e) -> adjustVolume(true));
        actionTable.put(Actions.DecreaseVolume, (e) -> adjustVolume(false));
        actionTable.put(Actions.SingleSeek, (e) -> videoRenderer.singleSeek(e.offset));
        actionTable.put(Actions.StartOptionActivity, (e) -> startOptionWindow());
    }

    private void startOptionWindow() {
        if (!state.videoLoaded) {
            return;
        }

        VideoOptions._state = state;

        startActivity(new Intent(this, VideoOptions.class));
    }

    private AudioManager audioManager;

    private void adjustVolume(boolean increase) {
        if (audioManager == null) {
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                increase ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI);
    }

    private void processEvent(Event e) {
        Log.d(TAG, "process event " + e.toString());
        actionTable.get(e.action).accept(e);
    }
}
