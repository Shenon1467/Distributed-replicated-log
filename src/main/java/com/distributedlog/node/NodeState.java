package com.distributedlog.node;

public class NodeState {
    private int currentTerm = 0;
    private String votedFor = null;
    private NodeRole role = NodeRole.FOLLOWER;

    public synchronized int getCurrentTerm() { return currentTerm; }

    public synchronized void setCurrentTerm(int term) { this.currentTerm = term; }  // <-- add this

    public synchronized void incrementTerm() { currentTerm++; }

    public synchronized String getVotedFor() { return votedFor; }

    public synchronized void setVotedFor(String votedFor) { this.votedFor = votedFor; }

    public synchronized NodeRole getRole() { return role; }

    public synchronized void setRole(NodeRole role) { this.role = role; }

    @Override
    public String toString() {
        return "NodeState{" +
                "term=" + currentTerm +
                ", votedFor='" + votedFor + '\'' +
                ", role=" + role +
                '}';
    }
}
