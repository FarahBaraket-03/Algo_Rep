import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicReference;

public class Program1 {
    private static final AtomicReference<MatrixClock> matrixClock = 
        new AtomicReference<>(new MatrixClock("P1", 4));
    private static final int PORT = 12345;
    private static final String[] PEER_ADDRESSES = {"localhost:12346", "localhost:12347", "localhost:12348"};

    public static void main(String[] args) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("Program4 waiting on port " + PORT);
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
            addEventWithSleep("e3");
            sendMessageWithConfirmation("m0_to_P2", PEER_ADDRESSES[0]);
            addEventWithSleep("e4");
            addEventWithSleep("e5");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void addEventWithSleep(String eventName) throws InterruptedException {
        incrementClock();
        MessageTrackerClient.addEvent("P1", eventName, matrixClock.get().getClock());
        System.out.println("P1 event: " + eventName + " at clock: " + matrixClock.get());
        Thread.sleep(500);
    }

    private static void sendMessageWithConfirmation(String content, String address) throws InterruptedException {
        incrementClock();
        int[][] sendClock = matrixClock.get().getClock();
        MessageTrackerClient.addEvent("P1", "send " + content, sendClock);
        System.out.println("P1 sending to " + address + ": " + content + " at clock: " + matrixClock.get());
        sendMessage(content, address);
        Thread.sleep(1000);
    }

    private static void handleClient(Socket socket) {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            Message msg = (Message)in.readObject();
            try { Thread.sleep(200); } catch (InterruptedException ie) {}
            
            System.out.println("P1 received message: " + msg.getContent() + 
                             " from " + msg.getFromProcess() + 
                             " with clock: " + msg.getClock());
            
            updateClock(msg.getClock());
            
            String eventDesc = "recv " + msg.getContent();
            MessageTrackerClient.addEvent("P1", eventDesc, matrixClock.get().getClock());
            MessageTrackerClient.addMessage(msg.getFromProcess(), "P1", msg.getContent(), 
                                         msg.getClock().getClock(), matrixClock.get().getClock());
            
            System.out.println("P1 processed receive at clock: " + matrixClock.get());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void incrementClock() {
        MatrixClock current = matrixClock.get();
        MatrixClock newClock = new MatrixClock(current); // Use the copy constructor
        newClock.increment(newClock.getProcessIndex()); // Use the proper increment method
        matrixClock.set(newClock);
        System.out.println("Incremented clock to: " + matrixClock.get());
}

    private static void updateClock(MatrixClock receivedClock) {
        MatrixClock current = matrixClock.get();
        MatrixClock newClock = new MatrixClock(current); // Use the copy constructor
        newClock.update(receivedClock); // Use the proper update method
        matrixClock.set(newClock);
        System.out.println("Updating clock from " + current + " to " + newClock);
}

    private static void sendMessage(String content, String address) {
        String[] parts = address.split(":");
        int retries = 10;
        while (retries > 0) {
            try (Socket socket = new Socket(parts[0], Integer.parseInt(parts[1]));
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                out.writeObject(new Message(matrixClock.get().getProcessId(), content, matrixClock.get()));
                System.out.println(matrixClock.get().getProcessId() + " successfully sent to " + address + 
                                 ": " + content + " with clock: " + matrixClock.get());
                break;
            } catch (IOException e) {
                retries--;
                System.err.println(matrixClock.get().getProcessId() + " send failed to " + address + 
                                 ", retries left: " + retries);
                try { Thread.sleep(1000); } catch (InterruptedException ie) {}
            }
        }
    }
}