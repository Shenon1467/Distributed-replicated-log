package com.distributedlog;

import com.distributedlog.messages.AppendEntries;
import com.distributedlog.messages.RequestVote;
import com.distributedlog.messages.RequestVoteResponse;
import com.distributedlog.messages.AppendEntriesResponse;
import com.distributedlog.network.MessageClient;
import com.distributedlog.network.MessageServer;
import com.distributedlog.node.NodeState;
import com.distributedlog.node.NodeRole;
import com.distributedlog.node.NodeTimers;
import com.google.gson.Gson;

public class Main {
    public static void main(String[] args) {
        NodeState nodeState = new NodeState();

        // election timeout behavior
        Runnable electionTimeoutTask = () -> {
            synchronized (nodeState) {
                if (nodeState.getRole() != NodeRole.LEADER) {
                    System.out.println("Election timeout! Node becomes CANDIDATE.");
                    nodeState.setRole(NodeRole.CANDIDATE);
                    nodeState.setCurrentTerm(nodeState.getCurrentTerm() + 1);
                    nodeState.setVotedFor("Self");
                    System.out.println("Updated NodeState: " + nodeState);
                }
            }
        };

        // heartbeat behavior (leader)
        Runnable heartbeatTask = () -> {
            synchronized (nodeState) {
                if (nodeState.getRole() == NodeRole.LEADER) {
                    System.out.println("Leader heartbeat (would send AppendEntries in a cluster)...");
                }
            }
        };

        NodeTimers timers = new NodeTimers(nodeState, electionTimeoutTask, heartbeatTask);
        timers.start();

        int serverPort = 5001;
        new Thread(new MessageServer(serverPort, nodeState, timers)).start();

        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        // send a RequestVote RPC and parse the response
        RequestVote vote = new RequestVote(1, "NodeA");
        String rawResp = MessageClient.sendMessageWithResponse("localhost", serverPort, vote);
        if (rawResp != null) {
            RequestVoteResponse r = new Gson().fromJson(rawResp, RequestVoteResponse.class);
            System.out.println("RequestVoteResponse: term=" + r.getTerm() + ", voteGranted=" + r.isVoteGranted());
        }

        // send an AppendEntries (heartbeat)
        AppendEntries append = new AppendEntries(1, "Leader1");
        String rawResp2 = MessageClient.sendMessageWithResponse("localhost", serverPort, append);
        if (rawResp2 != null) {
            AppendEntriesResponse ar = new Gson().fromJson(rawResp2, AppendEntriesResponse.class);
            System.out.println("AppendEntriesResponse: term=" + ar.getTerm() + ", success=" + ar.isSuccess());
        }
    }
}
