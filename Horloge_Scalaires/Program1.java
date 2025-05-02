import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Program1 {
    private static AtomicInteger scalarClock = new AtomicInteger(0);
    private static final int PORT = 12345;
    private static final String[] PEER_ADDRESSES = {"localhost:12346", "localhost:12347", "localhost:12348"};

    public static void main(String[] args) {
        try {
            Thread.sleep(2500); // Initial delay to ensure all processes are ready
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Start server thread to handle incoming messages
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
            // Local event e1
            addEventWithSleep("e1");
            
            // Local event e2
            addEventWithSleep("e2");
            
            // Send message to P2
            sendMessageWithConfirmation("m1_to_P2", PEER_ADDRESSES[0]);
            
            // Local event e3
            addEventWithSleep("e3");
            
            // Send message to P3
            sendMessageWithConfirmation("m2_to_P3", PEER_ADDRESSES[1]);
            
            // Local event e4
            addEventWithSleep("e4");
            
            // Local event e5
            addEventWithSleep("e5");
            
            // // Send message to P4
            // sendMessageWithConfirmation("m3_to_P4", PEER_ADDRESSES[2]);
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void addEventWithSleep(String eventName) throws InterruptedException {
        // For local event, just increment the clock
        incrementClock();
        MessageTrackerClient.addEvent("P1", eventName, scalarClock.get());
        System.out.println("P1 event: " + eventName + " at clock: " + scalarClock.get());
        Thread.sleep(500);
    }

    private static void sendMessageWithConfirmation(String content, String address) throws InterruptedException {
        // Increment clock before sending
        incrementClock();
        int sendClock = scalarClock.get();
        MessageTrackerClient.addEvent("P1", "send " + content, sendClock);
        System.out.println("P1 sending to " + address + ": " + content + " at clock: " + sendClock);
        
        sendMessage(content, address);
        Thread.sleep(1000); // Wait for message to be processed
    }

    private static void handleClient(Socket socket) {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            Message msg = (Message)in.readObject();
            // Add delay to ensure proper synchronization
            try { Thread.sleep(200); } catch (InterruptedException ie) {}
            System.out.println("P1 received message: " + msg.getContent() + 
                             " from " + msg.getFromProcess() + 
                             " with timestamp: " + msg.getTimestamp() + 
                             " current clock: " + scalarClock.get());
            
            // Update clock according to Lamport's algorithm
            updateClock(msg.getTimestamp());
            
            String eventDesc = "recv " + msg.getContent() + " from " + msg.getFromProcess();
            MessageTrackerClient.addEvent("P1", eventDesc, scalarClock.get());
            MessageTrackerClient.addMessage(msg.getFromProcess(), "P1", msg.getContent(), 
                                         msg.getTimestamp(), scalarClock.get());
            
            System.out.println("P1 processed receive at clock: " + scalarClock.get());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void incrementClock() {
        // For local events and sending messages
        scalarClock.incrementAndGet();
    }

    private static void updateClock(int receivedClock) {
        int newClock = Math.max(scalarClock.get(), receivedClock) + 1;
        System.out.println("Updating clock from " + scalarClock.get() + 
                     " to " + newClock + " (received: " + receivedClock + ")");
                     scalarClock.set(newClock);
}

    private static void sendMessage(String content, String address) {
        String[] parts = address.split(":");
        int retries = 10;
        while (retries > 0) {
            try (Socket socket = new Socket(parts[0], Integer.parseInt(parts[1]));
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                // Include current clock in the message
                out.writeObject(new Message("P1", content, scalarClock.get()));
                System.out.println("P1 successfully sent to " + address + ": " + content + 
                                 " with clock: " + scalarClock.get());
                break;
            } catch (IOException e) {
                retries--;
                System.err.println("P1 send failed to " + address + ", retries left: " + retries);
                try { Thread.sleep(1000); } catch (InterruptedException ie) {}
            }
        }
    }
}