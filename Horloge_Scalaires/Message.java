import java.io.Serializable;

public class Message implements Serializable {
    private final String content;
    private final int timestamp;
    private final String fromProcess; // Ajouté
    public Message(String fromProcess,String content, int timestamp) {
        this.content = content;
        this.timestamp = timestamp;
        this.fromProcess = fromProcess; // Ajouté
    }

    public String getContent() {
        return content;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public String getFromProcess() {
        return fromProcess;
    }

}