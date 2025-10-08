package com.distributedlog;

import com.distributedlog.messages.AppendEntries;
import com.distributedlog.messages.RequestVote;
import com.distributedlog.network.MessageClient;
import com.distributedlog.network.MessageServer;
import com.distributedlog.node.NodeState;
import com.distributedlog.node.NodeRole;
import com.distributedlog.node.NodeTimers;

public class Main {
    public static void main(String[] args) {
        NodeState nodeState = new NodeState();

        // Define election timeout task
        Runnable electionTimeoutTask = () -> {
            synchronized (nodeState) {
                System.out.println("Election timeout! Node becomes CANDIDATE.");
                nodeState.setRole(NodeRole.CANDIDATE);
                nodeState.setCurrentTerm(nodeState.getCurrentTerm() + 1);
                nodeState.setVotedFor("Self");
                System.out.println("Updated NodeState: " + nodeState);
            }
        };

        // Define heartbeat task
        Runnable heartbeatTask = () -> {
            synchronized (nodeState) {
                if (nodeState.getRole() == NodeRole.LEADER) {
                    System.out.println("Leader heartbeat: sending AppendEntries to followers...");
                    AppendEntries append = new AppendEntries(nodeState.getCurrentTerm(), "LeaderNode");
                    // TODO: send to followers via MessageClient
                }
            }
        };

        // Start NodeTimers
        NodeTimers timers = new NodeTimers(nodeState, electionTimeoutTask, heartbeatTask);
        timers.start();

        // Start MessageServer
        int serverPort = 5001;
        new Thread(new MessageServer(serverPort, nodeState, timers)).start();

        // Wait to ensure server starts
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        // Simulate sending messages
        RequestVote vote = new RequestVote(1, "NodeA");
        AppendEntries append = new AppendEntries(1, "Leader1");

        MessageClient.sendMessage("localhost", serverPort, vote);
        MessageClient.sendMessage("localhost", serverPort, append);
    }
}
