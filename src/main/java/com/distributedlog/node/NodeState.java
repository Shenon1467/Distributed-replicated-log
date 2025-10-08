package com.distributedlog.node;

public class NodeState {
    private int currentTerm = 0;         // Current term number
    private String votedFor = null;      // Candidate voted for in this term
    private NodeRole role = NodeRole.FOLLOWER; // Initial role

    // Thread-safe getters/setters
    public synchronized int getCurrentTerm() {
        return currentTerm;
    }

    public synchronized void setCurrentTerm(int currentTerm) {
        this.currentTerm = currentTerm;
    }

    public synchronized String getVotedFor() {
        return votedFor;
    }

    public synchronized void setVotedFor(String votedFor) {
        this.votedFor = votedFor;
    }

    public synchronized NodeRole getRole() {
        return role;
    }

    public synchronized void setRole(NodeRole role) {
        this.role = role;
    }

    @Override
    public synchronized String toString() {
        return "NodeState{" +
                "term=" + currentTerm +
                ", votedFor='" + votedFor + '\'' +
                ", role=" + role +
                '}';
    }
}
