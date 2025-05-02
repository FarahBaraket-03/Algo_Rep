import java.io.*;
import java.net.*;
import java.util.Map;

public class MessageTrackerClient {
    private static final String TRACKER_ADDRESS = "localhost";
    private static final int TRACKER_PORT = 12349;

    public static void addEvent(String processName, String eventName, Map<String, Integer> clock) {
        sendToTracker(new TrackEvent(processName, eventName, clock));
    }

    public static void addMessage(String fromProcess, String toProcess, 
                                String content, Map<String, Integer> sendClock, 
                                Map<String, Integer> receiveClock) {
        // Extraire les valeurs scalaires pour le processus concerné
        int sendValue = sendClock.get(fromProcess);
        int receiveValue = receiveClock.get(toProcess);
        sendToTracker(new TrackMessage(fromProcess, toProcess, content, sendValue, receiveValue));
    }

    private static void sendToTracker(Serializable obj) {
        try (Socket socket = new Socket(TRACKER_ADDRESS, TRACKER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(obj);
        } catch (IOException e) {
            System.err.println("Failed to send to tracker: " + e.getMessage());
        }
    }
}