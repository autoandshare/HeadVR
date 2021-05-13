package autoandshare.headvr.lib;

public class Event {
    public Actions action;
    public boolean seekForward;
    public String sender;
    public float offset;

    public Event(String sender) {
        this(sender, Actions.NoAction);
    }
    public Event(String sender, Actions action) {
        this.action = action;
        this.seekForward = false;
        this.sender = sender;
        this.offset = 0;
    }

    @Override
    public String toString() {
        return "Event{" +
                "action=" + action +
                ", seekForward=" + seekForward +
                ", sender='" + sender + '\'' +
                ", offset=" + offset +
                '}';
    }
}
