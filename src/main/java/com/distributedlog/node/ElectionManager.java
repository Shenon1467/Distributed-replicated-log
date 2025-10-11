package com.distributedlog.node;

import com.distributedlog.messages.AppendEntries;
import com.distributedlog.messages.AppendEntriesResponse;
import com.distributedlog.messages.RequestVote;
import com.distributedlog.network.MessageClient;
import com.google.gson.Gson;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ElectionManager {
    private final NodeState nodeState;
    private final int selfPort;
    private final List<Integer> peerPorts;
    private final Map<Integer, Integer> nextIndex = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> matchIndex = new ConcurrentHashMap<>();
    private ScheduledExecutorService leaderScheduler;
    private final Gson gson = new Gson();

    public ElectionManager(NodeState nodeState, int selfPort, List<Integer> peerPorts) {
        this.nodeState = nodeState;
        this.selfPort = selfPort;
        this.peerPorts = peerPorts;
    }

    // --- Election (unchanged) ---
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

                        synchronized (nodeState) {
                            if (granted >= majority && nodeState.getRole() == NodeRole.CANDIDATE) {
                                nodeState.setRole(NodeRole.LEADER);
                                System.out.println("[Election] Node became LEADER for term " + nodeState.getCurrentTerm());
                                becomeLeader();
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[Client] Connection error to localhost:" + port + " -> " + e.getMessage());
                }
            }).start();
        }
    }

    // Called when candidate becomes leader
    private void becomeLeader() {
        System.out.println("[Leader] Initializing leader state for term " + nodeState.getCurrentTerm());

        synchronized (nodeState) {
            //set self as leader
            nodeState.setLeaderId(nodeState.getNodeId());

            int lastIndex = nodeState.getLastLogIndex();
            for (int p : peerPorts) {
                nextIndex.put(p, lastIndex + 1);
                matchIndex.put(p, 0);
            }
        }

        startLeaderSchedule();
    }

    private void startLeaderSchedule() {
        if (leaderScheduler != null && !leaderScheduler.isShutdown()) {
            leaderScheduler.shutdownNow();
        }
        leaderScheduler = Executors.newSingleThreadScheduledExecutor();
        leaderScheduler.scheduleAtFixedRate(this::sendHeartbeatsAndReplicate, 0, 500, TimeUnit.MILLISECONDS);
    }

    /** Heartbeat + replication loop */
    private void sendHeartbeatsAndReplicate() {
        // for each peer send AppendEntries that may contain entries (if leader has new ones)
        for (int peer : peerPorts) {
            CompletableFuture.runAsync(() -> sendAppendEntriesToPeer(peer));
        }
    }

    /** Send AppendEntries to a single peer, possibly including entries starting from nextIndex[peer] */
    private void sendAppendEntriesToPeer(int peerPort) {
        int nextIdx = nextIndex.getOrDefault(peerPort, 1);
        int prevIndex = Math.max(0, nextIdx - 1);
        int prevTerm;
        List<String> entriesToSend = null; // null => heartbeat
        int leaderCommit;
        synchronized (nodeState) {
            prevTerm = nodeState.getTermAtIndex(prevIndex);
            int lastIndex = nodeState.getLastLogIndex();
            if (nextIdx <= lastIndex) {
                // send entries from nextIdx..lastIndex
                entriesToSend = nodeState.getCommandsFromTo(nextIdx, lastIndex);
            }
            leaderCommit = nodeState.getCommitIndex();
        }

        AppendEntries ae = new AppendEntries(nodeState.getCurrentTerm(),
                "Leader" + selfPort,
                prevIndex,
                prevTerm,
                entriesToSend,
                leaderCommit);

        try {
            String respJson = MessageClient.sendMessage("localhost", peerPort, ae);
            if (respJson == null) return;

            AppendEntriesResponse resp = new Gson().fromJson(respJson, AppendEntriesResponse.class);

            synchronized (nodeState) {
                // If follower has higher term, step down
                if (resp.getTerm() > nodeState.getCurrentTerm()) {
                    nodeState.setCurrentTerm(resp.getTerm());
                    nodeState.setRole(NodeRole.FOLLOWER);
                    stopLeaderScheduler();
                    return;
                }

                if (resp.isSuccess()) {
                    int matched = resp.getMatchIndex();
                    nextIndex.put(peerPort, matched + 1);
                    matchIndex.put(peerPort, matched);
                } else {
                    // follower rejected — decrement nextIndex (backoff) and retry later
                    int ni = Math.max(1, nextIndex.getOrDefault(peerPort, 1) - 1);
                    nextIndex.put(peerPort, ni);
                }

                // Try to advance commit index (basic majority check)
                tryAdvanceCommitIndex();
            }
        } catch (Exception e) {
            System.out.println("[Leader] append to " + peerPort + " failed: " + e.getMessage());
        }
    }

    private void tryAdvanceCommitIndex() {
        int lastIndex;
        synchronized (nodeState) {
            lastIndex = nodeState.getLastLogIndex();
        }
        int majority = (peerPorts.size() + 1) / 2 + 1;

        for (int N = lastIndex; N > nodeState.getCommitIndex(); N--) {
            final int target = N;
            int count = 1; // leader itself
            for (int p : peerPorts) {
                int mk = matchIndex.getOrDefault(p, 0);
                if (mk >= target) count++;
            }

            if (count >= majority) {
                // ensure entry at N was created in current term (safety)
                int termAtN;
                synchronized (nodeState) {
                    termAtN = nodeState.getTermAtIndex(target);
                }
                if (termAtN == nodeState.getCurrentTerm()) {
                    // advance commit index
                    System.out.println("[Leader] Advancing commitIndex to " + target);
                    nodeState.setCommitIndex(target);
                    break;
                }
            }
        }
    }

    private void stopLeaderScheduler() {
        if (leaderScheduler != null) {
            leaderScheduler.shutdownNow();
            leaderScheduler = null;
        }
    }

    /** Called by external client to append a new command to leader's log */
    public void appendCommandAsLeader(String command) {
        synchronized (nodeState) {
            int term = nodeState.getCurrentTerm();
            // prevLogIndex should be last log index; we append by passing prevLogIndex = lastIndex
            nodeState.appendEntries(nodeState.getLastLogIndex(), Collections.singletonList(command), term);
        }
        // replication will happen on next heartbeat cycle
    }
}
