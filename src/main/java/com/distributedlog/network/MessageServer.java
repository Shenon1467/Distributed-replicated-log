package com.distributedlog.network;

import com.distributedlog.messages.AppendEntries;
import com.distributedlog.messages.RequestVote;
import com.distributedlog.node.NodeState;
import com.distributedlog.node.NodeTimers;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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

                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(client.getInputStream()))) {

                    String message = in.readLine();
                    if (message != null) {
                        System.out.println("[Server " + port + "] Received: " + message);

                        if (message.contains("candidateId")) {
                            RequestVote vote = gson.fromJson(message, RequestVote.class);
                            synchronized (nodeState) {
                                if (vote.getTerm() > nodeState.getCurrentTerm()) {
                                    nodeState.setCurrentTerm(vote.getTerm());
                                    nodeState.setVotedFor(vote.getCandidateId());
                                    nodeState.setRole(nodeState.getRole()); // stay follower
                                    System.out.println("Current NodeState: " + nodeState);
                                }
                            }
                        } else if (message.contains("leaderId")) {
                            AppendEntries append = gson.fromJson(message, AppendEntries.class);
                            synchronized (nodeState) {
                                if (append.getTerm() >= nodeState.getCurrentTerm()) {
                                    nodeState.setCurrentTerm(append.getTerm());
                                    nodeState.setRole(com.distributedlog.node.NodeRole.FOLLOWER);

                                    // Reset election timer when heartbeat received
                                    nodeTimers.resetElectionTimeout();
                                }
                                System.out.println("Current NodeState: " + nodeState);
                            }
                        }
                    }
                }

                client.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
