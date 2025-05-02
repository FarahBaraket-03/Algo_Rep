import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Program2 {
    private static AtomicInteger scalarClock = new AtomicInteger(0);
    private static final int PORT = 12346;
    private static final String[] PEER_ADDRESSES = {"localhost:12345", "localhost:12347", "localhost:12348"};

    public static void main(String[] args) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("Program2 waiting on port " + PORT);
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
            // sendMessageWithConfirmation("m1_to_P1", PEER_ADDRESSES[0]);
            addEventWithSleep("e3");
            sendMessageWithConfirmation("m2_to_P3", PEER_ADDRESSES[1]);
            addEventWithSleep("e4");
            addEventWithSleep("e5");
            // sendMessageWithConfirmation("m3_to_P4", PEER_ADDRESSES[2]);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void addEventWithSleep(String eventName) throws InterruptedException {
        incrementClock();
        MessageTrackerClient.addEvent("P2", eventName, scalarClock.get());
        System.out.println("P2 event: " + eventName + " at clock: " + scalarClock.get());
        Thread.sleep(500);
    }

    private static void sendMessageWithConfirmation(String content, String address) throws InterruptedException {
        incrementClock();
        int sendClock = scalarClock.get();
        MessageTrackerClient.addEvent("P2", "send " + content, sendClock);
        System.out.println("P2 sending to " + address + ": " + content + " at clock: " + sendClock);
        sendMessage(content, address);
        Thread.sleep(1000);
    }

    private static void handleClient(Socket socket) {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            Message msg = (Message)in.readObject();

             try { Thread.sleep(200); } catch (InterruptedException ie) {}

            System.out.println("P2 received message: " + msg.getContent() + 
                             " from " + msg.getFromProcess() + 
                             " with timestamp: " + msg.getTimestamp() + 
                             " current clock: " + scalarClock.get());
            
            updateClock(msg.getTimestamp());
            
            String eventDesc = "recv " + msg.getContent() + " from " + msg.getFromProcess();
            MessageTrackerClient.addEvent("P2", eventDesc, scalarClock.get());
            MessageTrackerClient.addMessage(msg.getFromProcess(), "P2", msg.getContent(), 
                                         msg.getTimestamp(), scalarClock.get());
            
            System.out.println("P2 processed receive at clock: " + scalarClock.get());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void incrementClock() {
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
                out.writeObject(new Message("P2", content, scalarClock.get()));
                System.out.println("P2 successfully sent to " + address + ": " + content + 
                                 " with clock: " + scalarClock.get());
                break;
            } catch (IOException e) {
                retries--;
                System.err.println("P2 send failed to " + address + ", retries left: " + retries);
                try { Thread.sleep(1000); } catch (InterruptedException ie) {}
            }
        }
    }
}