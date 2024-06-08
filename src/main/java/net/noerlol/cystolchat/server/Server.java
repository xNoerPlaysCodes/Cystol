package net.noerlol.cystolchat.server;

import net.noerlol.cystolchat.common.Message;
import net.noerlol.cystolchat.common.MessageType;
import net.noerlol.cystolchat.common.User;
import net.noerlol.cystolchat.server.connection.ClientManager;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Server {
    private static int PORT = 8080;
    private static Set<ClientManager> cmS = new HashSet<>();
    private static final String VERSION = "1.0.0-rd";
    private static Map<String, Object> config;
    private static boolean VERBOSE;

    public static void main(String[] args) throws IOException {
        System.out.println("Cystol SelfHostable Server [ CC ]");
        {
            if (!Files.exists(Paths.get("config"))) {
                Files.createDirectories(Paths.get("config"));
                Files.createFile(Paths.get("config" + File.separator + "config.yml"));
                config = getConfig("config" + File.separator + "config.yml");
                BufferedWriter writer = new BufferedWriter(new FileWriter(new File("config" + File.separator + "config.yml")));
                writer.write("port: 8080 # Port for Server to host on\n");
                writer.write("verbose: false # verbose, false: off, true: on\n");
                writer.close();
                System.out.println("Config folders setup, check to modify");
            } else {
                config = getConfig("config" + File.separator + "config.yml");
                PORT = getPort();
                VERBOSE = getIsVerbose();

                if (!VERBOSE) {
                    System.out.println("Port: " + PORT);
                    System.out.println("Starting ...");
                } else {
                    System.out.println("Info:");
                    String m = "  Port: " + PORT + "\n" +
                            "  Verbose: " + VERBOSE + "\n" +
                            "  Cystol SHS Version: " + VERSION;
                    System.out.println(m);
                }

                try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                    System.out.println("Server started!");

                    while (true) {
                        Socket clientSocket = serverSocket.accept();
                        ClientManager clientManager = new ClientManager(clientSocket);
                        cmS.add(clientManager);
                        new Thread(clientManager).start();
                    }
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static Map<String, Object> getConfig(String file) {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = new FileInputStream(file)) {
            return yaml.load(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static int getPort() {
        int port = 8080;
        if (config.containsKey("port")) {
            // Get the value associated with the key
            Object value = config.get("port");
            // Check if the value is a string
            if (value instanceof Integer) {
                // Cast the value to a string and return it
                port = (int) value;
                return port;
            } else {
                // Handle if the value is not a string (optional)
                System.err.println("Value for port is not a string.");
                return port;
            }
        } else {
            // Handle if the key does not exist in the YAML map (optional)
            System.err.println("port not found in config file.");
            return port;
        }
    }

    public static boolean getIsVerbose() {
        boolean prefix = true;
        String module = "verbose";
        if (config.containsKey(module)) {
            // Get the value associated with the key
            Object value = config.get(module);
            // Check if the value is a string
            if (value instanceof Boolean) {
                // Cast the value to a string and return it
                prefix = (boolean) value;
                return prefix;
            } else {
                // Handle if the value is not a string (optional)
                System.err.println("Value for " + module + " is not a boolean.");
                return true;
            }
        } else {
            // Handle if the key does not exist in the YAML map (optional)
            System.err.println(module + " not found in config file.");
            return true;
        }
    }

    public static void broadcast(String message, ClientManager sender) {
        for (ClientManager cm : cmS) {
            if (cm != sender) {
                cm.sendMessage(message);
            }
        }
    }
}