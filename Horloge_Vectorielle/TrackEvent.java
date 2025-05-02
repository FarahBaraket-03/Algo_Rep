import java.io.Serializable;
import java.util.Map;

public class TrackEvent implements Serializable {
    String processName;
    String eventName;
    Map<String, Integer> clock;
    
    public TrackEvent(String processName, String eventName, Map<String, Integer> clock) {
        this.processName = processName;
        this.eventName = eventName;
        this.clock = clock;
    }
}