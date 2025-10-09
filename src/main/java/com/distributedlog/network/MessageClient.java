package com.distributedlog.network;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;

public class MessageClient {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Send a message object as JSON and return the JSON response
    public static String sendMessage(String host, int port, Object messageObject) {
        try (Socket socket = new Socket(host, port);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Convert object to JSON
            String json = objectMapper.writeValueAsString(messageObject);

            // Send message
            writer.write(json);
            writer.newLine();
            writer.flush();

            // Read response
            return reader.readLine();

        } catch (IOException e) {
            System.err.println("[Client] Connection error to " + host + ":" + port + " -> " + e.getMessage());
            return null;
        }
    }
}
