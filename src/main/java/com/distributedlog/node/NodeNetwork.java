package com.distributedlog.node;

import com.distributedlog.messages.AppendEntries;
import com.distributedlog.network.NodeService;

import java.util.HashMap;
import java.util.Map;

public class NodeNetwork {
    // Simulate network: map nodeId -> NodeService instance
    private static final Map<String, NodeService> nodes = new HashMap<>();

    public static void registerNode(String nodeId, NodeService nodeService) {
        nodes.put(nodeId, nodeService);
    }

    public static boolean sendAppendEntries(String targetNodeId, AppendEntries ae) {
        NodeService node = nodes.get(targetNodeId);
        if (node == null) return false;
        return node.receiveAppendEntries(ae);
    }

    public static boolean sendRequestVote(String targetNodeId, String candidateId, int term) {
        NodeService node = nodes.get(targetNodeId);
        if (node == null) return false;
        return node.receiveVoteRequest(candidateId, term);
    }
}
