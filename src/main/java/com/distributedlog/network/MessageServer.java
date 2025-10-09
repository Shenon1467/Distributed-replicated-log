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

                        // ---------- RequestVote handling (Follower behavior) ----------
                        if (message.contains("candidateId")) {
                            RequestVote vote = gson.fromJson(message, RequestVote.class);
                            RequestVoteResponse resp;

                            synchronized (nodeState) {
                                // If RPC term > currentTerm, update term and step down to follower
                                if (vote.getTerm() > nodeState.getCurrentTerm()) {
                                    nodeState.setCurrentTerm(vote.getTerm());
                                    nodeState.setVotedFor(null); // reset vote for new term
                                    nodeState.setRole(NodeRole.FOLLOWER);
                                }

                                boolean grant = false;
                                // Only grant vote if candidate's term == currentTerm (or higher) and hasn't voted yet
                                if (vote.getTerm() >= nodeState.getCurrentTerm()) {
                                    String votedFor = nodeState.getVotedFor();
                                    if (votedFor == null || votedFor.equals(vote.getCandidateId())) {
                                        // Note: we do NOT yet check log up-to-date (no log implemented in Phase 1)
                                        nodeState.setVotedFor(vote.getCandidateId());
                                        grant = true;
                                    }
                                } else {
                                    grant = false;
                                }

                                resp = new RequestVoteResponse(nodeState.getCurrentTerm(), grant);
                                System.out.println("[Server " + port + "] RequestVote -> grant=" + grant + ", nodeState=" + nodeState);
                            }

                            // send response JSON
                            out.println(gson.toJson(resp));
                        }

                        // ---------- AppendEntries handling (heartbeat) ----------
                        else if (message.contains("leaderId")) {
                            AppendEntries append = gson.fromJson(message, AppendEntries.class);
                            AppendEntriesResponse resp;

                            synchronized (nodeState) {
                                // If leader's term is greater, update our term and become follower
                                if (append.getTerm() > nodeState.getCurrentTerm()) {
                                    nodeState.setCurrentTerm(append.getTerm());
                                    nodeState.setVotedFor(null);
                                }

                                // If leader's term is >= ours, accept heartbeat and reset election timer
                                boolean success = false;
                                if (append.getTerm() >= nodeState.getCurrentTerm()) {
                                    nodeState.setRole(NodeRole.FOLLOWER);
                                    success = true;

                                    // reset election timer because follower heard from leader
                                    if (nodeTimers != null) {
                                        nodeTimers.resetElectionTimeout();
                                    }
                                }
                                resp = new AppendEntriesResponse(nodeState.getCurrentTerm(), success);
                                System.out.println("[Server " + port + "] AppendEntries -> success=" + success + ", nodeState=" + nodeState);
                            }

                            out.println(gson.toJson(resp));
                        } else {
                            // Unknown message type - reply with simple nack JSON or nothing
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
}
