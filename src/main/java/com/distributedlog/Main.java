package com.distributedlog;

import com.distributedlog.messages.AppendEntries;
import com.distributedlog.messages.RequestVote;
import com.distributedlog.network.MessageClient;
import com.distributedlog.network.MessageServer;
import com.distributedlog.node.*;

import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<Integer> nodePorts = Arrays.asList(5001, 5002, 5003);

        for (int port : nodePorts) {
            /**Detremine peer nodes except this node**/
            List<Integer> peers = nodePorts.stream().filter(p -> p != port).toList();

            /**Creates nodetimer, electionmanager and nodestate for each node**/
            NodeState nodeState = new NodeState("Node" + port);
            ElectionManager electionManager = new ElectionManager(nodeState, port, peers);
            NodeTimers nodeTimers = new NodeTimers(nodeState, electionManager);

            /**Start message server for the node**/
            MessageServer server = new MessageServer(port, nodeState, nodeTimers);
            new Thread(server).start();

            /**Start election time for the node**/
            nodeTimers.startElectionTimer();

            /**Small delays to avoid startup collisions**/

            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }

        /**Simulates external messages**/

        try {
            Thread.sleep(2000); // wait for servers to start
            RequestVote vote = new RequestVote(1, "NodeA");
            AppendEntries append = new AppendEntries(1, "Leader1");

            for (int port : nodePorts) {
                MessageClient.sendMessage("localhost", port, vote);
                MessageClient.sendMessage("localhost", port, append);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
