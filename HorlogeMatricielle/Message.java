import java.io.Serializable;

public class Message implements Serializable {
    private final String content;
    private final MatrixClock clock;
    private final String fromProcess;

    public Message(String fromProcess, String content, MatrixClock clock) {
        this.content = content;
        this.clock = clock;
        this.fromProcess = fromProcess;
    }

    public String getContent() {
        return content;
    }

    public MatrixClock getClock() {
        return clock;
    }

    public String getFromProcess() {
        return fromProcess;
    }
}