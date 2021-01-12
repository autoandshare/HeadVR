package autoandshare.headvr.lib;

public class Event {
    public Actions action;
    public boolean seekForward;
    public String sender;

    public Event(Actions action, boolean seekForward, String sender) {
        this.action = action;
        this.seekForward = seekForward;
        this.sender = sender;
    }
}
