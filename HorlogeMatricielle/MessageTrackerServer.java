import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.List;
import java.awt.geom.QuadCurve2D;
import java.util.stream.Collectors;

public class MessageTrackerServer {
    private final Map<String, List<Event>> processEvents = new ConcurrentHashMap<>();
    private final Set<TrackMessage> messages = new ConcurrentSkipListSet<>(
        Comparator.comparingInt(m -> m.sendClock[0][0])
    );
    private final JFrame frame = new JFrame("Unified Message Tracker - Matrix Clocks");
    private final DrawingPanel drawingPanel = new DrawingPanel();

    public static void main(String[] args) {
        new MessageTrackerServer().start();
    }

    private void start() {
        initializeGUI();
        startServer();
    }

    private void initializeGUI() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.add(new JScrollPane(drawingPanel), BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private void startServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(12349)) {
                System.out.println("Tracker server running on port 12349");
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> handleClient(clientSocket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleClient(Socket socket) {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            Object input = in.readObject();
            try { Thread.sleep(100); } catch (InterruptedException ie) {}
            
            if (input instanceof TrackEvent) {
                TrackEvent event = (TrackEvent)input;
                System.out.println("Received event: " + event.processName + " - " + event.eventName);
                System.out.println("Matrix clock: " + Arrays.deepToString(event.clock));
                processEvents.computeIfAbsent(event.processName, k -> new ArrayList<>())
                            .add(new Event(event.eventName, event.clock));
            } 
            else if (input instanceof TrackMessage) {
                TrackMessage msg = (TrackMessage)input;
                System.out.println("Received message: " + msg.fromProcess + " -> " + msg.toProcess + 
                                 " [" + msg.content + "]");
                System.out.println("Send clock: " + Arrays.deepToString(msg.sendClock));
                System.out.println("Receive clock: " + Arrays.deepToString(msg.receiveClock));
                messages.add(msg);
            }
            drawingPanel.repaint();
        } catch (Exception e) {
            System.err.println("Error handling client: " + e.getMessage());
        }
    }

    private class DrawingPanel extends JPanel {
        private static final int PROCESS_WIDTH = 200;
        private static final int EVENT_HEIGHT = 60;
        private static final int MARGIN = 80;
        private static final int PROCESS_SPACING = 120;
        private static final int EVENT_SPACING = 30;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Debug: Print all events
            System.out.println("\nCurrent Events:");
            processEvents.forEach((process, events) -> {
                System.out.println(process + ": " + events.stream()
                    .map(e -> e.name)
                    .collect(Collectors.toList()));
            });
            
            List<String> processes = new ArrayList<>(processEvents.keySet());
            Collections.sort(processes);
            
            Map<String, Map<String, Point>> eventCoordinates = new HashMap<>();
            
            // First pass: draw process lines and events
            for (int i = 0; i < processes.size(); i++) {
                String process = processes.get(i);
                int x = MARGIN + i * (PROCESS_WIDTH + PROCESS_SPACING);
                
                // Draw process line
                g2.setColor(Color.BLACK);
                g2.setFont(new Font("Arial", Font.BOLD, 14));
                g2.drawString(process, x, 30);
                g2.drawLine(x, 40, x, getHeight() - 50);
                
                List<Event> events = processEvents.get(process);
                if (events == null) continue;
                
                Map<String, Point> processEventCoords = new HashMap<>();
                
                for (int j = 0; j < events.size(); j++) {
                    int y = 60 + j * (EVENT_HEIGHT + EVENT_SPACING);
                    
                    // Draw event dot
                    g2.setColor(Color.BLUE);
                    g2.fillOval(x - 4, y - 4, 8, 8);
                    
                    // Draw event text
                    g2.setColor(Color.BLACK);
                    g2.setFont(new Font("Arial", Font.PLAIN, 12));
                    String[] parts = events.get(j).name.split(" ", 2);
                    if (parts.length > 1) {
                        g2.drawString(parts[0], x + 15, y + 5);
                        g2.drawString(parts[1], x + 15, y + 20);
                    } else {
                        g2.drawString(events.get(j).name, x + 15, y + 5);
                    }
                    
                    // Draw matrix clock
                    int[][] clock = events.get(j).clock;
                    int yOffset = 35;
                    for (int k = 0; k < clock.length; k++) {
                        String row = Arrays.toString(clock[k]);
                        g2.drawString(row, x + 15, y + yOffset);
                        yOffset += 15;
                    }
                    
                    processEventCoords.put(events.get(j).name, new Point(x, y));
                }
                eventCoordinates.put(process, processEventCoords);
            }
            
            // Second pass: draw message arrows
            g2.setStroke(new BasicStroke(2f));
            for (TrackMessage msg : messages) {
                Map<String, Point> senderEvents = eventCoordinates.get(msg.fromProcess);
                Map<String, Point> receiverEvents = eventCoordinates.get(msg.toProcess);
                
                if (senderEvents == null || receiverEvents == null) {
                    System.out.println("Missing events for " + msg.fromProcess + "->" + msg.toProcess);
                    continue;
                }
                
                // Find matching send event (exact match)
                Point sendPoint = senderEvents.get("send " + msg.content);
                
                // Find matching receive event (contains match)
                Point receivePoint = null;
                for (Map.Entry<String, Point> entry : receiverEvents.entrySet()) {
                    if (entry.getKey().contains("recv " + msg.content)) {
                        receivePoint = entry.getValue();
                        break;
                    }
                }
                
                if (sendPoint != null && receivePoint != null) {
                    // drawMessageArrow(g2, sendPoint, receivePoint, msg.content);
                } else {
                    System.out.println("No arrow for " + msg.content + 
                                     " - send: " + (sendPoint != null) + 
                                     ", recv: " + (receivePoint != null));
                }
            }
            
            // Adjust panel size based on content
            int maxEvents = processEvents.values().stream()
                .mapToInt(List::size).max().orElse(0);
            setPreferredSize(new Dimension(
                MARGIN * 2 + processes.size() * (PROCESS_WIDTH + PROCESS_SPACING) - PROCESS_SPACING,
                100 + maxEvents * (EVENT_HEIGHT + EVENT_SPACING)
            ));
        }
        
        private void drawMessageArrow(Graphics2D g2, Point from, Point to, String label) {
            boolean sameProcess = from.x == to.x;
            Color arrowColor = new Color(10, 70, 10);
            
            if (sameProcess) {
                int loopSize = 50;
                g2.setColor(arrowColor);
                g2.drawArc(from.x - loopSize, from.y, loopSize, 80, 0, 180);
                drawArrowHead(g2, new Point(from.x - loopSize/2, from.y + 40), 
                            new Point(from.x, from.y + 40));
                
                g2.setColor(Color.BLUE);
                // g2.drawString(label, from.x - loopSize + 10, from.y + 30);
            } else {
                int ctrlx = (from.x + to.x) / 2;
                int ctrly = Math.min(from.y, to.y) - 100;
                
                QuadCurve2D curve = new QuadCurve2D.Float();
                curve.setCurve(from.x, from.y, ctrlx, ctrly, to.x, to.y);
                g2.setColor(arrowColor);
                g2.draw(curve);
                // drawArrowHead(g2, new Point(ctrlx, ctrly), to);
            }
        }
        
        private void drawArrowHead(Graphics2D g2, Point from, Point to) {
            double dx = to.x - from.x;
            double dy = to.y - from.y;
            double angle = Math.atan2(dy, dx);
            int len = 15;
            
            Polygon arrowHead = new Polygon();
            arrowHead.addPoint(to.x, to.y);
            arrowHead.addPoint(
                (int)(to.x - len * Math.cos(angle - Math.PI/6)),
                (int)(to.y - len * Math.sin(angle - Math.PI/6))
            );
            arrowHead.addPoint(
                (int)(to.x - len * Math.cos(angle + Math.PI/6)),
                (int)(to.y - len * Math.sin(angle + Math.PI/6))
            );
            
            g2.setColor(new Color(0, 150, 0));
            g2.fill(arrowHead);
        }
    }

    private static class Event {
        final String name;
        final int[][] clock;
        Event(String name, int[][] clock) {
            this.name = name;
            this.clock = clock;
        }
    }
}