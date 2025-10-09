package com.distributedlog.node;

import com.distributedlog.messages.AppendEntries;

import java.util.List;

public class LeaderService {
    private final NodeState nodeState;
    private final List<String> otherNodeIds;

    public LeaderService(NodeState nodeState, List<String> allNodeIds) {
        this.nodeState = nodeState;
        this.otherNodeIds = allNodeIds.stream()
                .filter(id -> !id.equals(nodeState.getNodeId()))
                .toList();
        nodeState.initLeaderState(allNodeIds);
    }

    public void replicateLog() {
        int term = nodeState.getCurrentTerm();
        int leaderCommit = nodeState.getCommitIndex();
        for (String nodeId : otherNodeIds) {
            int nextIndex = nodeState.getNextIndex(nodeId);
            List<String> entries = nodeState.getCommandsFromTo(nextIndex, nodeState.getLastLogIndex());

            AppendEntries ae = new AppendEntries(
                    term,
                    nodeState.getNodeId(),
                    nextIndex - 1,
                    nodeState.getTermAtIndex(nextIndex - 1),
                    entries,
                    leaderCommit
            );

            boolean success = NodeNetwork.sendAppendEntries(nodeId, ae);
            if (success) {
                nodeState.updateFollowerProgress(nodeId, nextIndex + entries.size() - 1);
            }
        }
    }
}
