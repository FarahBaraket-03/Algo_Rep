import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

public class MessageTrackerServer {
    private final Map<String, List<Event>> processEvents = new HashMap<>();
    private final List<TrackedMessage> messages = new ArrayList<>();
    private final JFrame frame;
    private final DrawingPanel drawingPanel;
    private final Object lock = new Object();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new MessageTrackerServer().start();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                    "Erreur au démarrage: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public MessageTrackerServer() {
        frame = new JFrame("Unified Message Tracker - Vector Clocks");
        drawingPanel = new DrawingPanel();
        frame.setLayout(new BorderLayout());
        frame.add(new JScrollPane(drawingPanel), BorderLayout.CENTER);
    }

    private void start() {
        frame.setSize(1200, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        new Thread(this::startServer).start();
    }

    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(12349)) {
            System.out.println("Tracker server running on port 12349");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket socket) {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            Object input = in.readObject();
            
            synchronized (lock) {
                if (input instanceof TrackEvent) {
                    TrackEvent event = (TrackEvent) input;
                    processEvents.computeIfAbsent(event.processName, k -> new ArrayList<>())
                                .add(new Event(event.eventName, event.clock));
                } else if (input instanceof TrackMessage) {
                    TrackMessage msg = (TrackMessage) input;
                    messages.add(new TrackedMessage(msg.fromProcess, msg.toProcess, msg.content, 
                                                  msg.sendClock, msg.receiveClock));
                }
            }
            
            SwingUtilities.invokeLater(drawingPanel::repaint);
        } catch (Exception e) {
            System.err.println("Error handling client: " + e.getMessage());
        }
    }

    private class DrawingPanel extends JPanel {
        private static final int PROCESS_WIDTH = 250;
        private static final int EVENT_HEIGHT = 60;
        private static final int MARGIN = 50;
        private static final int ARROW_SIZE = 10;
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Get synchronized copies of data
            Map<String, List<Event>> events;
            List<TrackedMessage> msgs;
            synchronized (lock) {
                events = new HashMap<>(processEvents);
                msgs = new ArrayList<>(messages);
            }
            
            List<String> processes = new ArrayList<>(events.keySet());
            Collections.sort(processes);
            
            // Draw process columns
            drawProcessColumns(g2, processes, events);
            
            // Draw messages as arrows
            drawMessageArrows(g2, processes, events, msgs);
            
            // Set panel size based on content
            setPreferredSize(calculatePreferredSize(processes, events));
        }
        
        private void drawProcessColumns(Graphics2D g2, List<String> processes, Map<String, List<Event>> events) {
            g2.setFont(new Font("Monospaced", Font.BOLD, 14));
            
            for (int i = 0; i < processes.size(); i++) {
                String process = processes.get(i);
                int x = MARGIN + i * (PROCESS_WIDTH + 50);
                
                // Draw process name
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(process, x, 30);
                
                // Draw vertical line
                g2.drawLine(x + PROCESS_WIDTH/2, 40, x + PROCESS_WIDTH/2, getHeight() - 50);
                
                // Draw events
                List<Event> eventList = events.get(process);
                if (eventList != null) {
                    for (int j = 0; j < eventList.size(); j++) {
                        int y = 60 + j * EVENT_HEIGHT;
                        
                        // Draw event dot
                        g2.setColor(Color.BLUE);
                        g2.fillOval(x + PROCESS_WIDTH/2 - 4, y - 4, 8, 8);
                        
                        // Draw event text and vector clock
                        g2.setColor(Color.BLACK);
                        String eventText = eventList.get(j).name;
                        g2.drawString(eventText, x + 10, y + 5);
                        
                        // Draw vector clock
                        String vectorClock = formatVectorClock(eventList.get(j).clock, processes);
                        g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
                        g2.drawString(vectorClock, x + 10, y + 25);
                        g2.setFont(new Font("Monospaced", Font.BOLD, 14));
                    }
                }
            }
        }
        
        private String formatVectorClock(Map<String, Integer> clock, List<String> processes) {
            StringBuilder sb = new StringBuilder("[");
            for (String process : processes) {
                sb.append(clock.getOrDefault(process, 0)).append(",");
            }
            sb.setLength(sb.length() - 1); // Remove last comma
            sb.append("]");
            return sb.toString();
        }
        
        private void drawMessageArrows(Graphics2D g2, List<String> processes, 
                                      Map<String, List<Event>> events, List<TrackedMessage> msgs) {
            g2.setStroke(new BasicStroke(1.5f));
            g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
            
            for (TrackedMessage msg : msgs) {
                int fromIdx = processes.indexOf(msg.fromProcess);
                int toIdx = processes.indexOf(msg.toProcess);
                
                if (fromIdx >= 0 && toIdx >= 0) {
                    List<Event> fromEvents = events.get(msg.fromProcess);
                    List<Event> toEvents = events.get(msg.toProcess);
                    
                    int sendIdx = findEventIndex(fromEvents, msg.fromProcess, msg.sendClock);
                    int receiveIdx = findEventIndex(toEvents, msg.toProcess, msg.receiveClock);
                    
                    if (sendIdx >= 0 && receiveIdx >= 0) {
                        int x1 = MARGIN + fromIdx * (PROCESS_WIDTH + 50) + PROCESS_WIDTH/2;
                        int x2 = MARGIN + toIdx * (PROCESS_WIDTH + 50) + PROCESS_WIDTH/2;
                        int y1 = 60 + sendIdx * EVENT_HEIGHT;
                        int y2 = 60 + receiveIdx * EVENT_HEIGHT;
                        
                        // Draw curved arrow
                        drawCurvedArrow(g2, x1, y1, x2, y2, msg.content);
                    }
                }
            }
        }
        
        private int findEventIndex(List<Event> events, String processName, int clockValue) {
            if (events == null) return -1;
            for (int i = 0; i < events.size(); i++) {
                if (events.get(i).clock.getOrDefault(processName, 0) == clockValue) {
                    return i;
                }
            }
            return -1;
        }
        
        private void drawCurvedArrow(Graphics2D g2, int x1, int y1, int x2, int y2, String label) {
            // Calculate control point for the curve
            int ctrlX = (x1 + x2) / 2;
            int ctrlY = (y1 + y2) / 2 - Math.abs(x2 - x1) / 3;
            
            // Draw the curved line
            g2.setColor(new Color(0, 150, 0));
            QuadCurve2D.Float curve = new QuadCurve2D.Float(
                x1, y1, ctrlX, ctrlY, x2, y2);
            g2.draw(curve);
            
            // Draw arrow head
            double angle = Math.atan2(y2 - ctrlY, x2 - ctrlX);
            drawArrowHead(g2, x2, y2, angle);
            
            // Draw label
            g2.setColor(Color.BLACK);
            int labelX = ctrlX - 20;
            int labelY = ctrlY - 5;
            g2.drawString(label, labelX, labelY);
        }
        
        private void drawArrowHead(Graphics2D g2, int x, int y, double angle) {
            g2.setColor(new Color(0, 150, 0));
            
            Polygon arrowHead = new Polygon();
            arrowHead.addPoint(x, y);
            arrowHead.addPoint(
                (int)(x - ARROW_SIZE * Math.cos(angle - Math.PI / 6)),
                (int)(y - ARROW_SIZE * Math.sin(angle - Math.PI / 6)));
            arrowHead.addPoint(
                (int)(x - ARROW_SIZE * Math.cos(angle + Math.PI / 6)),
                (int)(y - ARROW_SIZE * Math.sin(angle + Math.PI / 6)));
            
            g2.fill(arrowHead);
        }
        
        private Dimension calculatePreferredSize(List<String> processes, Map<String, List<Event>> events) {
            int maxEvents = events.values().stream()
                .mapToInt(List::size)
                .max()
                .orElse(10);
            
            int width = MARGIN * 2 + processes.size() * (PROCESS_WIDTH + 50) - 50;
            int height = 80 + maxEvents * EVENT_HEIGHT;
            
            return new Dimension(width, height);
        }
    }

    private static class Event {
        final String name;
        final Map<String, Integer> clock;
        
        Event(String name, Map<String, Integer> clock) {
            this.name = name;
            this.clock = new HashMap<>(clock);
        }
    }

    private static class TrackedMessage {
        final String fromProcess;
        final String toProcess;
        final String content;
        final int sendClock;
        final int receiveClock;
        
        TrackedMessage(String fromProcess, String toProcess, 
                      String content, int sendClock, int receiveClock) {
            this.fromProcess = fromProcess;
            this.toProcess = toProcess;
            this.content = content;
            this.sendClock = sendClock;
            this.receiveClock = receiveClock;
        }
    }
}