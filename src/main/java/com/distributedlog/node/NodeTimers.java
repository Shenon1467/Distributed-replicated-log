package com.distributedlog.node;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles election timeout and heartbeat scheduling for a node.
 */
public class NodeTimers {
    private final NodeState nodeState;
    private final Runnable onElectionTimeout;
    private final Runnable onHeartbeat;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private int electionTimeoutMillis;
    private final Random random = new Random();

    private ScheduledFuture<?> electionFuture;

    public NodeTimers(NodeState nodeState, Runnable onElectionTimeout, Runnable onHeartbeat) {
        this.nodeState = nodeState;
        this.onElectionTimeout = onElectionTimeout;
        this.onHeartbeat = onHeartbeat;
        resetElectionTimeout();
    }

    /** Start both heartbeat and election timers */
    public void start() {
        scheduleHeartbeatTimer();
        startElectionTimer();
    }

    /** Schedule heartbeat timer (leader sends heartbeats) */
    private void scheduleHeartbeatTimer() {
        scheduler.scheduleAtFixedRate(() -> {
            synchronized (nodeState) {
                if (nodeState.getRole() == NodeRole.LEADER) {
                    onHeartbeat.run();
                }
            }
        }, 0, 1000, TimeUnit.MILLISECONDS); // 1 second interval
    }

    /** Start or restart election timer */
    public void startElectionTimer() {
        // Cancel previous timer if exists
        if (electionFuture != null && !electionFuture.isDone()) {
            electionFuture.cancel(true);
        }

        electionFuture = scheduler.schedule(() -> {
            synchronized (nodeState) {
                if (nodeState.getRole() != NodeRole.LEADER) {
                    onElectionTimeout.run();
                }
            }
        }, electionTimeoutMillis, TimeUnit.MILLISECONDS);
    }

    /** Reset election timeout (randomize between 150-300ms) */
    public void resetElectionTimeout() {
        electionTimeoutMillis = 150 + random.nextInt(150);
        startElectionTimer();
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
