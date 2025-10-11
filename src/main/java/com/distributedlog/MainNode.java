package com.distributedlog;

import com.distributedlog.node.NodeState;
import com.distributedlog.node.NodeTimers;
import com.distributedlog.node.ElectionManager;
import com.distributedlog.network.MessageServer;

import java.util.Arrays;
import java.util.List;

public class MainNode {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java com.distributedlog.MainNode <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String nodeId = "Node" + port;

        System.out.println("[MainNode] Starting node " + nodeId + " on port " + port);

        // Initialize NodeState
        NodeState nodeState = new NodeState(nodeId);

        // Define peer ports (all other nodes in the cluster)
        List<Integer> peerPorts = Arrays.asList(5001, 5002, 5003);
        peerPorts.remove((Integer) port); // remove self port

        // Initialize ElectionManager for leader election & log replication
        ElectionManager electionManager = new ElectionManager(nodeState, port, peerPorts);

        // Initialize NodeTimers with ElectionManager
        NodeTimers nodeTimers = new NodeTimers(nodeState, electionManager);

        // Start the message server for RPCs and client commands
        Thread serverThread = new Thread(new MessageServer(port, nodeState, nodeTimers));
        serverThread.start();

        System.out.println("[MainNode] Node " + nodeId + " is running. Waiting for messages...");
    }
}
