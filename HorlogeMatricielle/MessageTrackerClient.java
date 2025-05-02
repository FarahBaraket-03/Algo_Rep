import java.io.*;
import java.net.*;

public class MessageTrackerClient {
    private static final String TRACKER_HOST = "localhost";
    private static final int TRACKER_PORT = 12349;

    public static void addEvent(String processName, String eventName, int[][] clock) {
        sendToTracker(new TrackEvent(processName, eventName, clock));
    }

    public static void addMessage(String fromProcess, String toProcess, 
                                String content, int[][] sendClock, int[][] receiveClock) {
        sendToTracker(new TrackMessage(fromProcess, toProcess, content, sendClock, receiveClock));
    }

    private static void sendToTracker(Serializable obj) {
        int retries = 3;
        while (retries > 0) {
            try (Socket socket = new Socket(TRACKER_HOST, TRACKER_PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                out.writeObject(obj);
                break;
            } catch (IOException e) {
                retries--;
                System.err.println("Failed to send to tracker, retries left: " + retries);
                try { Thread.sleep(1000); } catch (InterruptedException ie) {}
            }
        }
    }
}