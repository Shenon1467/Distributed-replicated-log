package com.distributedlog.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientInterface {
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Press enter to connect to server: ");
        String host = scanner.nextLine().trim();
        if (host.isEmpty()) host = "localhost";

        System.out.print("Enter node port (5001, 5002, 5003): ");
        int currentLeaderPort = Integer.parseInt(scanner.nextLine().trim());

        System.out.println("\n Connected to Raft Node " + host + ":" + currentLeaderPort);
        System.out.println("Use below Commands:");
        System.out.println(" 1. Send [Logs]           → send command to the leader following the followers");
        System.out.println(" 2. Leader                → get leader info of this node");
        System.out.println(" 3. Nodes information     → get leader info of all nodes");
        System.out.println(" 4. Exit                  → quit client\n");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println(" Exiting client...");
                break;
            } else if (input.equalsIgnoreCase("leader")) {
                sendMessage(host, currentLeaderPort, gson.toJson(new LeaderQuery()));
            } else if (input.equalsIgnoreCase("nodes information")) {
                for (int port = 5001; port <= 5003; port++) {
                    sendMessage(host, port, gson.toJson(new LeaderQuery()), port);
                }
            } else if (input.startsWith("send ")) {
                String command = input.substring(5).trim();
                ClientCommand msg = new ClientCommand("set", command);
                currentLeaderPort = sendCommandWithRedirect(host, currentLeaderPort, msg);
            } else {
                System.out.println("Unknown command. Use 'send [log]' or 'leader' or 'nodes information'");
            }
        }

        scanner.close();
    }

    // Sends a command and handles redirect automatically
    private static int sendCommandWithRedirect(String host, int port, ClientCommand msg) {
        try {
            String response = sendMessage(host, port, gson.toJson(msg));
            if (response != null) {
                JsonObject json = gson.fromJson(response, JsonObject.class);
                if (json.has("status") && "redirect".equals(json.get("status").getAsString())
                        && json.has("leader")) {
                    String leaderId = json.get("leader").getAsString();
                    int leaderPort = extractPortFromLeaderId(leaderId);
                    System.out.println(" Redirecting to leader " + leaderId + " on port " + leaderPort);
                    return sendCommandWithRedirect(host, leaderPort, msg); // resend
                }
            }
        } catch (Exception e) {
            System.out.println(" Error sending command: " + e.getMessage());
        }
        return port; // return port used (leader or self)
    }

    // Sends a message to a node and returns the response
    private static String sendMessage(String host, int port, String jsonMessage) {
        return sendMessage(host, port, jsonMessage, port);
    }

    private static String sendMessage(String host, int port, String jsonMessage, int nodePort) {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out.println(jsonMessage);
            String response = in.readLine();
            System.out.println(" Node " + nodePort + " -> Response: " + response);
            return response;
        } catch (IOException e) {
            System.out.println(" Could not connect to node " + nodePort + ": " + e.getMessage());
        }
        return null;
    }

    // Convert leaderId like "Leader5002" or "Node5002" to port number
    private static int extractPortFromLeaderId(String leaderId) {
        return Integer.parseInt(leaderId.replaceAll("\\D+", ""));
    }

    static class ClientCommand {
        private final String clientCommand;
        private final String data;

        ClientCommand(String clientCommand, String data) {
            this.clientCommand = clientCommand;
            this.data = data;
        }
    }

    static class LeaderQuery {
        private final boolean getLeader = true;
    }
}
