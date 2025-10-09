package com.distributedlog.node;

import java.util.ArrayList;
import java.util.List;

public class NodeState {
    private int currentTerm = 0;
    private String votedFor = null;
    private NodeRole role = NodeRole.FOLLOWER;

    // Log replication state
    private final List<LogEntry> log = new ArrayList<>(); // 1-based index semantics simplified
    private int commitIndex = 0;

    // existing constructors/getters/setters here...
    public synchronized int getCurrentTerm() { return currentTerm; }
    public synchronized void setCurrentTerm(int term) { this.currentTerm = term; }
    public synchronized void incrementTerm() { this.currentTerm++; }

    public synchronized String getVotedFor() { return votedFor; }
    public synchronized void setVotedFor(String votedFor) { this.votedFor = votedFor; }

    public synchronized NodeRole getRole() { return role; }
    public synchronized void setRole(NodeRole role) { this.role = role; }

    @Override
    public synchronized String toString() {
        return "NodeState{term=" + currentTerm + ", votedFor='" + votedFor + "', role=" + role + ", logSize=" + log.size() + ", commitIndex=" + commitIndex + "}";
    }

    // --- log helper methods (synchronized) ---
    public synchronized int getLastLogIndex() {
        return log.size(); // 0 when empty
    }

    public synchronized int getLastLogTerm() {
        if (log.isEmpty()) return 0;
        return log.get(log.size() - 1).getTerm();
    }

    public synchronized void appendEntries(int startIndex, java.util.List<String> entries, int termOfEntry) {
        // startIndex is index after which new entries should be appended (1-based semantics)
        // for simplicity: if startIndex < log.size() we will truncate and append
        int currentSize = log.size();
        if (startIndex < currentSize) {
            // remove conflicting entries
            for (int i = currentSize - 1; i >= startIndex; i--) {
                log.remove(i);
            }
        }
        // append new entries
        for (String cmd : entries) {
            log.add(new LogEntry(termOfEntry, cmd));
        }
    }

    public synchronized int getCommitIndex() { return commitIndex; }

    public synchronized void setCommitIndex(int idx) {
        if (idx > commitIndex) {
            commitIndex = Math.min(idx, log.size());
        }
    }
}
