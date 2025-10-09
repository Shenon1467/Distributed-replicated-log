package com.distributedlog.network;

import com.distributedlog.messages.AppendEntries;
import com.distributedlog.messages.AppendEntriesResponse;
import com.distributedlog.messages.RequestVote;
import com.distributedlog.messages.RequestVoteResponse;
import com.distributedlog.node.NodeState;
import com.distributedlog.node.NodeTimers;
import com.distributedlog.node.NodeRole;
import com.google.gson.Gson;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class MessageServer implements Runnable {
    private final int port;
    private final NodeState nodeState;
    private final NodeTimers nodeTimers;
    private final Gson gson = new Gson();

    public MessageServer(int port, NodeState nodeState, NodeTimers nodeTimers) {
        this.port = port;
        this.nodeState = nodeState;
        this.nodeTimers = nodeTimers;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[Server] Listening on port " + port);

            while (true) {
                Socket client = serverSocket.accept();

                try (
                        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                        PrintWriter out = new PrintWriter(client.getOutputStream(), true)
                ) {
                    String message = in.readLine();
                    if (message != null) {
                        System.out.println("[Server " + port + "] Received: " + message);

                        if (message.contains("candidateId")) {
                            handleRequestVote(message, out);
                        } else if (message.contains("leaderId")) {
                            handleAppendEntries(message, out);
                        } else {
                            out.println("{}");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try { client.close(); } catch (IOException ignored) {}
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ------------------ Handle RequestVote RPC ------------------
    private void handleRequestVote(String message, PrintWriter out) {
        RequestVote vote = gson.fromJson(message, RequestVote.class);
        RequestVoteResponse resp;

        synchronized (nodeState) {
            if (vote.getTerm() > nodeState.getCurrentTerm()) {
                nodeState.setCurrentTerm(vote.getTerm());
                nodeState.setVotedFor(null);
                nodeState.setRole(NodeRole.FOLLOWER);
            }

            boolean grant = false;
            if (vote.getTerm() >= nodeState.getCurrentTerm()) {
                String votedFor = nodeState.getVotedFor();
                if (votedFor == null || votedFor.equals(vote.getCandidateId())) {
                    nodeState.setVotedFor(vote.getCandidateId());
                    grant = true;
                }
            }

            resp = new RequestVoteResponse(nodeState.getCurrentTerm(), grant);
            System.out.println("[Server " + port + "] RequestVote -> grant=" + grant + ", nodeState=" + nodeState);
        }

        out.println(gson.toJson(resp));
    }

    // ------------------ Handle AppendEntries RPC ------------------
    private void handleAppendEntries(String message, PrintWriter out) {
        AppendEntries append = gson.fromJson(message, AppendEntries.class);
        AppendEntriesResponse resp;

        synchronized (nodeState) {
            // If RPC's term is higher, update local term and become follower
            if (append.getTerm() > nodeState.getCurrentTerm()) {
                nodeState.setCurrentTerm(append.getTerm());
                nodeState.setVotedFor(null);
                nodeState.setRole(NodeRole.FOLLOWER);
            }

            boolean success = false;
            int matchIndex = 0;

            // If leader's term is older, reject quickly
            if (append.getTerm() < nodeState.getCurrentTerm()) {
                success = false;
                matchIndex = nodeState.getLastLogIndex();
            } else {
                // Use NodeState's appendEntriesWithConsistency which checks prevLogIndex/prevLogTerm
                success = nodeState.appendEntriesWithConsistency(append);
                if (success) {
                    matchIndex = nodeState.getLastLogIndex();
                    nodeState.setRole(NodeRole.FOLLOWER);
                    // Reset election timer on valid AppendEntries from leader
                    if (nodeTimers != null) nodeTimers.resetElectionTimeout();
                } else {
                    // conflict: keep matchIndex as last known index
                    matchIndex = nodeState.getLastLogIndex();
                }
            }

            resp = new AppendEntriesResponse(nodeState.getCurrentTerm(), success, matchIndex);
            System.out.println("[Server " + port + "] AppendEntries -> success=" + success + ", nodeState=" + nodeState);
        }

        out.println(gson.toJson(resp));
    }
}
