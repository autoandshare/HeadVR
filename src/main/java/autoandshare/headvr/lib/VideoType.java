package autoandshare.headvr.lib;

public class VideoType {

    public enum Type {
        Auto,
        Plane,
        VR180,
        VR360,
    }
    public enum Layout {
        Auto,
        Mono,
        SideBySide,
        TopAndBottom,
    }
    public enum Aspect {
        Auto,
        Half,
        Full
    }

    public Type type = Type.Auto;
    public Layout layout = Layout.Auto;
    public Aspect aspect = Aspect.Auto;

    public boolean isSBS() {
        return layout == Layout.SideBySide;
    }
    public boolean isTAB() {
        return layout == Layout.TopAndBottom;
    }
    public boolean isMono() {
        return layout == Layout.Mono;
    }
    public boolean isFull() {
        return aspect == Aspect.Full;
    }
    public boolean isHalf() {
        return aspect == Aspect.Half;
    }
    public boolean isVR() {
        return type != Type.Plane;
    }

    @Override
    public String toString() {
        return "VideoType{" +
                "type=" + type +
                ", layout=" + layout +
                ", aspect=" + aspect +
                '}';
    }
}
