package com.distributedlog.network;

import com.distributedlog.messages.AppendEntries;
import com.distributedlog.node.NodeState;

public class NodeService {
    private final NodeState nodeState;

    public NodeService(NodeState nodeState) {
        this.nodeState = nodeState;
    }

    // Called by leader
    public boolean receiveAppendEntries(AppendEntries ae) {
        return nodeState.appendEntriesWithConsistency(ae);
    }

    // Called by candidate for vote
    public boolean receiveVoteRequest(String candidateId, int term) {
        synchronized (nodeState) {
            if (term < nodeState.getCurrentTerm()) return false;
            if (nodeState.getVotedFor() == null || nodeState.getVotedFor().equals(candidateId)) {
                nodeState.setVotedFor(candidateId);
                nodeState.setCurrentTerm(term);
                return true;
            }
            return false;
        }
    }
}
