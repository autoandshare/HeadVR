package autoandshare.headvr.lib;

public class Event {
    public Actions action;
    public boolean seekForward;
    public String sender;
    public float offset;

    public Event(String sender) {
        this.action = Actions.NoAction;
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
