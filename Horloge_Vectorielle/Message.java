import java.io.Serializable;
import java.util.Map;

public class Message implements Serializable {
    private final String content;
    private final Map<String, Integer> timestamp;
    private final String fromProcess;
    
    public Message(String fromProcess, String content, Map<String, Integer> timestamp) {
        this.content = content;
        this.timestamp = timestamp;
        this.fromProcess = fromProcess;
    }
    
    public String getContent() {
        return content;
    }
    
    public Map<String, Integer> getTimestamp() {
        return timestamp;
    }
    
    public String getFromProcess() {
        return fromProcess;
    }
}