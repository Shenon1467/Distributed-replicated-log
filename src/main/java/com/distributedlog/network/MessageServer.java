package com.distributedlog.network;

import com.distributedlog.messages.AppendEntries;
import com.distributedlog.messages.AppendEntriesResponse;
import com.distributedlog.messages.RequestVote;
import com.distributedlog.messages.RequestVoteResponse;
import com.distributedlog.node.NodeRole;
import com.distributedlog.node.NodeState;
import com.distributedlog.node.NodeTimers;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;

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
                    if (message != null && !message.isBlank()) {
                        System.out.println("[Server " + port + "] Received: " + message.trim());
                        handleIncomingMessage(message.trim(), out);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        client.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ------------------ Dispatch Messages ------------------
    private void handleIncomingMessage(String message, PrintWriter out) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();

            if (json.has("candidateId")) {
                handleRequestVote(message, out);
            } else if (json.has("leaderId")) {
                handleAppendEntries(message, out);
            }
            // ✅ Updated: Accept messages with "clientCommand"
            else if (json.has("clientCommand")) {
                handleClientCommand(json, out);
            }
            // ✅ Still handle leader queries
            else if (json.has("getLeader")) {
                handleLeaderQuery(out);
            } else {
                out.println("{\"status\":\"unknown_message\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.println("{\"error\":\"invalid_json\"}");
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
            if (append.getTerm() > nodeState.getCurrentTerm()) {
                nodeState.setCurrentTerm(append.getTerm());
                nodeState.setVotedFor(null);
                nodeState.setRole(NodeRole.FOLLOWER);
            }

            boolean success;
            int matchIndex;

            if (append.getTerm() < nodeState.getCurrentTerm()) {
                success = false;
                matchIndex = nodeState.getLastLogIndex();
            } else {
                success = nodeState.appendEntriesWithConsistency(append);
                matchIndex = nodeState.getLastLogIndex();

                if (success) {
                    nodeState.setRole(NodeRole.FOLLOWER);
                    if (nodeTimers != null) nodeTimers.resetElectionTimeout();

                    nodeState.setLeaderId(append.getLeaderId()); //track leader
                }
            }

            resp = new AppendEntriesResponse(nodeState.getCurrentTerm(), success, matchIndex);
            System.out.println("[Server " + port + "] AppendEntries -> success=" + success + ", nodeState=" + nodeState);
        }

        out.println(gson.toJson(resp));
    }

    // ------------------ Handle Client Command ------------------
    private void handleClientCommand(JsonObject json, PrintWriter out) {
        synchronized (nodeState) {
            // ✅ Updated: match your client’s message format
            String command = json.has("data") ? json.get("data").getAsString() : null;
            System.out.println("[Client->Server " + port + "] Received client command JSON: " + json);

            if (command == null || command.isEmpty()) {
                out.println("{\"status\":\"error\",\"message\":\"Empty command\"}");
                return;
            }

            if (nodeState.getRole() != NodeRole.LEADER) {
                String leaderId = nodeState.getLeaderId() != null ? nodeState.getLeaderId() : "unknown";
                out.println("{\"status\":\"redirect\",\"leader\":\"" + leaderId + "\",\"message\":\"This node is not the leader\"}");
                return;
            }

            // Append client command as a new log entry
            nodeState.appendEntries(
                    nodeState.getLastLogIndex(),
                    Collections.singletonList(command),
                    nodeState.getCurrentTerm()
            );
            nodeState.setCommitIndex(nodeState.getLastLogIndex());
            nodeState.saveLog(); // ensure persistence

            out.println("{\"status\":\"ok\",\"message\":\"Command committed: " + command + "\"}");
            System.out.println("[Server " + port + "] Client command committed -> " + command);
        }
    }

    // ------------------ Handle Leader Query ------------------
    private void handleLeaderQuery(PrintWriter out) {
        synchronized (nodeState) {
            String leader = nodeState.getLeaderId() != null ? nodeState.getLeaderId() : "unknown";
            out.println("{\"leaderId\":\"" + leader + "\"}");
        }
    }
}
