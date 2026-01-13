import java.util.HashSet;// class for storing blocked cells using a hash table implementation
import java.util.Set;//Set interface that HashSet implements

public class Grid {
    private int rows;
    private int cols;
    private RobotAgent[][] cells;//2D array storing RobotAgent objects representing robots in cells
    private Set<String> blockedCells; // New: tracks permanently blocked cells
//tracking permanently blocked cells (e.g., obstacles) ,Uses strings like "2,3" as keys for blocked positions
    //constructor
    public Grid(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.cells = new RobotAgent[rows][cols];
        this.blockedCells = new HashSet<>();
    }
//Cell Availability Check
    public boolean isCellFree(int x, int y) {
        if (x < 0 || x >= rows || y < 0 || y >= cols) {
            return false;//Return false if coordinates are outside grid boundaries
        }
        // Cell is NOT free if: occupied by robot OR marked as blocked
        return cells[x][y] == null && !isCellBlocked(x, y);
        //Cell is free only if No robot present and Cell is not blocked
    }
//Cell Occupation Method:Synchronized: Thread-safe method for multi-agent coordination
    public synchronized boolean occupyCell(int x, int y, RobotAgent robot) {
        if (x < 0 || x >= rows || y < 0 || y >= cols) {
            return false; // Out of bounds
        }

        // Check if cell is blocked
        if (isCellBlocked(x, y)) {
            System.err.println("ERROR: Attempt to occupy BLOCKED cell (" + x + "," + y + ")");
            return false;
        }
// Prevent occupation of blocked cells with error message
        if (cells[x][y] != null) {
            System.err.println("WARNING: Attempt to occupy occupied cell (" + x + "," + y + ")");
            System.err.println("Current occupant: " + cells[x][y].getRobotId());
            System.err.println("New occupant: " + robot.getRobotId());
            return false;
        }

        cells[x][y] = robot;
        return true;
    }

    public synchronized void freeCell(int x, int y) {
        if (x >= 0 && x < rows && y >= 0 && y < cols) {
            cells[x][y] = null;
        }
    }
//Returns robot at given coordinates or null if out of bounds
    public RobotAgent getRobotAt(int x, int y) {
        if (x >= 0 && x < rows && y >= 0 && y < cols) {
            return cells[x][y];
        }
        return null;
    }

    // ============ BLOCKED CELLS MANAGEMENT ============

    public synchronized boolean toggleCellBlocked(int x, int y) {
        if (x < 0 || x >= rows || y < 0 || y >= cols) {
            return false;
        }

        String cellKey = x + "," + y;
        if (blockedCells.contains(cellKey)) {
            // Unblock cell (only if not occupied)
            if (cells[x][y] == null) {
                blockedCells.remove(cellKey);
                return true;
            }
            return false;
        } else {
            // Block cell (only if not occupied)
            if (cells[x][y] == null) {
                blockedCells.add(cellKey);
                return true;
            }
            return false;
        }
    }

    public synchronized boolean setCellBlocked(int x, int y, boolean blocked) {
        if (x < 0 || x >= rows || y < 0 || y >= cols) {
            return false;
        }

        String cellKey = x + "," + y;
        if (blocked) {
            // Block cell (only if not occupied)
            if (cells[x][y] == null) {
                blockedCells.add(cellKey);
                return true;
            }
            return false;
        } else {
            // Unblock cell
            blockedCells.remove(cellKey);
            return true;
        }
    }

    public synchronized void clearAllBlockedCells() {
        blockedCells.clear();
    }

    public boolean isCellBlocked(int x, int y) {
        if (x < 0 || x >= rows || y < 0 || y >= cols) {
            return true; // Out of bounds counts as blocked
        }
        return blockedCells.contains(x + "," + y);
    }

    public Set<String> getBlockedCells() {
        return new HashSet<>(blockedCells);
    }

    public int getBlockedCellCount() {
        return blockedCells.size();
    }

    // ============ GETTERS ============

    public int getRows() { return rows; }
    public int getCols() { return cols; }

    public synchronized void printGridState() {
        System.out.println("=== Current Grid State ===");
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (cells[i][j] != null) {
                    System.out.print(cells[i][j].getRobotId().charAt(5) + " ");
                } else if (isCellBlocked(i, j)) {
                    System.out.print("# "); // # for blocked cell
                } else {
                    System.out.print(". ");
                }
            }
            System.out.println();
        }
        System.out.println("=========================");
        System.out.println("Blocked cells: " + blockedCells.size());
    }
}