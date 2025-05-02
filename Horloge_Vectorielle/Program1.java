import java.io.*;
import java.net.*;
import java.util.*;

public class Program1 {
    private static VectorClock vectorClock;
    private static final int PORT = 12345;
    private static final String[] PEER_ADDRESSES = {"localhost:12346", "localhost:12347", "localhost:12348"};
    private static final String PROCESS_NAME = "P1";

    public static void main(String[] args) {
        vectorClock = new VectorClock(PROCESS_NAME);
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("Program1 waiting on port " + PORT);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> handleClient(clientSocket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        executeLocalInstructions();
    }

    private static void executeLocalInstructions() {
        try {
            addEventWithSleep("e1");
            addEventWithSleep("e2");
            sendUniqueMessage("m1_to_P2", PEER_ADDRESSES[0]);
            addEventWithSleep("e3");
            // sendUniqueMessage("m2_to_P3", PEER_ADDRESSES[1]);
            addEventWithSleep("e4");
            addEventWithSleep("e5");
            // sendUniqueMessage("m3_to_P4", PEER_ADDRESSES[2]);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void addEventWithSleep(String eventName) throws InterruptedException {
        vectorClock.increment(PROCESS_NAME);
        MessageTrackerClient.addEvent(PROCESS_NAME, eventName, vectorClock.getClock());
        System.out.println(PROCESS_NAME + " clock: " + vectorClock);
        Thread.sleep(500);
    }

    private static void sendUniqueMessage(String content, String address) throws InterruptedException {
        vectorClock.increment(PROCESS_NAME);
        MessageTrackerClient.addEvent(PROCESS_NAME, "send " + content, vectorClock.getClock());
        sendMessage(content, address);
        System.out.println(PROCESS_NAME + " clock: " + vectorClock);
        Thread.sleep(500);
    }

    private static void handleClient(Socket socket) {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            Message msg = (Message)in.readObject();
            try { Thread.sleep(200); } catch (InterruptedException ie) {}
            VectorClock receivedClock = new VectorClock(msg.getTimestamp());
            vectorClock.update(receivedClock);
            vectorClock.increment(PROCESS_NAME);
            
            String eventDesc = "recv " + msg.getContent() + " from " + msg.getFromProcess();
            MessageTrackerClient.addEvent(PROCESS_NAME, eventDesc, vectorClock.getClock());
            MessageTrackerClient.addMessage(msg.getFromProcess(), PROCESS_NAME, msg.getContent(), 
                                         msg.getTimestamp(), vectorClock.getClock());
            
            System.out.println(PROCESS_NAME + " clock updated: " + vectorClock);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    
    private static void sendMessage(String content, String address) {
        String[] parts = address.split(":");
        int retries = 10;
        while (retries > 0) {
            try (Socket socket = new Socket(parts[0], Integer.parseInt(parts[1]));
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                // Include current clock in the message
                out.writeObject(new Message("P1", content, vectorClock.getClock()));
                System.out.println("P1 successfully sent to " + address + ": " + content + 
                                 " with clock: " + vectorClock.getClock());
                break;
            } catch (IOException e) {
                retries--;
                System.err.println("P1 send failed to " + address + ", retries left: " + retries);
                try { Thread.sleep(1000); } catch (InterruptedException ie) {}
            }
        }
    }


}