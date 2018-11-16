package autoandshare.headvr.lib.rendering;

import android.graphics.Canvas;
import android.graphics.PointF;
import android.view.Surface;

import com.google.vr.sdk.base.Eye;

public class VRSurface {
    private VRTexture2D vrTexture2D;
    private Surface surface;
    private int widthPixel;
    private int heightPixel;

    public VRSurface(float width, float height, float distance,
                     PointF topLeft) {
        vrTexture2D = new VRTexture2D();
        vrTexture2D.updatePositions(
                width, height, distance,
                topLeft);

        widthPixel = (int) (width * 384);
        heightPixel = (int) (height * 384);
        vrTexture2D.getSurfaceTexture().setDefaultBufferSize(
                widthPixel, heightPixel);

        surface = new Surface(vrTexture2D.getSurfaceTexture());
    }

    public Canvas getCanvas() {
        return surface.lockCanvas(null);
    }

    public void releaseCanvas(Canvas canvas) {
        surface.unlockCanvasAndPost(canvas);
        vrTexture2D.getSurfaceTexture().updateTexImage();
    }

    public void draw(Eye eye) {
        vrTexture2D.draw(eye);
    }
}
