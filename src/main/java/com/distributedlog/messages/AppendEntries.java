package com.distributedlog.messages;

import java.util.List;

public class AppendEntries {
    private int term;                // Leader’s term
    private String leaderId;         // So follower can redirect clients
    private int prevLogIndex;        // Index of log entry immediately preceding new ones
    private int prevLogTerm;         // Term of prevLogIndex entry
    private List<String> entries;    // Log entries to store (empty for heartbeat)
    private int leaderCommit;        // Leader’s commit index

    // ✅ Full constructor (Phase 3)
    public AppendEntries(int term, String leaderId, int prevLogIndex, int prevLogTerm,
                         List<String> entries, int leaderCommit) {
        this.term = term;
        this.leaderId = leaderId;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.entries = entries;
        this.leaderCommit = leaderCommit;
    }

    // ✅ Minimal constructor (for backward compatibility / testing)
    public AppendEntries(int term, String leaderId) {
        this(term, leaderId, 0, 0, null, 0);
    }

    // ---------- Getters ----------
    public int getTerm() { return term; }
    public String getLeaderId() { return leaderId; }
    public int getPrevLogIndex() { return prevLogIndex; }
    public int getPrevLogTerm() { return prevLogTerm; }
    public List<String> getEntries() { return entries; }
    public int getLeaderCommit() { return leaderCommit; }

    // ---------- Setters ----------
    public void setTerm(int term) { this.term = term; }
    public void setLeaderId(String leaderId) { this.leaderId = leaderId; }
    public void setPrevLogIndex(int prevLogIndex) { this.prevLogIndex = prevLogIndex; }
    public void setPrevLogTerm(int prevLogTerm) { this.prevLogTerm = prevLogTerm; }
    public void setEntries(List<String> entries) { this.entries = entries; }
    public void setLeaderCommit(int leaderCommit) { this.leaderCommit = leaderCommit; }

    @Override
    public String toString() {
        return "AppendEntries{" +
                "term=" + term +
                ", leaderId='" + leaderId + '\'' +
                ", prevLogIndex=" + prevLogIndex +
                ", prevLogTerm=" + prevLogTerm +
                ", entries=" + entries +
                ", leaderCommit=" + leaderCommit +
                '}';
    }
}
