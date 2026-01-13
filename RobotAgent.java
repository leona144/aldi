import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class RobotAgent extends Agent {
    private String robotId;
    private int currentX, currentY, targetX, targetY;
    private Color color;
    private boolean isMoving = true;
    private boolean isStatic = false; // New: for blocking robots
    private int requestsNeededToMove = 3; // Number of requests needed for static robots to move
    private int requestCount = 0; // Count of received requests
    private int originalX, originalY; // Original position for static robots to return to
    private boolean temporarilyMoving = false; // Whether static robot is temporarily moving
    private int tempTargetX, tempTargetY; // Temporary target for static robot movement

    // Ricart-Agrawala variables
    private int logicalClock = 0;
    private boolean requesting = false;
    private int requestedCellX = -1, requestedCellY = -1;
    private int requestTimestamp = 0;
    private Map<String, Boolean> okReceived = new ConcurrentHashMap<>();
    private Queue<ACLMessage> pendingRequests = new ConcurrentLinkedQueue<>();

    // Movement control
    private boolean hasPendingRequest = false;
    private boolean canMoveThisCycle = true;
    private final Object movementLock = new Object();
    private long lastMoveTime = 0;
    private static final long MOVE_INTERVAL = 800; // Reduced from 1000 for faster testing

    // Deadlock detection
    private int consecutiveBlocks = 0;
    private static final int MAX_BLOCKS_BEFORE_DEADLOCK = 3;
    private boolean inDeadlock = false;
    private long deadlockStartTime = 0;
    private static final long DEADLOCK_TIMEOUT = 5000; // 5 seconds

    // Retry mechanism
    private int retryCount = 0;
    private static final int MAX_RETRIES = 10;
    private long lastRequestTime = 0;
    private static final long RETRY_INTERVAL = 500; // Retry every 500ms

    // Shared resources
    private Grid grid;
    private MainFrame mainFrame;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length >= 10) {
            this.robotId = (String) args[0];
            this.currentX = (Integer) args[1];
            this.currentY = (Integer) args[2];
            this.targetX = (Integer) args[3];
            this.targetY = (Integer) args[4];
            this.isStatic = (Boolean) args[5]; // New: static parameter
            this.requestsNeededToMove = (Integer) args[6]; // Number of requests needed
            this.originalX = (Integer) args[7]; // Original X position
            this.originalY = (Integer) args[8]; // Original Y position
            this.grid = (Grid) args[9];
            this.mainFrame = (MainFrame) args[10];
            this.color = isStatic ? Color.GRAY : (robotId.equals("Robot1") ? Color.RED : Color.BLUE);
        }

        // Initial occupation
        synchronized(grid) {
            grid.occupyCell(currentX, currentY, this);
            if (mainFrame != null) {
                mainFrame.updateGrid();
            }
        }

        if (isStatic) {
            log("ADAPTIVE STATIC AGENT at (" + currentX + "," + currentY + ") - Needs " +
                    requestsNeededToMove + " requests to move");
            log("Original position: (" + originalX + "," + originalY + ")");

            // Add behaviors for static robots
            addBehaviour(new MessageHandlingBehaviour());
            addBehaviour(new AdaptiveStaticBehaviour());
        } else {
            log("Agent started at (" + currentX + "," + currentY + ") → Target: (" + targetX + "," + targetY + ")");

            // Add behaviours only for moving robots
            addBehaviour(new MessageHandlingBehaviour());
            addBehaviour(new MovementBehaviour());
            addBehaviour(new DeadlockDetectionBehaviour());
            addBehaviour(new RetryBehaviour()); // New: retry behaviour
        }
    }

    private class RetryBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            //Only active for moving robots that haven't reached target
            if (isStatic || hasReachedTarget() || !isMoving) {
                block(500);
                return;
            }

            // Check if we should retry a request
            long currentTime = System.currentTimeMillis();
            if (requesting && hasPendingRequest &&
                    (currentTime - lastRequestTime > RETRY_INTERVAL) &&
                    retryCount < MAX_RETRIES) {

                log("Retrying request for cell (" + requestedCellX + "," + requestedCellY + ") - Attempt #" + (retryCount + 1));
                retryCount++;
                lastRequestTime = currentTime;

                // Resend the request
                synchronized(grid) {
                    RobotAgent occupant = grid.getRobotAt(requestedCellX, requestedCellY);
                    if (occupant != null) {
                        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                        request.addReceiver(occupant.getAID());
                        request.setContent("REQUEST:" + requestedCellX + ":" + requestedCellY + ":" + logicalClock);
                        request.setSender(getAID());
                        send(request);
                        log("Resent request to " + occupant.getRobotId());
                    }
                }
            }

            block(300);
        }
    }

    private class AdaptiveStaticBehaviour extends CyclicBehaviour {
        private int moveState = 0; // 0=waiting, 1=moving away, 2=waiting at temp, 3=returning
        private long waitStartTime = 0;
        private static final long WAIT_TIME = 3000; // Wait 3 seconds at temp position

        @Override
        public void action() {
            if (!isStatic) {
                block(300);
                return;
            }

            // Check if we should start moving
            //start moving when enough requests received
            if (requestCount >= requestsNeededToMove && moveState == 0 && !temporarilyMoving) {
                log("🎯 RECEIVED " + requestCount + " REQUESTS! Starting temporary movement to open path.");
                temporarilyMoving = true;
                moveState = 1; // Start moving away
                requestCount = 0; // Reset counter

                // Find a temporary position to move to
                int[] tempPos = findTemporaryPosition();
                if (tempPos != null) {
                    tempTargetX = tempPos[0];
                    tempTargetY = tempPos[1];
                    log("Will move temporarily to (" + tempTargetX + "," + tempTargetY + ")");
                } else {
                    // Default position
                    if (originalX == 2 && originalY == 2) {
                        tempTargetX = 1;
                        tempTargetY = 1;
                    } else if (originalX == 2 && originalY == 1) {
                        tempTargetX = 1;
                        tempTargetY = 0;
                    } else if (originalX == 2 && originalY == 3) {
                        tempTargetX = 3;
                        tempTargetY = 4;
                    } else {
                        tempTargetX = (originalX > 0) ? originalX - 1 : originalX + 1;
                        tempTargetY = (originalY > 0) ? originalY - 1 : originalY + 1;
                    }
                    log("Using default temporary position (" + tempTargetX + "," + tempTargetY + ")");
                }

                // Change color to purple to indicate adaptive mode
                color = new Color(128, 0, 128);
                if (mainFrame != null) {
                    mainFrame.updateGrid();
                }
            }

            // Handle movement states
            switch (moveState) {
                case 1: // Moving to temporary position
                    if (currentX == tempTargetX && currentY == tempTargetY) {
                        log("✅ Reached temporary position (" + tempTargetX + "," + tempTargetY + "). Waiting for main robots...");
                        moveState = 2; // Start waiting
                        waitStartTime = System.currentTimeMillis();
                    } else {
                        moveTowardTarget(tempTargetX, tempTargetY);
                    }
                    break;

                case 2: // Waiting at temporary position
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - waitStartTime >= WAIT_TIME) {
                        log("⏰ Wait time completed. Returning to original position...");
                        moveState = 3; // Start returning
                        tempTargetX = originalX;
                        tempTargetY = originalY;
                    }
                    break;

                case 3: // Returning to original position
                    if (currentX == originalX && currentY == originalY) {
                        log("✅ Returned to original position. Resetting to static mode.");
                        temporarilyMoving = false;
                        moveState = 0;
                        color = Color.GRAY; // Return to gray color
                        if (mainFrame != null) {
                            mainFrame.updateGrid();
                        }
                    } else {
                        moveTowardTarget(tempTargetX, tempTargetY);
                    }
                    break;
            }

            block(300);
        }
    }

    private void moveTowardTarget(int targetX, int targetY) {
        // Calculate direction
        int dx = targetX - currentX;
        int dy = targetY - currentY;

        int nextX = currentX;
        int nextY = currentY;

        // Prioritize larger difference
        if (Math.abs(dx) > Math.abs(dy)) {
            if (dx > 0) nextX = currentX + 1;
            else if (dx < 0) nextX = currentX - 1;
        } else {
            if (dy > 0) nextY = currentY + 1;
            else if (dy < 0) nextY = currentY - 1;
        }

        // Validate and move
        if (nextX >= 0 && nextX < 5 && nextY >= 0 && nextY < 5) {
            synchronized(grid) {
                // Check if cell is free
                if (grid.isCellFree(nextX, nextY) && !grid.isCellBlocked(nextX, nextY)) {
                    grid.freeCell(currentX, currentY);
                    int oldX = currentX, oldY = currentY;
                    currentX = nextX;
                    currentY = nextY;
                    grid.occupyCell(currentX, currentY, this);

                    log("↪️ Moved from (" + oldX + "," + oldY + ") to (" + currentX + "," + currentY + ")");

                    if (mainFrame != null) {
                        mainFrame.updateGrid();
                    }
                } else {
                    // Cell not free, try alternative
                    log("⏸️ Cell (" + nextX + "," + nextY + ") not free. Trying alternative...");
                    tryAlternativeMove(targetX, targetY);
                }
            }
        }
    }

    private void tryAlternativeMove(int targetX, int targetY) {
        int[][] alternatives = {
                {currentX - 1, currentY}, // up
                {currentX + 1, currentY}, // down
                {currentX, currentY - 1}, // left
                {currentX, currentY + 1}  // right
        };

        for (int[] alt : alternatives) {
            int x = alt[0];
            int y = alt[1];

            if (x >= 0 && x < 5 && y >= 0 && y < 5) {
                synchronized(grid) {
                    if (grid.isCellFree(x, y) && !grid.isCellBlocked(x, y)) {
                        grid.freeCell(currentX, currentY);
                        int oldX = currentX, oldY = currentY;
                        currentX = x;
                        currentY = y;
                        grid.occupyCell(currentX, currentY, this);

                        log("↪️ Alternative move from (" + oldX + "," + oldY + ") to (" + currentX + "," + currentY + ")");

                        if (mainFrame != null) {
                            mainFrame.updateGrid();
                        }
                        return;
                    }
                }
            }
        }
    }

    private int[] findTemporaryPosition() {
        // Find a position away from the current position that is free
        int[][] possiblePositions = {
                {currentX - 1, currentY}, // up
                {currentX + 1, currentY}, // down
                {currentX, currentY - 1}, // left
                {currentX, currentY + 1}, // right
                {currentX - 1, currentY - 1}, // up-left
                {currentX - 1, currentY + 1}, // up-right
                {currentX + 1, currentY - 1}, // down-left
                {currentX + 1, currentY + 1}  // down-right
        };

        // Try positions that are away from the path first
        for (int[] pos : possiblePositions) {
            int x = pos[0];
            int y = pos[1];

            if (x >= 0 && x < 5 && y >= 0 && y < 5) {
                // Check if cell is free and not blocked
                if (grid.isCellFree(x, y) && !grid.isCellBlocked(x, y)) {
                    // Also check if it's not too close to other static robots
                    boolean tooClose = false;
                    for (int i = 0; i < 5; i++) {
                        for (int j = 0; j < 5; j++) {
                            RobotAgent other = grid.getRobotAt(i, j);
                            if (other != null && other.isStatic() && other != this) {
                                int distance = Math.abs(x - i) + Math.abs(y - j);
                                if (distance <= 1) {
                                    tooClose = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (!tooClose) {
                        return pos;
                    }
                }
            }
        }

        // If no good position found, try any free position
        for (int[] pos : possiblePositions) {
            int x = pos[0];
            int y = pos[1];

            if (x >= 0 && x < 5 && y >= 0 && y < 5) {
                if (grid.isCellFree(x, y) && !grid.isCellBlocked(x, y)) {
                    return pos;
                }
            }
        }
        return null;
    }

    private class MovementBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            if (!isMoving || hasReachedTarget() || isStatic) {
                block(500);
                return;
            }

            if (inDeadlock) {
                // Try to resolve deadlock
                attemptDeadlockResolution();
                block(800);
                return;
            }

            synchronized(movementLock) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastMoveTime >= MOVE_INTERVAL) {
                    canMoveThisCycle = true;

                    if (!requesting && !hasPendingRequest && canMoveThisCycle) {
                        requestNextCell();
                        canMoveThisCycle = false;
                        lastMoveTime = currentTime;
                    }
                }
            }

            block(100);
        }
    }

    private class DeadlockDetectionBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            if (isStatic || hasReachedTarget() || !isMoving) {
                block(1000);
                return;
            }

            // Check for deadlock timeout
            if (inDeadlock && System.currentTimeMillis() - deadlockStartTime > DEADLOCK_TIMEOUT) {
                log("⚠️ DEADLOCK TIMEOUT - Concluding path is impossible");
                isMoving = false;
                if (mainFrame != null) {
                    mainFrame.logMessage("[DEADLOCK] " + robotId + " cannot reach target - path blocked");
                }
            }

            block(500);
        }
    }

    private class MessageHandlingBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            // Check for REQUEST messages
            ACLMessage requestMsg = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
            if (requestMsg != null) {
                handleRequestMessage(requestMsg);
            }

            // Check for OK messages
            ACLMessage okMsg = receive(MessageTemplate.MatchPerformative(ACLMessage.AGREE));
            if (okMsg != null) {
                handleOkMessage(okMsg);
            }

            // Check for RELEASE messages
            ACLMessage informMsg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            if (informMsg != null && informMsg.getContent() != null &&
                    informMsg.getContent().startsWith("RELEASE")) {
                handleReleaseMessage(informMsg);
            }

            // Check for DEADLOCK messages
            ACLMessage deadlockMsg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            if (deadlockMsg != null && deadlockMsg.getContent() != null &&
                    deadlockMsg.getContent().startsWith("DEADLOCK")) {
                handleDeadlockMessage(deadlockMsg);
            }

            block(50);
        }
    }

    private void handleRequestMessage(ACLMessage msg) {
        String content = msg.getContent();
        String[] parts = content.split(":");
        int cellX = Integer.parseInt(parts[1]);
        int cellY = Integer.parseInt(parts[2]);
        int timestamp = Integer.parseInt(parts[3]);

        updateClock(timestamp);

        // Increment request count for static robots
        if (isStatic) {
            requestCount++;
            log("📨 Received request #" + requestCount + " from " + msg.getSender().getLocalName() +
                    " for cell (" + cellX + "," + cellY + ")");

            // If we're temporarily moving, grant access immediately
            if (temporarilyMoving) {
                ACLMessage ok = msg.createReply();
                ok.setPerformative(ACLMessage.AGREE);
                ok.setContent("OK:" + logicalClock);
                send(ok);
                log("✅ Granted access to " + msg.getSender().getLocalName() + " (temporarily moving)");

                // Reset request count since we're granting access
                requestCount = 0;
                return;
            }
        }

        // Static robots check request count before deciding
        if (isStatic && !temporarilyMoving) {
            // Deny access but count the request
            log("❌ Denying access (static). Request count: " + requestCount + "/" + requestsNeededToMove);
            ACLMessage deny = msg.createReply();
            deny.setPerformative(ACLMessage.REFUSE);
            deny.setContent("STATIC_BLOCKED:" + logicalClock + ":" + requestCount);
            send(deny);
            return;
        }

        boolean shouldGrant = false;

        if (!requesting) {
            shouldGrant = true;
        } else {
            // Ricart-Agrawala algorithm
            if (requestTimestamp > timestamp) {
                shouldGrant = true;
            } else if (requestTimestamp == timestamp) {
                if (robotId.compareTo(msg.getSender().getLocalName()) > 0) {
                    shouldGrant = true;
                } else {
                    pendingRequests.add(msg);
                    log("📥 Queued request from " + msg.getSender().getLocalName());
                    return;
                }
            } else {
                pendingRequests.add(msg);
                log("📥 Queued request from " + msg.getSender().getLocalName());
                return;
            }
        }

        if (shouldGrant) {
            ACLMessage ok = msg.createReply();
            ok.setPerformative(ACLMessage.AGREE);
            ok.setContent("OK:" + logicalClock);
            send(ok);
            log("✅ Granted access to " + msg.getSender().getLocalName());
        }
    }

    private void handleOkMessage(ACLMessage msg) {
        String senderId = msg.getSender().getLocalName();
        String content = msg.getContent();

        if (content.startsWith("STATIC_BLOCKED")) {
            String[] parts = content.split(":");
            int requestCount = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;
            log("⏸️ Blocked by static robot " + senderId + " (request count: " + requestCount + "/" + requestsNeededToMove + ")");
            consecutiveBlocks++;
            checkForDeadlock();

            // Reset retry count when blocked
            retryCount = 0;
            lastRequestTime = System.currentTimeMillis();
            return;
        }

        okReceived.put(senderId, true);
        log("✅ Received OK from " + senderId);

        // Reset block counter on successful move
        consecutiveBlocks = 0;
        retryCount = 0;

        // If we have OK from the other robot and we're requesting, move
        if (okReceived.size() == 1 && requesting) {
            new Thread(() -> {
                try {
                    Thread.sleep(50);
                    moveToRequestedCell();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void handleDeadlockMessage(ACLMessage msg) {
        String senderId = msg.getSender().getLocalName();
        log("⚠️ Received DEADLOCK alert from " + senderId);

        if (!inDeadlock) {
            inDeadlock = true;
            deadlockStartTime = System.currentTimeMillis();
            log("⚠️ ENTERING DEADLOCK RESOLUTION MODE");
            if (mainFrame != null) {
                mainFrame.logMessage("[DEADLOCK] " + robotId + " entering deadlock resolution");
            }
        }
    }

    private void handleReleaseMessage(ACLMessage msg) {
        String content = msg.getContent();
        String[] parts = content.split(":");

        log("📤 Received RELEASE from " + msg.getSender().getLocalName());

        // Process pending requests
        if (!pendingRequests.isEmpty()) {
            ACLMessage pendingMsg = pendingRequests.poll();
            ACLMessage ok = pendingMsg.createReply();
            ok.setPerformative(ACLMessage.AGREE);
            ok.setContent("OK:" + logicalClock);
            send(ok);
            log("✅ Granted pending request");
        }
    }

    private void requestNextCell() {
        if (hasReachedTarget()) {
            log("🎯 TARGET REACHED!");
            isMoving = false;
            return;
        }

        if (requesting || hasPendingRequest || inDeadlock) {
            return;
        }

        int[] nextCell = calculateNextMove();
        if (nextCell[0] == currentX && nextCell[1] == currentY) {
            log("⏸️ Cannot move - staying in same cell");

            // If stuck, try alternate path
            if (consecutiveBlocks > 0) {
                log("🔄 Trying alternate path due to being stuck...");
                int[] alternate = findAlternatePathWhenStuck();
                if (alternate != null) {
                    log("🔄 Found alternate path to (" + alternate[0] + "," + alternate[1] + ")");
                    requestAlternateCell(alternate[0], alternate[1]);
                }
            }
            return;
        }

        log("📝 Planning to move to (" + nextCell[0] + "," + nextCell[1] + ")");

        synchronized(grid) {
            // Check if cell is adjacent
            if (!isAdjacentCell(currentX, currentY, nextCell[0], nextCell[1])) {
                log("⚠️ ERROR: Can only move to adjacent cells! Trying alternate...");
                int[] adjacent = findAdjacentMoveTowardTarget();
                if (adjacent != null) {
                    nextCell = adjacent;
                    log("🔄 Found adjacent move to (" + nextCell[0] + "," + nextCell[1] + ")");
                } else {
                    log("❌ No valid adjacent move found!");
                    return;
                }
            }

            // Check if cell is BLOCKED
            if (grid.isCellBlocked(nextCell[0], nextCell[1])) {
                log("⛔ Cell (" + nextCell[0] + "," + nextCell[1] + ") is BLOCKED (obstacle)");
                consecutiveBlocks++;
                checkForDeadlock();

                // Try alternate move immediately
                int[] alternate = findAlternatePathWhenStuck();
                if (alternate != null) {
                    log("🔄 Trying alternate move to (" + alternate[0] + "," + alternate[1] + ")");
                    requestAlternateCell(alternate[0], alternate[1]);
                }
                return;
            }

            if (grid.isCellFree(nextCell[0], nextCell[1])) {
                // Cell is free, move immediately
                moveToCell(nextCell[0], nextCell[1]);
            } else {
                // Cell occupied by another robot, need to request access
                RobotAgent occupant = grid.getRobotAt(nextCell[0], nextCell[1]);
                if (occupant != null) {
                    requesting = true;
                    hasPendingRequest = true;
                    requestedCellX = nextCell[0];
                    requestedCellY = nextCell[1];
                    requestTimestamp = ++logicalClock;
                    okReceived.clear();
                    retryCount = 0;
                    lastRequestTime = System.currentTimeMillis();

                    ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                    request.addReceiver(occupant.getAID());
                    request.setContent("REQUEST:" + nextCell[0] + ":" + nextCell[1] + ":" + logicalClock);
                    request.setSender(getAID());
                    send(request);

                    log("📨 Requesting cell (" + nextCell[0] + "," + nextCell[1] + ") from " + occupant.getRobotId());
                }
            }
        }
    }

    private int[] findAlternatePathWhenStuck() {
        // When stuck, try moving in any direction that might help
        int[][] possibleMoves = {
                {currentX - 1, currentY}, // up
                {currentX + 1, currentY}, // down
                {currentX, currentY - 1}, // left
                {currentX, currentY + 1}  // right
        };

        // First try moves that get us closer to target
        List<int[]> goodMoves = new ArrayList<>();
        List<int[]> neutralMoves = new ArrayList<>();

        for (int[] move : possibleMoves) {
            int newX = move[0];
            int newY = move[1];

            if (newX >= 0 && newX < 5 && newY >= 0 && newY < 5) {
                // Check if cell is not blocked and is free
                if (!grid.isCellBlocked(newX, newY) && grid.isCellFree(newX, newY)) {
                    int currentDist = Math.abs(currentX - targetX) + Math.abs(currentY - targetY);
                    int newDist = Math.abs(newX - targetX) + Math.abs(newY - targetY);

                    if (newDist < currentDist) {
                        goodMoves.add(move);
                    } else {
                        neutralMoves.add(move);
                    }
                }
            }
        }

        if (!goodMoves.isEmpty()) {
            return goodMoves.get(0);
        }

        if (!neutralMoves.isEmpty()) {
            return neutralMoves.get(0);
        }

        return null;
    }

    private void requestAlternateCell(int x, int y) {
        // Check if cell is adjacent
        if (!isAdjacentCell(currentX, currentY, x, y)) {
            log("⚠️ ERROR: Alternate cell is not adjacent!");
            return;
        }

        // Check if cell is blocked
        if (grid.isCellBlocked(x, y)) {
            log("⛔ Alternate cell (" + x + "," + y + ") is also BLOCKED");
            consecutiveBlocks++;
            checkForDeadlock();
            return;
        }

        // Check if cell is free
        if (grid.isCellFree(x, y)) {
            moveToCell(x, y);
        } else {
            // Cell occupied by another robot
            RobotAgent occupant = grid.getRobotAt(x, y);
            if (occupant != null) {
                requesting = true;
                hasPendingRequest = true;
                requestedCellX = x;
                requestedCellY = y;
                requestTimestamp = ++logicalClock;
                okReceived.clear();
                retryCount = 0;
                lastRequestTime = System.currentTimeMillis();

                ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                request.addReceiver(occupant.getAID());
                request.setContent("REQUEST:" + x + ":" + y + ":" + logicalClock);
                request.setSender(getAID());
                send(request);

                log("📨 Requesting alternate cell (" + x + "," + y + ") from " + occupant.getRobotId());
            }
        }
    }

    private void checkForDeadlock() {
        if (consecutiveBlocks >= MAX_BLOCKS_BEFORE_DEADLOCK && !inDeadlock) {
            inDeadlock = true;
            deadlockStartTime = System.currentTimeMillis();
            log("⚠️ DEADLOCK DETECTED! Blocked " + consecutiveBlocks + " times");
            if (mainFrame != null) {
                mainFrame.logMessage("[DEADLOCK] " + robotId + " detected deadlock");
            }

            // Alert other robot
            broadcastDeadlockAlert();
        }
    }

    private void broadcastDeadlockAlert() {
        ACLMessage deadlockAlert = new ACLMessage(ACLMessage.INFORM);
        String otherRobotName = robotId.equals("Robot1") ? "Robot2" : "Robot1";
        deadlockAlert.addReceiver(new jade.core.AID(otherRobotName, jade.core.AID.ISLOCALNAME));
        deadlockAlert.setContent("DEADLOCK:" + currentX + ":" + currentY + ":" + logicalClock);
        deadlockAlert.setSender(getAID());
        send(deadlockAlert);
        log("📤 Broadcasted DEADLOCK alert to " + otherRobotName);
    }

    private void attemptDeadlockResolution() {
        log("🔄 Attempting deadlock resolution...");

        // Strategy 1: Try long alternate path
        int[] longAlternate = findLongAlternatePath();
        if (longAlternate != null) {
            log("🔄 Found long alternate path to (" + longAlternate[0] + "," + longAlternate[1] + ")");
            requestAlternateCell(longAlternate[0], longAlternate[1]);
            inDeadlock = false;
            consecutiveBlocks = 0;
            return;
        }

        // Strategy 2: Try opposite direction temporarily
        int[] opposite = findOppositeMove();
        if (opposite != null) {
            log("🔄 Trying opposite direction to (" + opposite[0] + "," + opposite[1] + ")");
            requestAlternateCell(opposite[0], opposite[1]);
            return;
        }

        // Strategy 3: Wait and retry
        log("⏸️ Waiting for path to clear...");
    }

    private int[] findLongAlternatePath() {
        // Try to find a path that goes around the blockage
        int[][] possibleMoves = {
                {currentX - 1, currentY}, // up
                {currentX + 1, currentY}, // down
                {currentX, currentY - 1}, // left
                {currentX, currentY + 1}  // right
        };

        // Try moves that are far from target but might unblock
        for (int[] move : possibleMoves) {
            int newX = move[0];
            int newY = move[1];

            if (newX >= 0 && newX < 5 && newY >= 0 && newY < 5) {
                // Check if cell is not blocked and is free
                if (!grid.isCellBlocked(newX, newY) && grid.isCellFree(newX, newY)) {
                    // Check if this move might lead somewhere
                    int distanceFromTarget = Math.abs(newX - targetX) + Math.abs(newY - targetY);
                    int currentDistance = Math.abs(currentX - targetX) + Math.abs(currentY - targetY);

                    // Even if it takes us further, it might unblock us
                    if (distanceFromTarget <= currentDistance + 2) { // Allow some detour
                        return move;
                    }
                }
            }
        }
        return null;
    }

    private int[] findOppositeMove() {
        // Move opposite to target direction to create space
        int dx = targetX - currentX;
        int dy = targetY - currentY;

        int oppositeX = currentX;
        int oppositeY = currentY;

        if (dx > 0) oppositeX = currentX - 1; // Move opposite of target
        else if (dx < 0) oppositeX = currentX + 1;

        if (dy > 0) oppositeY = currentY - 1;
        else if (dy < 0) oppositeY = currentY + 1;

        // Validate move
        if (oppositeX >= 0 && oppositeX < 5 && oppositeY >= 0 && oppositeY < 5) {
            // Check if cell is not blocked and is free
            if (!grid.isCellBlocked(oppositeX, oppositeY) && grid.isCellFree(oppositeX, oppositeY)) {
                return new int[]{oppositeX, oppositeY};
            }
        }
        return null;
    }

    private boolean isAdjacentCell(int fromX, int fromY, int toX, int toY) {
        int dx = Math.abs(fromX - toX);
        int dy = Math.abs(fromY - toY);
        return (dx == 1 && dy == 0) || (dx == 0 && dy == 1);
    }

    private int[] findAdjacentMoveTowardTarget() {
        int currentX = getCurrentX();
        int currentY = getCurrentY();
        int targetX = getTargetX();
        int targetY = getTargetY();

        int[][] adjacentCells = {
                {currentX - 1, currentY}, // up
                {currentX + 1, currentY}, // down
                {currentX, currentY - 1}, // left
                {currentX, currentY + 1}  // right
        };

        List<int[]> goodMoves = new ArrayList<>();
        List<int[]> neutralMoves = new ArrayList<>();

        for (int[] cell : adjacentCells) {
            int newX = cell[0];
            int newY = cell[1];

            if (newX < 0 || newX >= 5 || newY < 0 || newY >= 5) {
                continue;
            }

            // Check if cell is blocked
            if (grid.isCellBlocked(newX, newY)) {
                continue;
            }

            // Check if cell is free
            if (!grid.isCellFree(newX, newY)) {
                continue;
            }

            int currentDistance = Math.abs(currentX - targetX) + Math.abs(currentY - targetY);
            int newDistance = Math.abs(newX - targetX) + Math.abs(newY - targetY);

            if (newDistance < currentDistance) {
                goodMoves.add(cell);
            } else {
                neutralMoves.add(cell);
            }
        }

        if (!goodMoves.isEmpty()) {
            return goodMoves.get(0);
        }

        if (!neutralMoves.isEmpty()) {
            return neutralMoves.get(0);
        }

        return null;
    }

    private void moveToRequestedCell() {
        if (!requesting) return;

        synchronized(grid) {
            if (!isAdjacentCell(currentX, currentY, requestedCellX, requestedCellY)) {
                log("⚠️ ERROR: Requested cell is not adjacent! Canceling move.");
                requesting = false;
                hasPendingRequest = false;
                okReceived.clear();
                retryCount = 0;
                return;
            }

            // Check if cell is blocked
            if (grid.isCellBlocked(requestedCellX, requestedCellY)) {
                log("⛔ Requested cell (" + requestedCellX + "," + requestedCellY + ") is BLOCKED");
                requesting = false;
                hasPendingRequest = false;
                okReceived.clear();
                retryCount = 0;
                consecutiveBlocks++;
                checkForDeadlock();
                return;
            }

            if (!grid.isCellFree(requestedCellX, requestedCellY)) {
                log("❌ Cell (" + requestedCellX + "," + requestedCellY + ") is now occupied");
                requesting = false;
                hasPendingRequest = false;
                okReceived.clear();
                retryCount = 0;

                // Try adjacent move instead
                int[] alternate = findAdjacentMoveTowardTarget();
                if (alternate != null) {
                    log("🔄 Trying adjacent move to (" + alternate[0] + "," + alternate[1] + ")");
                    moveToCell(alternate[0], alternate[1]);
                }
                return;
            }

            grid.freeCell(currentX, currentY);
            int oldX = currentX, oldY = currentY;
            currentX = requestedCellX;
            currentY = requestedCellY;
            grid.occupyCell(currentX, currentY, this);

            log("✅ MOVED from (" + oldX + "," + oldY + ") to (" + currentX + "," + currentY + ")");

            if (mainFrame != null) {
                mainFrame.updateGrid();
            }
        }

        sendRelease();
        requesting = false;
        hasPendingRequest = false;
        requestedCellX = -1;
        requestedCellY = -1;
        okReceived.clear();
        retryCount = 0;

        if (hasReachedTarget()) {
            log("🎯 TARGET REACHED!");
            isMoving = false;
        }
    }

    private void moveToCell(int x, int y) {
        // Verify this is an adjacent move
        if (!isAdjacentCell(currentX, currentY, x, y)) {
            log("⚠️ ERROR: Cannot move to non-adjacent cell (" + x + "," + y + ")");
            return;
        }

        synchronized(grid) {
            // Check if cell is blocked
            if (grid.isCellBlocked(x, y)) {
                log("⛔ Cell (" + x + "," + y + ") is BLOCKED (obstacle)");
                consecutiveBlocks++;
                checkForDeadlock();
                return;
            }

            // Check if cell is free
            if (!grid.isCellFree(x, y)) {
                log("❌ Cell (" + x + "," + y + ") is occupied");
                return;
            }

            grid.freeCell(currentX, currentY);
            int oldX = currentX, oldY = currentY;
            currentX = x;
            currentY = y;
            grid.occupyCell(currentX, currentY, this);

            log("✅ MOVED from (" + oldX + "," + oldY + ") to (" + currentX + "," + currentY + ")");

            if (mainFrame != null) {
                mainFrame.updateGrid();
            }
        }

        if (hasReachedTarget()) {
            log("🎯 TARGET REACHED!");
            isMoving = false;
        }
    }

    private void sendRelease() {
        ACLMessage release = new ACLMessage(ACLMessage.INFORM);
        String otherRobotName = robotId.equals("Robot1") ? "Robot2" : "Robot1";
        release.addReceiver(new jade.core.AID(otherRobotName, jade.core.AID.ISLOCALNAME));
        release.setContent("RELEASE:" + currentX + ":" + currentY + ":" + logicalClock);
        release.setSender(getAID());
        send(release);

        log("📤 Sent RELEASE for (" + currentX + "," + currentY + ")");
    }

    private void updateClock(int receivedTimestamp) {
        logicalClock = Math.max(logicalClock, receivedTimestamp) + 1;
    }

    private int[] calculateNextMove() {
        int[] next = {currentX, currentY};

        // Simple pathfinding: move toward target using Manhattan distance
        int dx = targetX - currentX;
        int dy = targetY - currentY;

        // Try to go straight through if possible
        if (dx != 0) {
            // Need to move in X direction
            int nextX = currentX + (dx > 0 ? 1 : -1);
            int nextY = currentY;

            // Check if this cell is available
            if (nextX >= 0 && nextX < 5 && nextY >= 0 && nextY < 5) {
                if (!grid.isCellBlocked(nextX, nextY)) {
                    next[0] = nextX;
                    next[1] = nextY;
                    return next;
                }
            }
        }

        if (dy != 0) {
            // Need to move in Y direction
            int nextX = currentX;
            int nextY = currentY + (dy > 0 ? 1 : -1);

            // Check if this cell is available
            if (nextX >= 0 && nextX < 5 && nextY >= 0 && nextY < 5) {
                if (!grid.isCellBlocked(nextX, nextY)) {
                    next[0] = nextX;
                    next[1] = nextY;
                    return next;
                }
            }
        }

        // If direct path blocked, try adjacent cells
        log("🔄 Direct path blocked, trying alternate route...");
        int[] adjacent = findAdjacentMoveTowardTarget();
        if (adjacent != null) {
            return adjacent;
        }

        // Verify this is an adjacent move
        if (!isAdjacentCell(currentX, currentY, next[0], next[1])) {
            log("⚠️ WARNING: Calculated non-adjacent move! Staying put.");
            return new int[]{currentX, currentY};
        }

        return next;
    }

    private boolean hasReachedTarget() {
        return currentX == targetX && currentY == targetY;
    }

    private String getTimestamp() {
        return String.format("%tT", new java.util.Date());
    }

    private void log(String message) {
        if (mainFrame != null) {
            mainFrame.logMessage("[" + getTimestamp() + "][" + robotId + "] " + message);
        }
    }

    // ============ PUBLIC GETTERS ============

    public String getRobotId() { return robotId; }
    public int getCurrentX() { return currentX; }
    public int getCurrentY() { return currentY; }
    public int getTargetX() { return targetX; }
    public int getTargetY() { return targetY; }
    public Color getColor() { return color; }
    public int getLogicalClock() { return logicalClock; }
    public boolean isRequesting() { return requesting; }
    public boolean isMoving() { return isMoving; }
    public boolean isStatic() { return isStatic; }
    public boolean isInDeadlock() { return inDeadlock; }
    public boolean isTemporarilyMoving() { return temporarilyMoving; }
    public int getRequestCount() { return requestCount; }
    public void setMoving(boolean moving) { this.isMoving = moving; }
}