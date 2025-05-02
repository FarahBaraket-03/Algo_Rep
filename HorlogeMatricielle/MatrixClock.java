import java.io.Serializable;
import java.util.Arrays;

public class MatrixClock implements Serializable {
    private final int[][] clock;
    private final String processId;
    private final int size;

    public MatrixClock(String processId, int size) {
        this.processId = processId;
        this.size = size;
        this.clock = new int[size][size];
    }

    // Copy constructor
    public MatrixClock(MatrixClock other) {
        this.processId = other.processId;
        this.size = other.size;
        this.clock = new int[size][size];
        for (int i = 0; i < size; i++) {
            System.arraycopy(other.clock[i], 0, this.clock[i], 0, size);
        }
    }

    public void increment(int processIndex) {
        clock[processIndex][processIndex]++;
    }

    public void update(MatrixClock other) {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                clock[i][j] = Math.max(clock[i][j], other.clock[i][j]);
            }
        }
        // Increment our own counter after update
        int index = getProcessIndex();
        if (index >= 0) {
            clock[index][index]++;
        }
    }

    public int[][] getClock() {
        return copyClock();
    }

    public String getProcessId() {
        return processId;
    }

    public int getSize() {
        return size;
    }

    public int getProcessIndex() {
        return Integer.parseInt(processId.substring(1)) - 1;
    }

    private int[][] copyClock() {
        int[][] copy = new int[size][size];
        for (int i = 0; i < size; i++) {
            System.arraycopy(clock[i], 0, copy[i], 0, size);
        }
        return copy;
    }

public int getClockValue(int i, int j) {
    return clock[i][j];
}

public void setClockValue(int i, int j, int value) {
    clock[i][j] = value;
}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MatrixClock[");
        for (int i = 0; i < size; i++) {
            sb.append(Arrays.toString(clock[i]));
            if (i < size - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}