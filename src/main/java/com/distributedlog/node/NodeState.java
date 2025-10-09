package com.distributedlog.node;

import com.distributedlog.messages.AppendEntries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NodeState keeps currentTerm, votedFor, role, and a local log.
 * Log uses 1-based indexing semantics: first entry has index 1; log.size() is last index.
 * Also tracks leader replication state (nextIndex, matchIndex) for log replication.
 */
public class NodeState {
    private final String nodeId;       // Unique ID of this node
    private int currentTerm = 0;
    private String votedFor = null;
    private NodeRole role = NodeRole.FOLLOWER;

    // Log + replication state
    private final List<LogEntry> log = new ArrayList<>();
    private int commitIndex = 0;
    private int lastApplied = 0;

    // Leader replication tracking
    private final Map<String, Integer> nextIndex = new HashMap<>();
    private final Map<String, Integer> matchIndex = new HashMap<>();

    // --- Constructor (nodeId required) ---
    public NodeState(String nodeId) {
        this.nodeId = nodeId;
    }

    // --- Node ID accessor ---
    public synchronized String getNodeId() {
        return nodeId;
    }

    // --- term / role / vote accessors ---
    public synchronized int getCurrentTerm() { return currentTerm; }
    public synchronized void setCurrentTerm(int term) { this.currentTerm = term; }
    public synchronized void incrementTerm() { this.currentTerm++; }
    public synchronized String getVotedFor() { return votedFor; }
    public synchronized void setVotedFor(String votedFor) { this.votedFor = votedFor; }
    public synchronized NodeRole getRole() { return role; }
    public synchronized void setRole(NodeRole role) { this.role = role; }

    // --- log helpers ---
    public synchronized int getLastLogIndex() { return log.size(); }
    public synchronized int getLastLogTerm() { return log.isEmpty() ? 0 : log.get(log.size() - 1).getTerm(); }

    public synchronized int getTermAtIndex(int index) {
        if (index <= 0 || index > log.size()) return 0;
        return log.get(index - 1).getTerm();
    }

    public synchronized List<String> getCommandsFromTo(int startIndex, int endIndex) {
        List<String> out = new ArrayList<>();
        if (startIndex <= 0) startIndex = 1;
        if (endIndex > log.size()) endIndex = log.size();
        for (int i = startIndex; i <= endIndex; i++) {
            out.add(log.get(i - 1).getCommand());
        }
        return out;
    }

    // --- AppendEntries for followers with consistency check ---
    public synchronized boolean appendEntriesWithConsistency(AppendEntries ae) {
        int prevLogIndex = ae.getPrevLogIndex();
        int prevLogTerm = ae.getPrevLogTerm();

        // Conflict detection
        if (prevLogIndex > 0 && getTermAtIndex(prevLogIndex) != prevLogTerm) {
            return false;
        }

        // Append / overwrite entries
        appendEntries(prevLogIndex, ae.getEntries(), ae.getTerm());

        // Update commit index
        setCommitIndex(ae.getLeaderCommit());
        return true;
    }

    public synchronized void appendEntries(int prevLogIndex, List<String> entries, int termOfEntry) {
        int currentSize = log.size();
        int expectedNextIndex = prevLogIndex + 1;

        // Remove conflicting entries
        if (expectedNextIndex <= currentSize) {
            for (int i = currentSize; i >= expectedNextIndex; i--) {
                log.remove(i - 1);
            }
        }

        // Append new entries
        if (entries != null) {
            for (String cmd : entries) {
                log.add(new LogEntry(termOfEntry, cmd));
            }
        }
    }

    // --- commit / apply logic ---
    public synchronized int getCommitIndex() { return commitIndex; }
    public synchronized void setCommitIndex(int newCommitIndex) {
        if (newCommitIndex > commitIndex) {
            commitIndex = Math.min(newCommitIndex, log.size());
            applyCommittedEntries();
        }
    }

    private void applyCommittedEntries() {
        while (lastApplied < commitIndex) {
            lastApplied++;
            LogEntry e = log.get(lastApplied - 1);
            System.out.println("[StateMachine] Applying log index " + lastApplied + " -> " + e.getCommand());
        }
    }

    // --- Leader replication state helpers ---
    public synchronized void initLeaderState(List<String> allNodeIds) {
        int next = getLastLogIndex() + 1;
        nextIndex.clear();
        matchIndex.clear();
        for (String nodeId : allNodeIds) {
            nextIndex.put(nodeId, next);
            matchIndex.put(nodeId, 0);
        }
    }

    public synchronized int getNextIndex(String nodeId) {
        return nextIndex.getOrDefault(nodeId, getLastLogIndex() + 1);
    }

    public synchronized void decrementNextIndex(String nodeId) {
        int current = nextIndex.getOrDefault(nodeId, getLastLogIndex() + 1);
        if (current > 1) nextIndex.put(nodeId, current - 1);
    }

    public synchronized int getMatchIndex(String nodeId) {
        return matchIndex.getOrDefault(nodeId, 0);
    }

    public synchronized void updateFollowerProgress(String followerId, int matchIdx) {
        matchIndex.put(followerId, matchIdx);
        nextIndex.put(followerId, matchIdx + 1);
        updateCommitIndexForLeader();
    }

    private synchronized void updateCommitIndexForLeader() {
        int N = log.size();
        for (int i = commitIndex + 1; i <= N; i++) {
            int count = 1; // count self
            for (int match : matchIndex.values()) {
                if (match >= i) count++;
            }
            if (count > (matchIndex.size() + 1) / 2 && getTermAtIndex(i) == currentTerm) {
                setCommitIndex(i);
            }
        }
    }

    @Override
    public synchronized String toString() {
        return "NodeState{" +
                "nodeId='" + nodeId + '\'' +
                ", term=" + currentTerm +
                ", votedFor='" + votedFor + '\'' +
                ", role=" + role +
                ", logSize=" + log.size() +
                ", commitIndex=" + commitIndex +
                ", lastApplied=" + lastApplied +
                ", nextIndex=" + nextIndex +
                ", matchIndex=" + matchIndex +
                '}';
    }
}
