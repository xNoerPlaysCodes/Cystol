package net.noerlol.cystolchat.server.connection;

import net.noerlol.cystolchat.common.Convertor;
import net.noerlol.cystolchat.common.Message;
import net.noerlol.cystolchat.common.User;
import net.noerlol.cystolchat.server.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientManager implements Runnable {
    private Socket socket;
    private PrintWriter out;

    public ClientManager(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)/*;*/
        ) {
            this.out = out;
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                if ("exit".equalsIgnoreCase(inputLine)) {
                    break;
                }

                if (!inputLine.contains("|")) {
                    // Skip processing messages without "|" (Non Protocol)
                    continue;
                }

                String displayedMessage = Convertor.decodeMessage(inputLine).getMessage();
                String username = Convertor.decodeMessage(inputLine).getUser().getUsername();
                String type = Convertor.decodeMessage(inputLine).getType().name();
                String format = "--------\n" +
                        "user: " + username + "\n" +
                        "message:\n" +
                        "----\n" +
                        displayedMessage + "\n" +
                        "----\n" +
                        "type: " + type + "\n" +
                        "time: " + System.currentTimeMillis() + "\n--------";
                System.out.println(format);
                Server.broadcast(inputLine, this);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public void sendMessage(String msg) {
        if (out != null) {
            out.println(msg);
        }
    }
}
