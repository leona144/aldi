
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.Set;

public class GridPanel extends JPanel {
    private Grid grid;
    private int cellSize = 80;
    private Font coordFont = new Font("Arial", Font.PLAIN, 10);
    private Font robotFont = new Font("Arial", Font.BOLD, 14);
    private Font clockFont = new Font("Arial", Font.PLAIN, 10);
    private Font deadlockFont = new Font("Arial", Font.BOLD, 10);
    private Font blockedFont = new Font("Arial", Font.BOLD, 16);
    private boolean showSetup = false;
    private int robot1StartX = -1, robot1StartY = -1;
    private int robot1TargetX = -1, robot1TargetY = -1;
    private int robot2StartX = -1, robot2StartY = -1;
    private int robot2TargetX = -1, robot2TargetY = -1;
    private boolean blockEditMode = false;
    private boolean eraseMode = false; // true = erase blocks, false = add blocks

    public GridPanel(Grid grid) {
        this.grid = grid;
        setPreferredSize(new Dimension(
                grid.getCols() * cellSize,
                grid.getRows() * cellSize
        ));
        setBackground(new Color(250, 250, 250));
        setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 2));
    }
    public void setBlockEditMode(boolean enabled, boolean erase) {
        this.blockEditMode = enabled;
        this.eraseMode = erase;
        if (enabled) {
            setToolTipText(erase ?
                    "Click cells to REMOVE obstacles (Right-click to add)" :
                    "Click cells to ADD obstacles (Right-click to remove)");
        } else {
            setToolTipText(null);
        }
        repaint();
    }

    public boolean isBlockEditMode() {
        return blockEditMode;
    }

    public boolean isEraseMode() {
        return eraseMode;
    }

    public void toggleCellBlocked(int row, int col) {
        if (grid != null) {
            if (eraseMode) {
                grid.setCellBlocked(row, col, false);
            } else {
                grid.setCellBlocked(row, col, true);
            }
            repaint();
        }
    }

    public void setSetupPositions(int r1sx, int r1sy, int r1tx, int r1ty,
                                  int r2sx, int r2sy, int r2tx, int r2ty) {
        this.robot1StartX = r1sx;
        this.robot1StartY = r1sy;
        this.robot1TargetX = r1tx;
        this.robot1TargetY = r1ty;
        this.robot2StartX = r2sx;
        this.robot2StartY = r2sy;
        this.robot2TargetX = r2tx;
        this.robot2TargetY = r2ty;
        this.showSetup = true;
    }

    public void clearSetupPositions() {
        this.showSetup = false;
        this.robot1StartX = this.robot1StartY = -1;
        this.robot1TargetX = this.robot1TargetY = -1;
        this.robot2StartX = this.robot2StartY = -1;
        this.robot2TargetX = this.robot2TargetY = -1;
    }

    public int getCellSize() {
        return cellSize;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        drawGridBackground(g2d);
        drawGridLines(g2d);
        drawCellCoordinates(g2d);
        if (showSetup) {
            drawSetupPositions(g2d);
        }
        drawRobots(g2d);
        drawLegend(g2d);
        drawTitle(g2d);
        if (blockEditMode) {
            drawEditModeIndicator(g2d);
        }
    }

    private void drawGridBackground(Graphics2D g2d) {
        Set<String> blockedCells = grid.getBlockedCells();

        for (int i = 0; i < grid.getRows(); i++) {
            for (int j = 0; j < grid.getCols(); j++) {
                String cellKey = i + "," + j;
                boolean isBlocked = blockedCells.contains(cellKey);

                Color bgColor;
                if (isBlocked) {
                    bgColor = new Color(80, 80, 80); 
                } else {
                    bgColor = (i + j) % 2 == 0 ?
                            new Color(245, 245, 245) : new Color(255, 255, 255);
                }

                g2d.setColor(bgColor);
                g2d.fillRect(j * cellSize, i * cellSize, cellSize, cellSize);
                if (isBlocked) {
                    drawBlockedCellPattern(g2d, i, j);
                }
            }
        }
    }

    private void drawBlockedCellPattern(Graphics2D g2d, int row, int col) {
        int x = col * cellSize;
        int y = row * cellSize;
        g2d.setColor(new Color(120, 120, 120));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(x + 10, y + 10, x + cellSize - 10, y + cellSize - 10);
        g2d.drawLine(x + cellSize - 10, y + 10, x + 10, y + cellSize - 10);
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(x, y, cellSize, cellSize);
        g2d.setFont(blockedFont);
        g2d.setColor(Color.WHITE);
        FontMetrics fm = g2d.getFontMetrics();
        String xMark = "✗";
        int textWidth = fm.stringWidth(xMark);
        g2d.drawString(xMark,
                x + (cellSize - textWidth) / 2,
                y + cellSize / 2 + fm.getAscent() / 2 - 5);
    }

    private void drawGridLines(Graphics2D g2d) {
        g2d.setColor(new Color(220, 220, 220));
        g2d.setStroke(new BasicStroke(1.5f));
        for (int j = 0; j <= grid.getCols(); j++) {
            g2d.drawLine(j * cellSize, 0, j * cellSize, grid.getRows() * cellSize);
        }
        for (int i = 0; i <= grid.getRows(); i++) {
            g2d.drawLine(0, i * cellSize, grid.getCols() * cellSize, i * cellSize);
        }
    }

    private void drawCellCoordinates(Graphics2D g2d) {
        g2d.setFont(coordFont);
        Set<String> blockedCells = grid.getBlockedCells();

        for (int i = 0; i < grid.getRows(); i++) {
            for (int j = 0; j < grid.getCols(); j++) {
                String cellKey = i + "," + j;
                boolean isBlocked = blockedCells.contains(cellKey);

                g2d.setColor(isBlocked ? new Color(200, 200, 200) : new Color(150, 150, 150));

                String coord = String.format("(%d,%d)", i, j);
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(coord);

                g2d.drawString(coord,
                        j * cellSize + (cellSize - textWidth) / 2,
                        i * cellSize + 15
                );
            }
        }
    }

    private void drawSetupPositions(Graphics2D g2d) {

        if (robot1StartX != -1 && robot1StartY != -1) {
            drawSetupMarker(g2d, robot1StartX, robot1StartY,
                    Color.RED, "R1 Start", false);
        }

        if (robot1TargetX != -1 && robot1TargetY != -1) {
            drawSetupMarker(g2d, robot1TargetX, robot1TargetY,
                    Color.RED, "R1 Target", true);
        }

        if (robot2StartX != -1 && robot2StartY != -1) {
            drawSetupMarker(g2d, robot2StartX, robot2StartY,
                    Color.BLUE, "R2 Start", false);
        }
        if (robot2TargetX != -1 && robot2TargetY != -1) {
            drawSetupMarker(g2d, robot2TargetX, robot2TargetY,
                    Color.BLUE, "R2 Target", true);
        }
    }

    private void drawSetupMarker(Graphics2D g2d, int row, int col,
                                 Color color, String label, boolean isTarget) {

        if (grid.isCellBlocked(row, col)) {
            return;
        }

        int centerX = col * cellSize + cellSize / 2;
        int centerY = row * cellSize + cellSize / 2;
        int markerSize = isTarget ? 25 : 20;

        if (isTarget) {
         
            Polygon diamond = new Polygon();
            diamond.addPoint(centerX, centerY - markerSize/2);
            diamond.addPoint(centerX + markerSize/2, centerY);
            diamond.addPoint(centerX, centerY + markerSize/2);
            diamond.addPoint(centerX - markerSize/2, centerY);
            g2d.setColor(color);
            g2d.fill(diamond);
            g2d.setColor(color.darker());
            g2d.draw(diamond);
        } else {
            Ellipse2D circle = new Ellipse2D.Double(
                    centerX - markerSize/2,
                    centerY - markerSize/2,
                    markerSize,
                    markerSize
            );

            g2d.setColor(color);
            g2d.fill(circle);
            g2d.setColor(color.darker());
            g2d.draw(circle);
        }

        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        g2d.setColor(Color.BLACK);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(label);
        g2d.drawString(label, centerX - textWidth/2, centerY - markerSize/2 - 5);
    }

    private void drawEditModeIndicator(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 200, 100));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.setColor(new Color(200, 100, 0));
        String modeText = eraseMode ?
                "OBSTACLE REMOVAL MODE: Click cells to remove obstacles" :
                "OBSTACLE PLACEMENT MODE: Click cells to add obstacles";
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(modeText);
        g2d.drawString(modeText,
                (getWidth() - textWidth) / 2,
                getHeight() - 30);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(Color.BLACK);
        String instruction = "Right-click to toggle mode | Press ESC or click 'Done' to exit";
        int instrWidth = g2d.getFontMetrics().stringWidth(instruction);
        g2d.drawString(instruction,
                (getWidth() - instrWidth) / 2,
                getHeight() - 10);
    }

    private void drawRobots(Graphics2D g2d) {
        for (int i = 0; i < grid.getRows(); i++) {
            for (int j = 0; j < grid.getCols(); j++) {
                RobotAgent robot = grid.getRobotAt(i, j);
                if (robot != null) {
                    drawRobot(g2d, robot, i, j);
                }
            }
        }
    }

    private void drawRobot(Graphics2D g2d, RobotAgent robot, int row, int col) {
        int centerX = col * cellSize + cellSize / 2;
        int centerY = row * cellSize + cellSize / 2;
        int robotSize = cellSize - 30;
        Ellipse2D robotBody = new Ellipse2D.Double(
                centerX - robotSize/2,
                centerY - robotSize/2,
                robotSize,
                robotSize
        );
        if (robot.isStatic() && !robot.isTemporarilyMoving()) {
            g2d.setColor(Color.GRAY);
            g2d.fill(robotBody);
            g2d.setColor(Color.DARK_GRAY);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawLine(centerX - robotSize/4, centerY - robotSize/4,
                    centerX + robotSize/4, centerY + robotSize/4);
            g2d.drawLine(centerX + robotSize/4, centerY - robotSize/4,
                    centerX - robotSize/4, centerY + robotSize/4);
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(robotBody);
        } else if (robot.isStatic() && robot.isTemporarilyMoving()) {
            g2d.setColor(new Color(128, 0, 128)); 
            g2d.fill(robotBody);
            g2d.setColor(new Color(200, 150, 255));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(centerX - robotSize/4, centerY,
                    centerX + robotSize/4, centerY);
            g2d.drawLine(centerX, centerY - robotSize/4,
                    centerX, centerY + robotSize/4);
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(robotBody);
        } else {
            GradientPaint gradient = new GradientPaint(
                    centerX - robotSize/2, centerY - robotSize/2,
                    robot.getColor(),
                    centerX + robotSize/2, centerY + robotSize/2,
                    robot.getColor().darker()
            );

            g2d.setPaint(gradient);
            g2d.fill(robotBody);
            if (robot.isInDeadlock()) {
            
                g2d.setColor(Color.RED);
                g2d.setStroke(new BasicStroke(3));
                g2d.draw(robotBody);
                g2d.setColor(new Color(255, 0, 0, 100));
                Ellipse2D glow = new Ellipse2D.Double(
                        centerX - robotSize/2 - 8,
                        centerY - robotSize/2 - 8,
                        robotSize + 16,
                        robotSize + 16
                );
                g2d.fill(glow);
                g2d.setFont(deadlockFont);
                g2d.setColor(Color.RED);
                String deadlockText = "DEADLOCK";
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(deadlockText);
                g2d.drawString(deadlockText, centerX - textWidth/2, centerY - robotSize/2 - 10);
            } else if (robot.isRequesting()) {
                g2d.setColor(Color.YELLOW);
                g2d.setStroke(new BasicStroke(3));
                g2d.draw(robotBody);
                g2d.setColor(new Color(255, 255, 200, 100));
                Ellipse2D glow = new Ellipse2D.Double(
                        centerX - robotSize/2 - 5,
                        centerY - robotSize/2 - 5,
                        robotSize + 10,
                        robotSize + 10
                );
                g2d.fill(glow);
            } else {
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(2));
                g2d.draw(robotBody);
            }
        }

        g2d.setStroke(new BasicStroke(1));
        g2d.setFont(robotFont);
        g2d.setColor(Color.WHITE);
        String id = robot.getRobotId();
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(id);
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.drawString(id, centerX - textWidth/2 + 1, centerY + 5 + 1);
        g2d.setColor(Color.WHITE);
        g2d.drawString(id, centerX - textWidth/2, centerY + 5);
        if (!robot.isStatic() || robot.isTemporarilyMoving()) {
            g2d.setFont(clockFont);
            String clockText = "C:" + robot.getLogicalClock();
            textWidth = g2d.getFontMetrics().stringWidth(clockText);
            g2d.setColor(Color.BLACK);
            g2d.drawString(clockText, centerX - textWidth/2, centerY + 25);
            if (robot.isStatic() && robot.isTemporarilyMoving()) {
                g2d.setFont(new Font("Arial", Font.BOLD, 12));
                String requestText = "Req:" + robot.getRequestCount();
                textWidth = g2d.getFontMetrics().stringWidth(requestText);
                g2d.setColor(new Color(0, 0, 0, 150));
                g2d.fillRect(centerX - textWidth/2 - 3, centerY + 35 - 12,
                        textWidth + 6, 16);
                g2d.setColor(Color.YELLOW);
                g2d.drawString(requestText, centerX - textWidth/2, centerY + 35);
            }
            if (!(robot.getCurrentX() == robot.getTargetX() &&
                    robot.getCurrentY() == robot.getTargetY())) {
                drawTargetIndicator(g2d, robot, centerX, centerY, robotSize);
            } else {
                g2d.setColor(Color.GREEN);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawLine(centerX - 5, centerY, centerX - 2, centerY + 5);
                g2d.drawLine(centerX - 2, centerY + 5, centerX + 5, centerY - 5);
            }
        } else {
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.setColor(Color.WHITE);
            String staticText = "STATIC";
            textWidth = g2d.getFontMetrics().stringWidth(staticText);
            g2d.drawString(staticText, centerX - textWidth/2, centerY + 25);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            String requestText = "Req:" + robot.getRequestCount();
            textWidth = g2d.getFontMetrics().stringWidth(requestText);
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(centerX - textWidth/2 - 3, centerY + 35 - 12,
                    textWidth + 6, 16);
            g2d.setColor(Color.YELLOW);
            g2d.drawString(requestText, centerX - textWidth/2, centerY + 35);
        }
    }
    private void drawTargetIndicator(Graphics2D g2d, RobotAgent robot, int centerX, int centerY, int robotSize) {
        int targetX = robot.getTargetX();
        int targetY = robot.getTargetY();
        int currentX = robot.getCurrentX();
        int currentY = robot.getCurrentY();
        double angle = Math.atan2(targetY - currentY, targetX - currentX);
        int arrowSize = robotSize / 3;
        g2d.setColor(new Color(0, 150, 0, 200));
        g2d.setStroke(new BasicStroke(2));
        int endX = centerX + (int)(Math.cos(angle) * arrowSize);
        int endY = centerY + (int)(Math.sin(angle) * arrowSize);
        g2d.drawLine(centerX, centerY, endX, endY);
        Polygon arrowHead = new Polygon();
        arrowHead.addPoint(endX, endY);
        arrowHead.addPoint(
                endX - (int)(Math.cos(angle + Math.PI/6) * 8),
                endY - (int)(Math.sin(angle + Math.PI/6) * 8)
        );
        arrowHead.addPoint(
                endX - (int)(Math.cos(angle - Math.PI/6) * 8),
                endY - (int)(Math.sin(angle - Math.PI/6) * 8)
        );
        g2d.fill(arrowHead);
    }

    private void drawLegend(Graphics2D g2d) {
        int legendY = getHeight() - 140;
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("Legend:", 10, legendY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        g2d.setColor(Color.RED);
        g2d.fillOval(70, legendY - 15, 15, 15);
        g2d.setColor(Color.BLACK);
        g2d.drawString("= Robot1", 90, legendY - 3);
        g2d.setColor(Color.BLUE);
        g2d.fillOval(160, legendY - 15, 15, 15);
        g2d.setColor(Color.BLACK);
        g2d.drawString("= Robot2", 180, legendY - 3);
        g2d.setColor(Color.GRAY);
        g2d.fillOval(250, legendY - 15, 15, 15);
        g2d.setColor(Color.BLACK);
        g2d.drawString("= Static", 270, legendY - 3);
        g2d.setColor(new Color(128, 0, 128));
        g2d.fillOval(340, legendY - 15, 15, 15);
        g2d.setColor(Color.BLACK);
        g2d.drawString("= Adaptive", 360, legendY - 3);
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(70, legendY + 15, 15, 15);
        g2d.setColor(Color.BLACK);
        g2d.drawString("= Obstacle", 90, legendY + 27);
        g2d.setColor(Color.YELLOW);
        g2d.drawOval(180, legendY + 15, 15, 15);
        g2d.setColor(Color.BLACK);
        g2d.drawString("= Requesting", 200, legendY + 27);
        g2d.setColor(Color.RED);
        g2d.drawOval(310, legendY + 15, 15, 15);
        g2d.setColor(Color.BLACK);
        g2d.drawString("= Deadlock", 330, legendY + 27);
        g2d.setColor(Color.RED);
        g2d.fillOval(70, legendY + 45, 15, 15);
        g2d.setColor(Color.BLACK);
        g2d.drawString("= Start", 90, legendY + 57);
        int[] xPoints = {155, 165, 160, 155};
        int[] yPoints = {legendY + 53, legendY + 53, legendY + 43, legendY + 53};
        g2d.setColor(Color.BLUE);
        g2d.fillPolygon(xPoints, yPoints, 4);
        g2d.setColor(Color.BLACK);
        g2d.drawString("= Target", 175, legendY + 57);
        g2d.setColor(new Color(0, 150, 0));
        g2d.drawLine(250, legendY + 50, 265, legendY + 50);
        Polygon arrow = new Polygon();
        arrow.addPoint(265, legendY + 50);
        arrow.addPoint(260, legendY + 45);
        arrow.addPoint(260, legendY + 55);
        g2d.fill(arrow);
        g2d.setColor(Color.BLACK);
        g2d.drawString("= Direction", 275, legendY + 57);
        if (blockEditMode) {
            g2d.setColor(new Color(255, 200, 0, 200));
            g2d.fillRect(350, legendY + 35, 15, 15);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(350, legendY + 35, 15, 15);
            g2d.drawString("= Edit Mode", 370, legendY + 57);
        }
    }

    private void drawTitle(Graphics2D g2d) {
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.setColor(new Color(33, 33, 33));
        String title = "JADE Multi-Robot Coordination with Adaptive Static Robots";
        FontMetrics fm = g2d.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        g2d.drawString(title, (getWidth() - titleWidth) / 2, 25);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(new Color(100, 100, 100));
        String subtitle = "Static robots move after receiving requests to open path";
        int subtitleWidth = g2d.getFontMetrics().stringWidth(subtitle);
        g2d.drawString(subtitle, (getWidth() - subtitleWidth) / 2, 45);
    }

}
