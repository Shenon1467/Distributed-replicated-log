package com.distributedlog.node;

import com.distributedlog.messages.AppendEntries;
import com.distributedlog.messages.RequestVote;
import com.distributedlog.network.MessageClient;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class ElectionManager {
    private final NodeState nodeState;
    private final int selfPort;
    private final List<Integer> peerPorts;
    private Timer heartbeatTimer;

    public ElectionManager(NodeState nodeState, int selfPort, List<Integer> peerPorts) {
        this.nodeState = nodeState;
        this.selfPort = selfPort;
        this.peerPorts = peerPorts;
    }

    // -------------------------------------------
    // PHASE 2: Leader Election Logic
    // -------------------------------------------
    public void startElection() {
        synchronized (nodeState) {
            nodeState.incrementTerm();
            nodeState.setVotedFor("Self");
            nodeState.setRole(NodeRole.CANDIDATE);
        }

        System.out.println("[Election] Starting election for term " + nodeState.getCurrentTerm());

        RequestVote voteRequest = new RequestVote(nodeState.getCurrentTerm(), "Self");

        AtomicInteger votesGranted = new AtomicInteger(1); // self vote
        int majority = (peerPorts.size() + 1) / 2 + 1;

        for (int port : peerPorts) {
            new Thread(() -> {
                try {
                    String responseJson = MessageClient.sendMessage("localhost", port, voteRequest);

                    if (responseJson != null && responseJson.contains("\"voteGranted\":true")) {
                        int granted = votesGranted.incrementAndGet();
                        System.out.println("[Election] Vote granted by node on port " + port + ". Total: " + granted);

                        synchronized (nodeState) {
                            if (granted >= majority && nodeState.getRole() == NodeRole.CANDIDATE) {
                                nodeState.setRole(NodeRole.LEADER);
                                System.out.println("[Election] Node became LEADER for term " + nodeState.getCurrentTerm());
                                startHeartbeat();
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[Client] Connection error to localhost:" + port + " -> " + e.getMessage());
                }
            }).start();
        }
    }

    // -------------------------------------------
    // PHASE 3 - COMPONENT 6: Heartbeat & Leader Logic
    // -------------------------------------------
    private void startHeartbeat() {
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
        }
        heartbeatTimer = new Timer(true);

        // send heartbeats periodically (every 1s)
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                synchronized (nodeState) {
                    if (nodeState.getRole() != NodeRole.LEADER) {
                        heartbeatTimer.cancel();
                        return;
                    }
                }

                AppendEntries heartbeat = new AppendEntries(
                        nodeState.getCurrentTerm(),
                        "Leader" + selfPort,
                        nodeState.getLastLogIndex(),
                        nodeState.getLastLogTerm(),
                        null, // no log entries (heartbeat only)
                        nodeState.getCommitIndex()
                );

                for (int port : peerPorts) {
                    new Thread(() -> {
                        try {
                            String response = MessageClient.sendMessage("localhost", port, heartbeat);
                            if (response != null && response.contains("\"success\":true")) {
                                System.out.println("[Heartbeat] ACK from follower on port " + port);
                            }
                        } catch (Exception e) {
                            System.out.println("[Heartbeat] Failed to contact follower on port " + port);
                        }
                    }).start();
                }
            }
        }, 0, 1000); // every 1 second
    }
}
