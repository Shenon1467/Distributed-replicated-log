package com.distributedlog.node;

import com.distributedlog.messages.RequestVote;
import com.distributedlog.messages.RequestVoteResponse;
import com.distributedlog.network.MessageClient;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ElectionManager {
    private final NodeState nodeState;
    private final int selfPort;
    private final List<Integer> peerPorts; // ports of other nodes

    public ElectionManager(NodeState nodeState, int selfPort, List<Integer> peerPorts) {
        this.nodeState = nodeState;
        this.selfPort = selfPort;
        this.peerPorts = peerPorts;
    }

    // Called when a node becomes candidate
    public void startElection() {
        synchronized (nodeState) {
            nodeState.incrementTerm();
            nodeState.setVotedFor("Self");
            nodeState.setRole(NodeRole.CANDIDATE);
        }

        System.out.println("[Election] Starting election for term " + nodeState.getCurrentTerm());

        RequestVote voteRequest = new RequestVote(nodeState.getCurrentTerm(), "Self");

        AtomicInteger votesGranted = new AtomicInteger(1); // vote for self
        int majority = (peerPorts.size() + 1) / 2 + 1;

        for (int port : peerPorts) {
            new Thread(() -> {
                try {
                    String responseJson = MessageClient.sendMessage("localhost", port, voteRequest);

                    if (responseJson != null && responseJson.contains("\"voteGranted\":true")) {
                        int granted = votesGranted.incrementAndGet();
                        System.out.println("[Election] Vote granted by node on port " + port + ". Total: " + granted);

                        // Become leader if majority reached
                        synchronized (nodeState) {
                            if (granted >= majority && nodeState.getRole() == NodeRole.CANDIDATE) {
                                nodeState.setRole(NodeRole.LEADER);
                                System.out.println("[Election] Node became LEADER for term " + nodeState.getCurrentTerm());
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[Client] Connection error to localhost:" + port + " -> " + e.getMessage());
                }
            }).start();
        }
    }
}
