package com.distributedlog.network;

import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;

public class MessageClient {
    private static final Gson gson = new Gson();

    public static void sendMessage(String host, int port, Object message) {
        try (Socket socket = new Socket(host, port)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            String json = gson.toJson(message);
            out.println(json);
            System.out.println("[Client] Sent message: " + json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
