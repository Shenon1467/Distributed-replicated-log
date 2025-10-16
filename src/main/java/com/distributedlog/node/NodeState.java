package com.distributedlog.node;

import com.distributedlog.messages.AppendEntries;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

/**
 * NodeState keeps currentTerm, votedFor, role, and a local log.
 * Log uses 1-based indexing semantics: first entry has index 1; log.size() is last index.
 * Also tracks leader replication state (nextIndex, matchIndex) for log replication.
 *
 * Now includes persistence support and leader tracking for client redirection.
 */
public class NodeState {
    private final String nodeId;       // Unique ID of this node
    private int currentTerm = 0;
    private String votedFor = null;
    private NodeRole role = NodeRole.FOLLOWER;

    // --- Leader tracking ---
    private String leaderId = null;    // current leader of the cluster

    // Log + replication state
    private final List<LogEntry> log = new ArrayList<>();
    private int commitIndex = 0;
    private int lastApplied = 0;

    // Leader replication tracking
    private final Map<String, Integer> nextIndex = new HashMap<>();
    private final Map<String, Integer> matchIndex = new HashMap<>();

    // --- Persistence ---
    private final File storageDir;

    public NodeState(String nodeId) {
        this.nodeId = nodeId;

        // Persistence setup
        this.storageDir = new File("data/" + nodeId);
        if (!storageDir.exists()) storageDir.mkdirs();

        loadState(); // Try to load state from disk
    }

    // --- Node ID accessor ---
    public synchronized String getNodeId() {
        return nodeId;
    }

    // --- term / role / vote accessors ---
    public synchronized int getCurrentTerm() { return currentTerm; }
    public synchronized void setCurrentTerm(int term) {
        this.currentTerm = term;
        saveState();
    }
    public synchronized void incrementTerm() {
        this.currentTerm++;
        saveState();
    }
    public synchronized String getVotedFor() { return votedFor; }
    public synchronized void setVotedFor(String votedFor) {
        this.votedFor = votedFor;
        saveState();
    }
    public synchronized NodeRole getRole() { return role; }
    public synchronized void setRole(NodeRole role) {
        this.role = role;
        //if this node is now leader, set leaderid to self
        if (role == NodeRole.LEADER) {
        this.leaderId = this.nodeId;
        }
        saveState();
    }

    // --- Leader accessors ---
    public synchronized String getLeaderId() {
        return leaderId;
    }
    public synchronized void setLeaderId(String leaderId) {
        this.leaderId = leaderId;
    }

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

        saveLog();//save the manual_log.txt
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

        saveState(); // Persist new log entries
    }

    // --- commit / apply logic ---
    public synchronized int getCommitIndex() { return commitIndex; }
    public synchronized void setCommitIndex(int newCommitIndex) {
        if (newCommitIndex > commitIndex) {
            commitIndex = Math.min(newCommitIndex, log.size());
            applyCommittedEntries();
            saveState();
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

    // --- Persistence Methods ---
    private synchronized void saveState() {
        try {
            Gson gson = new Gson();

            // Save metadata safely (allow nulls)
            File stateFile = new File(storageDir, "state.json");
            Map<String, Object> stateMap = new HashMap<>();
            stateMap.put("currentTerm", currentTerm);
            stateMap.put("votedFor", votedFor);
            stateMap.put("leaderId", leaderId); // persist leaderId too

            try (Writer writer = new FileWriter(stateFile)) {
                gson.toJson(stateMap, writer);
            }

            // Save log
            File logFile = new File(storageDir, "log.json");
            try (Writer writer = new FileWriter(logFile)) {
                gson.toJson(log, writer);
            }

            System.out.println("[Persistence] State saved for " + nodeId +
                    " (term=" + currentTerm + ", logSize=" + log.size() + ")");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void loadState() {
        try {
            Gson gson = new Gson();

            // Load metadata
            File stateFile = new File(storageDir, "state.json");
            if (stateFile.exists()) {
                try (Reader reader = new FileReader(stateFile)) {
                    Type type = new TypeToken<Map<String, Object>>() {}.getType();
                    Map<String, Object> data = gson.fromJson(reader, type);

                    // Check for null (empty file)
                    if (data != null) {
                        currentTerm = ((Double) data.getOrDefault("currentTerm", 0.0)).intValue();
                        votedFor = (String) data.getOrDefault("votedFor", null);
                        leaderId = (String) data.getOrDefault("leaderId", null);
                    }
                }
            }

            // Load log
            File logFile = new File(storageDir, "log.json");
            if (logFile.exists()) {
                try (Reader reader = new FileReader(logFile)) {
                    Type listType = new TypeToken<List<LogEntry>>() {}.getType();
                    List<LogEntry> loaded = gson.fromJson(reader, listType);
                    if (loaded != null) log.addAll(loaded);
                }
            }

            System.out.println("[Persistence] Loaded state for " + nodeId +
                    " (term=" + currentTerm + ", logSize=" + log.size() + ")");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- manual log saving ---
    public synchronized void saveLog() {
        try {
            File logFile = new File(storageDir, "manual_log.txt"); // separate from log.json
            try (Writer writer = new FileWriter(logFile)) {
                for (LogEntry entry : log) {
                    writer.write(entry.getTerm() + ":" + entry.getCommand() + "\n");
                }
            }
            System.out.println("[NodeState] Log manually saved for node " + nodeId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized String toString() {
        return "NodeState{" +
                "nodeId='" + nodeId + '\'' +
                ", term=" + currentTerm +
                ", votedFor='" + votedFor + '\'' +
                ", role=" + role +
                ", leaderId='" + leaderId + '\'' +
                ", logSize=" + log.size() +
                ", commitIndex=" + commitIndex +
                ", lastApplied=" + lastApplied +
                ", nextIndex=" + nextIndex +
                ", matchIndex=" + matchIndex +
                '}';
    }
}
