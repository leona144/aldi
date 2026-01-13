import javax.swing.*;
import java.awt.*;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.core.Runtime;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class MainFrame extends JFrame {
    private Grid grid;
    private GridPanel gridPanel;
    private JTextArea logArea;
    private enum SetupState { SELECT_ROBOT1_START, SELECT_ROBOT1_TARGET,
        SELECT_ROBOT2_START, SELECT_ROBOT2_TARGET, READY }
    private SetupState currentSetupState = SetupState.SELECT_ROBOT1_START;
    private int robot1StartX = -1, robot1StartY = -1;
    private int robot1TargetX = -1, robot1TargetY = -1;
    private int robot2StartX = -1, robot2StartY = -1;
    private int robot2TargetX = -1, robot2TargetY = -1;
    private String setupInstruction = "Click to select Robot1 Start Position";
    private JButton obstacleButton;
    private JButton clearObstaclesButton;
    private JButton staticRobotButton;
    private JButton clearStaticRobotsButton;
    private boolean staticRobotEditMode = false;
    private boolean eraseStaticMode = false;

    public MainFrame() {
        setTitle("JADE Multi-Robot Coordination - Adaptive Static Robots");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        grid = new Grid(5, 5);
        gridPanel = new GridPanel(grid);
        GridMouseListener mouseListener = new GridMouseListener();
        gridPanel.addMouseListener(mouseListener);
        gridPanel.addMouseMotionListener(mouseListener);
        gridPanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE && (gridPanel.isBlockEditMode() || staticRobotEditMode)) {
                    if (gridPanel.isBlockEditMode()) {
                        exitObstacleEditMode();
                    }
                    if (staticRobotEditMode) {
                        exitStaticRobotEditMode();
                    }
                }
            }
        });
        gridPanel.setFocusable(true);

        add(gridPanel, BorderLayout.CENTER);
        logArea = new JTextArea(15, 70);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(new Color(240, 240, 240));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Simulation Log"));
        add(logScroll, BorderLayout.EAST);
        JPanel controlPanel = new JPanel(new GridLayout(5, 4, 10, 10));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        controlPanel.setBackground(new Color(248, 248, 248));
        JButton setupButton = createStyledButton("Start Setup Mode",
                new Color(76, 175, 80), "Configure robot positions manually");

        JButton resetSetupButton = createStyledButton("Reset Positions",
                new Color(255, 152, 0), "Clear all selected positions");

        JButton autoSetupButton = createStyledButton("Auto Setup",
                new Color(33, 150, 243), "Use default positions");

        JButton loadButton = createStyledButton("Load Preset",
                new Color(156, 39, 176), "Load a predefined scenario");

        // Row 2 - Obstacle buttons
        obstacleButton = createStyledButton("Place Obstacles",
                new Color(255, 87, 34), "Click cells to add/remove obstacles");

        clearObstaclesButton = createStyledButton("Clear All Obstacles",
                new Color(183, 28, 28), "Remove all obstacles from grid");

        JButton mazeButton = createStyledButton("Create Maze",
                new Color(0, 77, 64), "Generate random maze obstacles");

        JButton wallButton = createStyledButton("Add Wall",
                new Color(121, 85, 72), "Add a wall of obstacles");

        // Row 3 - Static Robot buttons
        staticRobotButton = createStyledButton("Add Static Robots",
                new Color(128, 0, 128), "Click cells to add/remove static robots");

        clearStaticRobotsButton = createStyledButton("Clear Static Robots",
                new Color(183, 28, 183), "Remove all static robots");

        JButton adaptiveButton = createStyledButton("Adaptive Static Robots",
                new Color(128, 0, 128), "Test adaptive static robots scenario");

        JButton testButton = createStyledButton("Test Collision",
                new Color(255, 193, 7), "Test collision scenario");

        // Row 4 - Simulation buttons
        JButton startButton = createStyledButton("Start Simulation",
                new Color(0, 150, 136), "Start the simulation with current setup");

        JButton stopButton = createStyledButton("Stop Simulation",
                new Color(244, 67, 54), "Stop all agents");

        JButton pathButton = createStyledButton("Show Paths",
                new Color(0, 77, 64), "Visualize robot paths");

        JButton statsButton = createStyledButton("Statistics",
                new Color(26, 35, 126), "Show simulation statistics");

        // Row 5 - Utility buttons
        JButton clearLogButton = createStyledButton("Clear Log",
                new Color(96, 125, 139), "Clear the log panel");

        JButton deadlockButton = createStyledButton("Force Deadlock",
                new Color(183, 28, 28), "Create impossible deadlock scenario");

        JButton manualStaticButton = createStyledButton("Manual Static Setup",
                new Color(128, 0, 128), "Manually configure static robots");

        JButton updateStaticButton = createStyledButton("Update Static Robots",
                new Color(128, 0, 128), "Update static robot positions");

        // Add action listeners
        setupButton.addActionListener(e -> enterSetupMode());
        resetSetupButton.addActionListener(e -> resetSetup());
        autoSetupButton.addActionListener(e -> autoSetup());
        loadButton.addActionListener(e -> showPresetMenu());
        obstacleButton.addActionListener(e -> toggleObstacleEditMode(false));
        clearObstaclesButton.addActionListener(e -> clearAllObstacles());
        mazeButton.addActionListener(e -> createRandomMaze());
        wallButton.addActionListener(e -> addWall());
        staticRobotButton.addActionListener(e -> toggleStaticRobotEditMode(false));
        clearStaticRobotsButton.addActionListener(e -> clearAllStaticRobots());
        startButton.addActionListener(e -> startSimulation());
        stopButton.addActionListener(e -> stopJADEContainer());
        testButton.addActionListener(e -> testCollisionScenario());
        adaptiveButton.addActionListener(e -> startAdaptiveStaticRobotScenario());
        clearLogButton.addActionListener(e -> clearLog());
        deadlockButton.addActionListener(e -> createImpossibleDeadlock());
        pathButton.addActionListener(e -> showPaths());
        statsButton.addActionListener(e -> showStatistics());
        manualStaticButton.addActionListener(e -> manualStaticRobotSetup());
        updateStaticButton.addActionListener(e -> updateStaticRobots());

        // Add buttons to control panel
        controlPanel.add(setupButton);
        controlPanel.add(resetSetupButton);
        controlPanel.add(autoSetupButton);
        controlPanel.add(loadButton);
        controlPanel.add(obstacleButton);
        controlPanel.add(clearObstaclesButton);
        controlPanel.add(mazeButton);
        controlPanel.add(wallButton);
        controlPanel.add(staticRobotButton);
        controlPanel.add(clearStaticRobotsButton);
        controlPanel.add(adaptiveButton);
        controlPanel.add(testButton);
        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(pathButton);
        controlPanel.add(statsButton);
        controlPanel.add(clearLogButton);
        controlPanel.add(deadlockButton);
        controlPanel.add(manualStaticButton);
        controlPanel.add(updateStaticButton);

        add(controlPanel, BorderLayout.SOUTH);

        // Setup panel
        JPanel setupPanel = createSetupPanel();
        add(setupPanel, BorderLayout.WEST);

        // Set window size and position
        setSize(1200, 900);
        setLocationRelativeTo(null);
        setVisible(true);

        logMessage("=== JADE MULTI-ROBOT COORDINATION WITH ADAPTIVE STATIC ROBOTS ===");
        logMessage("New Feature: Static robots move after receiving 5 requests");
        logMessage("Static robots temporarily move to open path, then return to original position");
        logMessage("Right-click to remove obstacles/static robots while in edit mode");
        logMessage("-----------------------------------------------");

        // Start with auto setup
        autoSetup();
    }

    private class GridMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            int cellSize = gridPanel.getCellSize();
            int col = e.getX() / cellSize;
            int row = e.getY() / cellSize;

            // Validate click within grid
            if (row >= 0 && row < 5 && col >= 0 && col < 5) {
                if (gridPanel.isBlockEditMode()) {
                    // In obstacle edit mode
                    handleObstacleEdit(row, col, e.getButton() == MouseEvent.BUTTON3);
                } else if (staticRobotEditMode) {
                    // In static robot edit mode
                    handleStaticRobotEdit(row, col, e.getButton() == MouseEvent.BUTTON3);
                } else {
                    // In normal setup mode
                    handleGridClick(row, col);
                }
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            // Allow dragging to place/remove multiple obstacles or static robots
            int cellSize = gridPanel.getCellSize();
            int col = e.getX() / cellSize;
            int row = e.getY() / cellSize;

            if (row >= 0 && row < 5 && col >= 0 && col < 5) {
                boolean rightClick = (e.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) != 0;

                if (gridPanel.isBlockEditMode()) {
                    handleObstacleEdit(row, col, rightClick);
                } else if (staticRobotEditMode) {
                    handleStaticRobotEdit(row, col, rightClick);
                }
            }
        }
    }

    private void handleObstacleEdit(int row, int col, boolean rightClick) {
        // Toggle based on mouse button
        boolean shouldErase = rightClick != gridPanel.isEraseMode();

        if (shouldErase) {
            // Remove obstacle if present
            if (grid.isCellBlocked(row, col)) {
                grid.setCellBlocked(row, col, false);
                logMessage("[OBSTACLE] Removed obstacle from (" + row + "," + col + ")");
                gridPanel.repaint();
            }
        } else {
            // Add obstacle if cell is empty
            if (!grid.isCellBlocked(row, col) && grid.getRobotAt(row, col) == null) {
                grid.setCellBlocked(row, col, true);
                logMessage("[OBSTACLE] Added obstacle at (" + row + "," + col + ")");
                gridPanel.repaint();
            }
        }
    }

    private void handleStaticRobotEdit(int row, int col, boolean rightClick) {
        boolean shouldErase = rightClick != eraseStaticMode;

        if (shouldErase) {
            // Remove static robot if present
            RobotAgent robot = grid.getRobotAt(row, col);
            if (robot != null && robot.isStatic()) {
                grid.freeCell(row, col);
                logMessage("[STATIC] Removed static robot from (" + row + "," + col + ")");
                gridPanel.repaint();
            }
        } else {
            // Add static robot if cell is empty
            if (!grid.isCellBlocked(row, col) && grid.getRobotAt(row, col) == null) {
                // We'll create static robot later through manual setup or update
                logMessage("[STATIC] Marked cell (" + row + "," + col + ") for static robot");
                // For now, just mark it visually
                // The actual static robot will be created when starting simulation
            }
        }
    }

    private void toggleObstacleEditMode(boolean eraseMode) {
        if (gridPanel.isBlockEditMode()) {
            exitObstacleEditMode();
        } else {
            enterObstacleEditMode(eraseMode);
        }
    }

    private void enterObstacleEditMode(boolean eraseMode) {
        gridPanel.setBlockEditMode(true, eraseMode);
        obstacleButton.setText(eraseMode ? "Exit Remove Mode" : "Exit Place Mode");
        obstacleButton.setBackground(eraseMode ? new Color(255, 152, 0) : new Color(255, 87, 34));
        obstacleButton.setToolTipText("Click to exit obstacle edit mode");

        // Disable static robot edit mode if active
        if (staticRobotEditMode) {
            exitStaticRobotEditMode();
        }

        // Force ready state
        currentSetupState = SetupState.READY;

        logMessage("[OBSTACLE] Entered obstacle edit mode");
        logMessage("[OBSTACLE] " + (eraseMode ?
                "Click cells to REMOVE obstacles (Left-click = remove, Right-click = add)" :
                "Click cells to ADD obstacles (Left-click = add, Right-click = remove)"));
        logMessage("[OBSTACLE] Drag to place/remove multiple obstacles");
        logMessage("[OBSTACLE] Press ESC or click 'Exit Place Mode' to finish");

        // Request focus for key listener
        gridPanel.requestFocusInWindow();
    }

    private void exitObstacleEditMode() {
        gridPanel.setBlockEditMode(false, false);
        obstacleButton.setText("Place Obstacles");
        obstacleButton.setBackground(new Color(255, 87, 34));
        obstacleButton.setToolTipText("Click cells to add/remove obstacles");

        logMessage("[OBSTACLE] Exited obstacle edit mode");
        logMessage("[OBSTACLE] Total obstacles: " + grid.getBlockedCellCount());

        gridPanel.repaint();
    }

    private void toggleStaticRobotEditMode(boolean eraseMode) {
        if (staticRobotEditMode) {
            exitStaticRobotEditMode();
        } else {
            enterStaticRobotEditMode(eraseMode);
        }
    }

    private void enterStaticRobotEditMode(boolean eraseMode) {
        staticRobotEditMode = true;
        eraseStaticMode = eraseMode;
        staticRobotButton.setText(eraseMode ? "Exit Remove Static" : "Exit Add Static");
        staticRobotButton.setBackground(eraseMode ? new Color(255, 152, 0) : new Color(128, 0, 128));
        staticRobotButton.setToolTipText("Click to exit static robot edit mode");

        // Disable obstacle edit mode if active
        if (gridPanel.isBlockEditMode()) {
            exitObstacleEditMode();
        }

        // Force ready state
        currentSetupState = SetupState.READY;

        logMessage("[STATIC] Entered static robot edit mode");
        logMessage("[STATIC] " + (eraseMode ?
                "Click cells to REMOVE static robots (Left-click = remove, Right-click = add)" :
                "Click cells to ADD static robots (Left-click = add, Right-click = remove)"));
        logMessage("[STATIC] Drag to place/remove multiple static robots");
        logMessage("[STATIC] Press ESC or click 'Exit Add Static' to finish");

        // Request focus for key listener
        gridPanel.requestFocusInWindow();
    }

    private void exitStaticRobotEditMode() {
        staticRobotEditMode = false;
        staticRobotButton.setText("Add Static Robots");
        staticRobotButton.setBackground(new Color(128, 0, 128));
        staticRobotButton.setToolTipText("Click cells to add/remove static robots");

        logMessage("[STATIC] Exited static robot edit mode");

        gridPanel.repaint();
    }

    private void clearAllObstacles() {
        grid.clearAllBlockedCells();
        logMessage("[OBSTACLE] Cleared all obstacles from grid");
        gridPanel.repaint();
    }

    private void clearAllStaticRobots() {
        synchronized(grid) {
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 5; j++) {
                    RobotAgent robot = grid.getRobotAt(i, j);
                    if (robot != null && robot.isStatic()) {
                        grid.freeCell(i, j);
                    }
                }
            }
        }
        logMessage("[STATIC] Cleared all static robots from grid");
        gridPanel.repaint();
    }

    private void createRandomMaze() {
        clearAllObstacles();

        // Create random maze pattern (about 30% of cells)
        int totalCells = 5 * 5;
        int obstacleCount = (int)(totalCells * 0.3);

        for (int i = 0; i < obstacleCount; i++) {
            int row = (int)(Math.random() * 5);
            int col = (int)(Math.random() * 5);

            // Don't block start/target positions
            if ((row == robot1StartX && col == robot1StartY) ||
                    (row == robot1TargetX && col == robot1TargetY) ||
                    (row == robot2StartX && col == robot2StartY) ||
                    (row == robot2TargetX && col == robot2TargetY)) {
                continue;
            }

            grid.setCellBlocked(row, col, true);
        }

        logMessage("[OBSTACLE] Created random maze with " + grid.getBlockedCellCount() + " obstacles");
        gridPanel.repaint();
    }

    private void addWall() {
        // Add a vertical wall in the middle
        for (int row = 0; row < 5; row++) {
            int col = 2; // Middle column

            // Don't block start/target positions
            if ((row == robot1StartX && col == robot1StartY) ||
                    (row == robot1TargetX && col == robot1TargetY) ||
                    (row == robot2StartX && col == robot2StartY) ||
                    (row == robot2TargetX && col == robot2TargetY)) {
                continue;
            }

            grid.setCellBlocked(row, col, true);
        }

        logMessage("[OBSTACLE] Added vertical wall at column 2");
        gridPanel.repaint();
    }

    private void manualStaticRobotSetup() {
        logMessage("[STATIC] Manual static robot setup");
        logMessage("[STATIC] 1. Click 'Add Static Robots' button");
        logMessage("[STATIC] 2. Click cells to mark static robot positions");
        logMessage("[STATIC] 3. Click 'Update Static Robots' to create them");
        logMessage("[STATIC] 4. Start simulation with static robots in place");
    }

    private void updateStaticRobots() {
        logMessage("[STATIC] Updating static robots...");
        // This would normally create static robots at marked positions
        // For now, we'll just log a message
        logMessage("[STATIC] Use 'Adaptive Static Robots' button for pre-configured scenario");
    }

    private JButton createStyledButton(String text, Color bgColor, String tooltip) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        return button;
    }

    private void handleGridClick(int row, int col) {
        // Check if cell is blocked before setting position
        if (grid.isCellBlocked(row, col)) {
            logMessage("[SETUP] Cannot place robot on blocked cell (" + row + "," + col + ")");
            JOptionPane.showMessageDialog(this,
                    "Cannot place robot on a blocked cell!\n" +
                            "Remove the obstacle first or choose another cell.",
                    "Cell Blocked", JOptionPane.WARNING_MESSAGE);
            return;
        }

        switch (currentSetupState) {
            case SELECT_ROBOT1_START:
                robot1StartX = row;
                robot1StartY = col;
                updateSetupPanel();
                currentSetupState = SetupState.SELECT_ROBOT1_TARGET;
                setupInstruction = "Click to select Robot1 Target Position";
                logMessage("[SETUP] Robot1 Start set to (" + row + ", " + col + ")");
                break;

            case SELECT_ROBOT1_TARGET:
                robot1TargetX = row;
                robot1TargetY = col;
                updateSetupPanel();
                currentSetupState = SetupState.SELECT_ROBOT2_START;
                setupInstruction = "Click to select Robot2 Start Position";
                logMessage("[SETUP] Robot1 Target set to (" + row + ", " + col + ")");
                break;

            case SELECT_ROBOT2_START:
                robot2StartX = row;
                robot2StartY = col;
                updateSetupPanel();
                currentSetupState = SetupState.SELECT_ROBOT2_TARGET;
                setupInstruction = "Click to select Robot2 Target Position";
                logMessage("[SETUP] Robot2 Start set to (" + row + ", " + col + ")");
                break;

            case SELECT_ROBOT2_TARGET:
                robot2TargetX = row;
                robot2TargetY = col;
                updateSetupPanel();
                currentSetupState = SetupState.READY;
                setupInstruction = "Setup Complete! Click 'Start Simulation'";
                logMessage("[SETUP] Robot2 Target set to (" + row + ", " + col + ")");
                logMessage("[SETUP] ✓ Setup complete! Ready to start simulation.");
                break;
        }

        updateInstructionLabel();
        gridPanel.repaint();
    }

    private JPanel createSetupPanel() {
        JPanel setupPanel = new JPanel();
        setupPanel.setLayout(new BoxLayout(setupPanel, BoxLayout.Y_AXIS));
        setupPanel.setBorder(BorderFactory.createTitledBorder("Robot Configuration"));
        setupPanel.setBackground(new Color(248, 248, 248));
        setupPanel.setPreferredSize(new Dimension(250, 0));

        // Title
        JLabel titleLabel = new JLabel("Robot Setup");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(new Color(33, 33, 33));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Instruction label
        JLabel instructionLabel = new JLabel(setupInstruction);
        instructionLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        instructionLabel.setForeground(new Color(100, 100, 100));
        instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        instructionLabel.setName("instructionLabel");

        // Robot 1 configuration
        JPanel robot1Panel = createRobotConfigPanel("Robot 1 (Red)",
                robot1StartX, robot1StartY, robot1TargetX, robot1TargetY, Color.RED);
        robot1Panel.setName("robot1Panel");

        // Robot 2 configuration
        JPanel robot2Panel = createRobotConfigPanel("Robot 2 (Blue)",
                robot2StartX, robot2StartY, robot2TargetX, robot2TargetY, Color.BLUE);
        robot2Panel.setName("robot2Panel");

        // Static robots info
        JPanel staticPanel = new JPanel();
        staticPanel.setLayout(new BoxLayout(staticPanel, BoxLayout.Y_AXIS));
        staticPanel.setBorder(BorderFactory.createTitledBorder("Static Robots Configuration"));
        staticPanel.setBackground(new Color(250, 250, 250));
        staticPanel.setMaximumSize(new Dimension(230, 150));

        JLabel staticInfo = new JLabel("<html><center>Click 'Add Static Robots' to place<br>static robots on the grid<br><br>Static robots will block path<br>until they receive enough requests<br>then turn purple and move temporarily</center></html>");
        staticInfo.setFont(new Font("Arial", Font.PLAIN, 11));
        staticInfo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel staticControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        staticControls.setBackground(new Color(250, 250, 250));

        JButton quickStaticBtn = new JButton("Quick Static Setup");
        quickStaticBtn.setFont(new Font("Arial", Font.PLAIN, 10));
        quickStaticBtn.setBackground(new Color(128, 0, 128));
        quickStaticBtn.setForeground(Color.WHITE);
        quickStaticBtn.addActionListener(e -> quickStaticSetup());
        staticControls.add(quickStaticBtn);

        staticPanel.add(Box.createVerticalStrut(5));
        staticPanel.add(staticInfo);
        staticPanel.add(Box.createVerticalStrut(5));
        staticPanel.add(staticControls);

        // Status label
        JLabel statusLabel = new JLabel("Status: Setup Required");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        statusLabel.setForeground(Color.RED);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setName("statusLabel");

        // Add components
        setupPanel.add(Box.createVerticalStrut(10));
        setupPanel.add(titleLabel);
        setupPanel.add(Box.createVerticalStrut(10));
        setupPanel.add(instructionLabel);
        setupPanel.add(Box.createVerticalStrut(20));
        setupPanel.add(robot1Panel);
        setupPanel.add(Box.createVerticalStrut(10));
        setupPanel.add(robot2Panel);
        setupPanel.add(Box.createVerticalStrut(10));
        setupPanel.add(staticPanel);
        setupPanel.add(Box.createVerticalStrut(20));
        setupPanel.add(statusLabel);
        setupPanel.add(Box.createVerticalGlue());

        return setupPanel;
    }

    private void quickStaticSetup() {
        // Create a simple static robot blocking scenario
        clearAllStaticRobots();
        clearAllObstacles();

        // Set up robots in a crossing pattern with static robots blocking
        robot1StartX = 0; robot1StartY = 2;
        robot1TargetX = 4; robot1TargetY = 2;
        robot2StartX = 4; robot2StartY = 2;
        robot2TargetX = 0; robot2TargetY = 2;

        currentSetupState = SetupState.READY;
        setupInstruction = "Quick Static Setup Complete!";

        updateSetupPanel();
        updateInstructionLabel();
        visualizeSetup();

        logMessage("[STATIC] Quick static setup complete");
        logMessage("[STATIC] Robot1: Start (0,2) → Target (4,2)");
        logMessage("[STATIC] Robot2: Start (4,2) → Target (0,2)");
        logMessage("[STATIC] Now click 'Adaptive Static Robots' to test");
    }

    private JPanel createRobotConfigPanel(String title, int startX, int startY,
                                          int targetX, int targetY, Color color) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.setBackground(new Color(250, 250, 250));
        panel.setMaximumSize(new Dimension(230, 120));

        // Color indicator
        JPanel colorPanel = new JPanel();
        colorPanel.setBackground(color);
        colorPanel.setPreferredSize(new Dimension(20, 20));
        colorPanel.setMaximumSize(new Dimension(20, 20));
        colorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Start position label
        String startText = (startX == -1) ? "Not set" : String.format("(%d, %d)", startX, startY);
        JLabel startLabel = new JLabel("Start: " + startText);
        startLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        startLabel.setName("startLabel");

        // Target position label
        String targetText = (targetX == -1) ? "Not set" : String.format("(%d, %d)", targetX, targetY);
        JLabel targetLabel = new JLabel("Target: " + targetText);
        targetLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        targetLabel.setName("targetLabel");

        // Add components
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        header.add(colorPanel);
        header.add(new JLabel(title.split(" ")[0]));
        header.setBackground(new Color(250, 250, 250));

        panel.add(header);
        panel.add(Box.createVerticalStrut(5));
        panel.add(startLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(targetLabel);

        return panel;
    }

    private void enterSetupMode() {
        resetSetup();
        currentSetupState = SetupState.SELECT_ROBOT1_START;
        setupInstruction = "Click to select Robot1 Start Position";
        updateInstructionLabel();
        logMessage("[SETUP] Entering setup mode. Click grid cells to set positions.");
    }

    private void resetSetup() {
        robot1StartX = robot1StartY = robot1TargetX = robot1TargetY = -1;
        robot2StartX = robot2StartY = robot2TargetX = robot2TargetY = -1;
        currentSetupState = SetupState.SELECT_ROBOT1_START;
        setupInstruction = "Click to select Robot1 Start Position";

        synchronized(grid) {
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 5; j++) {
                    grid.freeCell(i, j);
                }
            }
        }

        updateSetupPanel();
        updateInstructionLabel();
        gridPanel.repaint();
        logMessage("[SETUP] All positions reset.");
    }

    private void autoSetup() {
        robot1StartX = 0; robot1StartY = 0;
        robot1TargetX = 4; robot1TargetY = 4;
        robot2StartX = 4; robot2StartY = 0;
        robot2TargetX = 0; robot2TargetY = 4;

        currentSetupState = SetupState.READY;
        setupInstruction = "Auto Setup Complete!";

        updateSetupPanel();
        updateInstructionLabel();
        visualizeSetup();

        logMessage("[SETUP] Auto setup applied.");
        logMessage("[SETUP] Robot1: Start (0,0) → Target (4,4)");
        logMessage("[SETUP] Robot2: Start (4,0) → Target (0,4)");
    }

    private void visualizeSetup() {
        synchronized(grid) {
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 5; j++) {
                    grid.freeCell(i, j);
                }
            }

            gridPanel.setSetupPositions(robot1StartX, robot1StartY, robot1TargetX, robot1TargetY,
                    robot2StartX, robot2StartY, robot2TargetX, robot2TargetY);
        }
        gridPanel.repaint();
    }

    private void showPresetMenu() {
        JPopupMenu presetMenu = new JPopupMenu();

        JMenuItem defaultItem = new JMenuItem("Default Crossing");
        defaultItem.addActionListener(e -> loadPreset(0, 0, 4, 4, 4, 0, 0, 4));

        JMenuItem parallelItem = new JMenuItem("Parallel Paths");
        parallelItem.addActionListener(e -> loadPreset(0, 1, 4, 1, 0, 3, 4, 3));

        JMenuItem cornerItem = new JMenuItem("Corner Exchange");
        cornerItem.addActionListener(e -> loadPreset(0, 0, 0, 4, 4, 4, 4, 0));

        JMenuItem collisionItem = new JMenuItem("Head-on Collision");
        collisionItem.addActionListener(e -> loadPreset(0, 2, 4, 2, 4, 2, 0, 2));

        JMenuItem adaptiveItem = new JMenuItem("Adaptive Static Test");
        adaptiveItem.addActionListener(e -> loadAdaptivePreset());

        JMenuItem staticBlockItem = new JMenuItem("Static Block Scenario");
        staticBlockItem.addActionListener(e -> loadStaticBlockPreset());

        presetMenu.add(defaultItem);
        presetMenu.add(parallelItem);
        presetMenu.add(cornerItem);
        presetMenu.add(collisionItem);
        presetMenu.addSeparator();
        presetMenu.add(adaptiveItem);
        presetMenu.add(staticBlockItem);

        JButton loadButton = (JButton) ((JPanel) getContentPane().getComponent(2)).getComponent(3);
        presetMenu.show(loadButton, 0, loadButton.getHeight());
    }

    private void loadPreset(int r1sx, int r1sy, int r1tx, int r1ty,
                            int r2sx, int r2sy, int r2tx, int r2ty) {
        robot1StartX = r1sx; robot1StartY = r1sy;
        robot1TargetX = r1tx; robot1TargetY = r1ty;
        robot2StartX = r2sx; robot2StartY = r2sy;
        robot2TargetX = r2tx; robot2TargetY = r2ty;

        currentSetupState = SetupState.READY;
        setupInstruction = "Preset Loaded!";

        updateSetupPanel();
        updateInstructionLabel();
        visualizeSetup();

        logMessage("[SETUP] Preset loaded.");
    }

    private void loadAdaptivePreset() {
        robot1StartX = 0; robot1StartY = 2;
        robot1TargetX = 4; robot1TargetY = 2;
        robot2StartX = 4; robot2StartY = 2;
        robot2TargetX = 0; robot2TargetY = 2;

        currentSetupState = SetupState.READY;
        setupInstruction = "Adaptive Preset Loaded!";

        updateSetupPanel();
        updateInstructionLabel();
        visualizeSetup();

        logMessage("[SETUP] Adaptive static robot preset loaded.");
        logMessage("[SETUP] Main robots will be blocked by static robots in middle");
        logMessage("[SETUP] Static robots will move after 3 requests");
    }

    private void loadStaticBlockPreset() {
        robot1StartX = 0; robot1StartY = 0;
        robot1TargetX = 4; robot1TargetY = 4;
        robot2StartX = 0; robot2StartY = 4;
        robot2TargetX = 4; robot2TargetY = 0;

        currentSetupState = SetupState.READY;
        setupInstruction = "Static Block Preset Loaded!";

        updateSetupPanel();
        updateInstructionLabel();
        visualizeSetup();

        logMessage("[SETUP] Static block preset loaded.");
        logMessage("[SETUP] Robots will be blocked by static robots at (2,2)");
    }

    private void startAdaptiveStaticRobotScenario() {
        logMessage("[ADAPTIVE] ===============================================");
        logMessage("[ADAPTIVE] ADAPTIVE STATIC ROBOT SCENARIO");
        logMessage("[ADAPTIVE] ===============================================");
        logMessage("[ADAPTIVE] Creating BLOCKING static robots at row 2");
        logMessage("[ADAPTIVE] Main robots will be COMPLETELY BLOCKED");
        logMessage("[ADAPTIVE] Static robots will move after 3 requests");
        logMessage("[ADAPTIVE] After main robots pass, static robots return");
        logMessage("[ADAPTIVE] ===============================================");

        new Thread(() -> {
            try {
                Runtime rt = Runtime.instance();
                Profile profile = new ProfileImpl();
                profile.setParameter(Profile.MAIN_HOST, "localhost");
                profile.setParameter(Profile.MAIN_PORT, "1099");
                profile.setParameter(Profile.GUI, "false");

                AgentContainer container = rt.createMainContainer(profile);

                // Clear grid
                resetSimulation();

                // Set up adaptive static robot scenario
                // Robot1 and Robot2 need to go THROUGH row 2
                robot1StartX = 0; robot1StartY = 2;
                robot1TargetX = 4; robot1TargetY = 2;
                robot2StartX = 4; robot2StartY = 2;
                robot2TargetX = 0; robot2TargetY = 2;

                logMessage("[ADAPTIVE] Creating BLOCKING static robots at row 2...");

                // Create static robots that ACTUALLY BLOCK the path at (2,2)
                // We need static robots in the exact path
                String[] staticRobotNames = {"BlockTop", "BlockMiddle", "BlockBottom"};
                int[][] staticPositions = {
                        {2, 1},  // Left of middle - BLOCKS Robot1's path
                        {2, 2},  // MIDDLE - THIS IS THE KEY! Blocks the actual path
                        {2, 3}   // Right of middle - BLOCKS Robot2's path
                };

                // Original positions for static robots to return to
                int[][] originalPositions = {
                        {2, 1},
                        {2, 2},  // Middle robot's original position
                        {2, 3}
                };

                for (int i = 0; i < staticRobotNames.length; i++) {
                    String blockId = staticRobotNames[i];
                    Object[] blockArgs = {
                            blockId,           // robotId
                            staticPositions[i][0],  // currentX
                            staticPositions[i][1],  // currentY
                            staticPositions[i][0],  // targetX (same)
                            staticPositions[i][1],  // targetY (same)
                            true,              // isStatic = true
                            3,                 // REDUCED: requests needed to move = 3 (for faster testing)
                            originalPositions[i][0], // originalX
                            originalPositions[i][1], // originalY
                            grid,              // shared grid
                            this              // main frame
                    };

                    AgentController blocker = container.createNewAgent(
                            blockId,
                            "RobotAgent",
                            blockArgs
                    );
                    blocker.start();
                    logMessage("[ADAPTIVE] Created BLOCKING static robot at (" +
                            staticPositions[i][0] + "," + staticPositions[i][1] + ")");
                }

                logMessage("[ADAPTIVE] Creating main robots...");

                // Create Robot1
                Object[] robot1Args = {
                        "Robot1",           // robotId
                        robot1StartX,       // startX
                        robot1StartY,       // startY
                        robot1TargetX,      // targetX
                        robot1TargetY,      // targetY
                        false,              // isStatic = false
                        0,                  // requests needed (not used)
                        0,                  // originalX (not used)
                        0,                  // originalY (not used)
                        grid,               // shared grid
                        this               // main frame
                };

                AgentController robot1 = container.createNewAgent(
                        "Robot1",
                        "RobotAgent",
                        robot1Args
                );

                // Create Robot2
                Object[] robot2Args = {
                        "Robot2",           // robotId
                        robot2StartX,       // startX
                        robot2StartY,       // startY
                        robot2TargetX,      // targetX
                        robot2TargetY,      // targetY
                        false,              // isStatic = false
                        0,                  // requests needed (not used)
                        0,                  // originalX (not used)
                        0,                  // originalY (not used)
                        grid,               // shared grid
                        this               // main frame
                };

                AgentController robot2 = container.createNewAgent(
                        "Robot2",
                        "RobotAgent",
                        robot2Args
                );

                logMessage("[ADAPTIVE] Starting main robots...");
                robot1.start();
                robot2.start();

                logMessage("[ADAPTIVE] ✓ Adaptive static robot scenario started!");
                logMessage("[ADAPTIVE] CRITICAL: Static robot at (2,2) BLOCKS the middle!");
                logMessage("[ADAPTIVE] Main robots CANNOT pass without static robots moving");
                logMessage("[ADAPTIVE] Watch static robots count requests (shown as 'Req:X')");
                logMessage("[ADAPTIVE] After 3 requests, static robots will turn purple and move");
                logMessage("[ADAPTIVE] After main robots pass, static robots return to original positions");
                logMessage("[ADAPTIVE] ===============================================");

                updateGrid();

            } catch (Exception e) {
                logMessage("[ADAPTIVE ERROR] " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void createImpossibleDeadlock() {
        logMessage("[TEST] ===============================================");
        logMessage("[TEST] CREATING IMPOSSIBLE DEADLOCK SCENARIO");
        logMessage("[TEST] ===============================================");
        logMessage("[TEST] Robots completely blocked on all sides");
        logMessage("[TEST] Should timeout and conclude path is impossible");
        logMessage("[TEST] ===============================================");

        new Thread(() -> {
            try {
                Runtime rt = Runtime.instance();
                Profile profile = new ProfileImpl();
                profile.setParameter(Profile.MAIN_HOST, "localhost");
                profile.setParameter(Profile.MAIN_PORT, "1099");
                profile.setParameter(Profile.GUI, "false");

                AgentContainer container = rt.createMainContainer(profile);
                resetSimulation();

                // Create completely blocked scenario
                // Robot1 surrounded by static robots
                robot1StartX = 2; robot1StartY = 2;
                robot1TargetX = 4; robot1TargetY = 4;

                logMessage("[TEST] Creating impossible deadlock at (2,2)...");

                // Surround Robot1 with static robots
                int[][] blockers = {
                        {1, 2}, // above
                        {3, 2}, // below
                        {2, 1}, // left
                        {2, 3}  // right
                };

                for (int i = 0; i < blockers.length; i++) {
                    String blockId = "BlockSurround" + i;
                    Object[] blockArgs = {
                            blockId,
                            blockers[i][0],
                            blockers[i][1],
                            blockers[i][0],
                            blockers[i][1],
                            true,
                            3, // requests needed
                            blockers[i][0], // originalX
                            blockers[i][1], // originalY
                            grid,
                            this
                    };

                    AgentController blocker = container.createNewAgent(blockId, "RobotAgent", blockArgs);
                    blocker.start();
                    logMessage("[TEST] Created blocker at (" + blockers[i][0] + "," + blockers[i][1] + ")");
                }

                // Create Robot1 (trapped)
                Object[] robot1Args = {
                        "Robot1",
                        robot1StartX,
                        robot1StartY,
                        robot1TargetX,
                        robot1TargetY,
                        false,
                        0,
                        0,
                        0,
                        grid,
                        this
                };

                AgentController robot1 = container.createNewAgent("Robot1", "RobotAgent", robot1Args);
                robot1.start();

                logMessage("[TEST] ✓ Impossible deadlock scenario created!");
                logMessage("[TEST] Robot1 should detect deadlock and timeout after 5 seconds");
                logMessage("[TEST] ===============================================");

                updateGrid();

            } catch (Exception e) {
                logMessage("[TEST ERROR] " + e.getMessage());
            }
        }).start();
    }

    private void showPaths() {
        logMessage("[INFO] Path visualization would show robot trajectories");
        logMessage("[INFO] Feature would draw lines showing past movements");
    }

    private void showStatistics() {
        // This would show move counts, collision attempts, etc.
        logMessage("[STATS] Statistics feature would show:");
        logMessage("[STATS] - Total moves made");
        logMessage("[STATS] - Collision attempts prevented");
        logMessage("[STATS] - Deadlock detections");
        logMessage("[STATS] - Alternate paths found");
    }

    private void startSimulation() {
        if (currentSetupState != SetupState.READY) {
            JOptionPane.showMessageDialog(this,
                    "Please complete the setup first!\n" +
                            "Current step: " + setupInstruction,
                    "Setup Incomplete", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Check if start/target positions are blocked
        if (grid.isCellBlocked(robot1StartX, robot1StartY) ||
                grid.isCellBlocked(robot1TargetX, robot1TargetY) ||
                grid.isCellBlocked(robot2StartX, robot2StartY) ||
                grid.isCellBlocked(robot2TargetX, robot2TargetY)) {

            JOptionPane.showMessageDialog(this,
                    "Cannot start simulation: Some robot positions are on blocked cells!\n" +
                            "Please remove obstacles from start/target positions or choose new positions.",
                    "Blocked Positions", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new Thread(() -> {
            try {
                logMessage("[SYSTEM] ===============================================");
                logMessage("[SYSTEM] STARTING SIMULATION WITH ADAPTIVE STATIC ROBOTS");
                logMessage("[SYSTEM] ===============================================");
                logMessage("[SYSTEM] Obstacle count: " + grid.getBlockedCellCount());
                logMessage("[SYSTEM] Robot1: Start (" + robot1StartX + "," + robot1StartY +
                        ") → Target (" + robot1TargetX + "," + robot1TargetY + ")");
                logMessage("[SYSTEM] Robot2: Start (" + robot2StartX + "," + robot2StartY +
                        ") → Target (" + robot2TargetX + "," + robot2TargetY + ")");
                logMessage("[SYSTEM] ===============================================");

                Runtime rt = Runtime.instance();
                Profile profile = new ProfileImpl();
                profile.setParameter(Profile.MAIN_HOST, "localhost");
                profile.setParameter(Profile.MAIN_PORT, "1099");
                profile.setParameter(Profile.GUI, "false");

                AgentContainer container = rt.createMainContainer(profile);
                resetSimulation();

                // Create Robot1
                Object[] robot1Args = {
                        "Robot1",
                        robot1StartX,
                        robot1StartY,
                        robot1TargetX,
                        robot1TargetY,
                        false,
                        0,
                        0,
                        0,
                        grid,
                        this
                };

                AgentController robot1 = container.createNewAgent("Robot1", "RobotAgent", robot1Args);

                // Create Robot2
                Object[] robot2Args = {
                        "Robot2",
                        robot2StartX,
                        robot2StartY,
                        robot2TargetX,
                        robot2TargetY,
                        false,
                        0,
                        0,
                        0,
                        grid,
                        this
                };

                AgentController robot2 = container.createNewAgent("Robot2", "RobotAgent", robot2Args);

                logMessage("[SYSTEM] Starting agents...");
                robot1.start();
                robot2.start();

                logMessage("[SYSTEM] ✓ Simulation started!");
                logMessage("[SYSTEM] -----------------------------------------------");

                // Exit edit modes if active
                if (gridPanel.isBlockEditMode()) {
                    exitObstacleEditMode();
                }
                if (staticRobotEditMode) {
                    exitStaticRobotEditMode();
                }

                updateGrid();

            } catch (Exception e) {
                logMessage("[ERROR] Failed to start simulation: " + e.getMessage());
            }
        }).start();
    }

    private void testCollisionScenario() {
        loadPreset(0, 2, 4, 2, 4, 2, 0, 2);
        startSimulation();
    }

    private void stopJADEContainer() {
        try {
            Runtime rt = Runtime.instance();
            if (rt != null) {
                rt.shutDown();
                logMessage("[SYSTEM] JADE Runtime shut down");
            }
        } catch (Exception e) {
            logMessage("[ERROR] Error stopping JADE: " + e.getMessage());
        }
    }

    private void updateSetupPanel() {
        SwingUtilities.invokeLater(() -> {
            updateInstructionLabel();
            updateRobotPanel("robot1Panel", robot1StartX, robot1StartY,
                    robot1TargetX, robot1TargetY);
            updateRobotPanel("robot2Panel", robot2StartX, robot2StartY,
                    robot2TargetX, robot2TargetY);
            updateStatusLabel();
        });
    }

    private void updateRobotPanel(String panelName, int startX, int startY, int targetX, int targetY) {
        JPanel robotPanel = (JPanel) findComponentByName(getContentPane(), panelName);
        if (robotPanel != null) {
            JLabel startLabel = (JLabel) findComponentByName(robotPanel, "startLabel");
            JLabel targetLabel = (JLabel) findComponentByName(robotPanel, "targetLabel");

            if (startLabel != null) {
                String startText = (startX == -1) ? "Not set" : String.format("(%d, %d)", startX, startY);
                startLabel.setText("Start: " + startText);
            }

            if (targetLabel != null) {
                String targetText = (targetX == -1) ? "Not set" : String.format("(%d, %d)", targetX, targetY);
                targetLabel.setText("Target: " + targetText);
            }
        }
    }

    private void updateInstructionLabel() {
        SwingUtilities.invokeLater(() -> {
            JLabel instructionLabel = (JLabel) findComponentByName(getContentPane(), "instructionLabel");
            if (instructionLabel != null) {
                instructionLabel.setText(setupInstruction);

                if (currentSetupState == SetupState.READY) {
                    instructionLabel.setForeground(new Color(0, 150, 0));
                    instructionLabel.setFont(new Font("Arial", Font.BOLD, 12));
                } else {
                    instructionLabel.setForeground(new Color(200, 100, 0));
                    instructionLabel.setFont(new Font("Arial", Font.ITALIC, 12));
                }
            }
        });
    }

    private void updateStatusLabel() {
        SwingUtilities.invokeLater(() -> {
            JLabel statusLabel = (JLabel) findComponentByName(getContentPane(), "statusLabel");
            if (statusLabel != null) {
                if (currentSetupState == SetupState.READY) {
                    statusLabel.setText("Status: Ready to Start");
                    statusLabel.setForeground(new Color(0, 150, 0));
                } else {
                    statusLabel.setText("Status: Setup in Progress");
                    statusLabel.setForeground(Color.ORANGE);
                }
            }
        });
    }

    private Component findComponentByName(Container container, String name) {
        for (Component comp : container.getComponents()) {
            if (name.equals(comp.getName())) {
                return comp;
            }
            if (comp instanceof Container) {
                Component found = findComponentByName((Container) comp, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void clearLog() {
        logArea.setText("");
        logMessage("[SYSTEM] Log cleared");
    }

    private void resetSimulation() {
        synchronized(grid) {
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 5; j++) {
                    grid.freeCell(i, j);
                }
            }
        }

        gridPanel.clearSetupPositions();
        updateGrid();
        logMessage("[SYSTEM] Simulation grid cleared");
    }

    public void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void updateGrid() {
        SwingUtilities.invokeLater(() -> {
            gridPanel.repaint();
        });
    }

    public static void main(String[] args) {
        System.setProperty("java.util.logging.config.file", "logging.properties");

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new MainFrame();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}

