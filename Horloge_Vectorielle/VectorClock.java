import java.util.HashMap;
import java.util.Map;

public class VectorClock {
    private final Map<String, Integer> clock;
    
    public VectorClock(String processName) {
        clock = new HashMap<>();
        clock.put(processName, 0);
    }
    
    public VectorClock(Map<String, Integer> initialClock) {
        this.clock = new HashMap<>(initialClock);
    }
    
    public void increment(String processName) {
        clock.put(processName, clock.getOrDefault(processName, 0) + 1);
    }
    
    public void update(VectorClock other) {
        for (Map.Entry<String, Integer> entry : other.clock.entrySet()) {
            String key = entry.getKey();
            clock.put(key, Math.max(clock.getOrDefault(key, 0), entry.getValue()));
        }
    }
    
    public int get(String processName) {
        return clock.getOrDefault(processName, 0);
    }
    
    public Map<String, Integer> getClock() {
        return new HashMap<>(clock);
    }
    
    @Override
    public String toString() {
        return clock.toString();
    }
}