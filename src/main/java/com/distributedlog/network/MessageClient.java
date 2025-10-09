package com.distributedlog.network;

import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;

public class MessageClient {
    private static final Gson gson = new Gson();

    /**
     * Send a message (object will be converted to JSON) and return the raw JSON response (or null on error).
     */
    public static String sendMessageWithResponse(String host, int port, Object message) {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            String json = gson.toJson(message);
            out.println(json);

            // read single-line response JSON (server will reply)
            String response = in.readLine();
            System.out.println("[Client] Sent message: " + json);
            System.out.println("[Client] Received response: " + response);
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
