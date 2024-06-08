package net.noerlol.cystolchat.client;

import net.noerlol.cystolchat.common.Convertor;
import net.noerlol.cystolchat.common.Message;
import net.noerlol.cystolchat.common.MessageType;
import net.noerlol.cystolchat.common.User;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;

public class Client extends JFrame implements ActionListener {

    private static String SERVER_ADDRESS = "localhost";
    private static int SERVER_PORT = 8080;
    private static String username = "DefaultUsername";

    private final static ImageIcon icon = new ImageIcon("/home/noerlol/Desktop/Assets4Cystol/icon.png");

    private final JTextArea chatArea;
    private final JTextField inputField;
    private final JButton sendButton;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public Client() {
        super.setTitle("Cystol");
        super.setMaximumSize(new Dimension(1280, 720));
        super.setMinimumSize(new Dimension(1280, 720));
        super.setIconImage(icon.getImage());

        // Create chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        // Ensure newest messages are always visible
        DefaultCaret caret = (DefaultCaret) chatArea.getCaret();
        caret.setVisible(true);

        // Create input field
        inputField = new JTextField();

        // Create send button
        sendButton = new JButton();
        sendButton.addActionListener(this);

        // Manipulation
        inputField.setAutoscrolls(true);
        inputField.setMinimumSize(new Dimension(super.getWidth(), (int) (super.getHeight() - (super.getHeight() * 0.4))));
        inputField.setMaximumSize(new Dimension(super.getWidth(), (int) (super.getHeight() - (super.getHeight() * 0.4))));
        inputField.setToolTipText("Send a message");
        inputField.setBackground(new Color(39,35,35));
        inputField.setForeground(Color.WHITE);
        Border lineBorder = new LineBorder(new Color(39, 35, 35));
        Border emptyBorder = new EmptyBorder(5, 5, 5, 5);
        Border compoundBorder = BorderFactory.createCompoundBorder(lineBorder, emptyBorder);
        inputField.setBorder(compoundBorder);

        sendButton.setBorder(BorderFactory.createCompoundBorder(lineBorder, new EmptyBorder(10, 10, 10, 10)));
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendButton.setBackground(new Color(14,13,13));
        sendButton.setForeground(Color.WHITE);
        sendButton.setText("Send");

        chatArea.setMaximumSize(new Dimension((super.getWidth() - 6), 99999));
        chatArea.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        chatArea.setBackground(new Color(14,13,13));
        chatArea.setForeground(Color.WHITE);
        chatArea.setBorder(BorderFactory.createCompoundBorder(lineBorder, new EmptyBorder(15, 15, 15, 15)));

        chatArea.setForeground(Color.WHITE);
        chatArea.setFont(Font.getFont(Font.SANS_SERIF));

        // Layout components
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.add(scrollPane);
        contentPanel.add(inputField);
        contentPanel.add(sendButton);

        contentPanel.setBackground(new Color(14, 13, 13));

        contentPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        getContentPane().add(contentPanel);

        // Connect to server on window close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
                System.exit(0);
            }
        });


        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !(inputField.getText().trim().isEmpty())) {
                    try {
                        sendOwnMessage(inputField.getText().trim());
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });

        // Pack and display the window
        pack();
        setVisible(true);

        showConnectionWindow();
        showUsernameSelectionWindow();

        // Connect to server in a separate thread
        new Thread(this::connectToServer).start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(sendButton)) {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
                try {
                    sendOwnMessage(message);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Start a thread to receive messages from the server
            new Thread(() -> {
                String fromServer;
                try {
                    while ((fromServer = in.readLine()) != null) {
                        Message message = Convertor.decodeMessage(fromServer);
                        String displayedMessage;
                        MessageType type = message.getType();
                        if (type.equals(MessageType.REGULAR)) {
                            displayedMessage = message.getUser().getUsername() + ": " + message.getMessage();
                            SwingUtilities.invokeLater(() -> chatArea.append(displayedMessage + "\n"));
                        } else if (type.equals(MessageType.ANNOUNCEMENT)) {
                            displayedMessage = "[ Announcement ] -> " + message.getMessage();
                            SwingUtilities.invokeLater(() -> chatArea.append(displayedMessage + "\n"));
                        } else if (type.equals(MessageType.BOT)) {
                            displayedMessage = message.getUser().getUsername() + " [BOT]: " + message.getMessage();
                            SwingUtilities.invokeLater(() -> chatArea.append(displayedMessage + "\n"));
                        } else {
                            displayedMessage = message.getUser().getUsername() + ": " + message.getMessage();
                            SwingUtilities.invokeLater(() -> chatArea.append(displayedMessage + "\n"));
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error receiving message: " + e.getMessage());
                    disconnect();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).start();
        } catch (ConnectException e) {
            JOptionPane.showMessageDialog(this, "Failed to connect to server!", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1); // No server found
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        }
    }

    private void disconnect() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    private void sendOwnMessage(String message) throws Exception {
        if (message.length() > 4096) {
            showTooLongMessageError();
            inputField.setText("");
        } else {
            if (message.startsWith("/")) {
                String command = message.split("/")[1];
                String[] commandArgs = command.split(" ");
                if (command.isEmpty()) {
                    inputField.setText("");
                } else if (command.startsWith("announce")) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i < commandArgs.length; i++) {
                        sb.append(commandArgs[i]).append(" ");
                    }

                    String announcement = sb.toString().trim();
                    sendAnnouncement(announcement);
                } else if (command.startsWith("nickname")) {
                    if (commandArgs.length == 2) {
                        username = commandArgs[1];
                        chatArea.append("[Self Message] You have now changed your nickname to '" + username + "'\n");
                        inputField.setText("");
                    } else {
                        chatArea.append("[Self Message] Your nickname is '" + username + "'\n");
                        inputField.setText("");
                    }
                } else if (command.equalsIgnoreCase("clear")) {
                    chatArea.setText("[Self Message] You have cleared the chat (for yourself)\n");
                    inputField.setText("");
                } else if (command.equalsIgnoreCase("help")) {
                    String helpMenu = """
                            [Self Message]
                            [Required Arguments] (Optional Arguments)
                             - /announce [announcement] -> Does an announcement
                             - /nickname (nickname) -> Sets or shows nickname based on arguments
                             - /help -> This menu!
                             - /clear -> Clears chat (for you)
                            """;
                    chatArea.append(helpMenu);
                    inputField.setText("");
                }
            } else {
                String encodedMessage = Convertor.encodeMessage(new Message(message, new User(username), MessageType.REGULAR));
                out.println(encodedMessage);
                inputField.setText("");
                chatArea.append("You: " + message + "\n");
            }
        }
    }

    private void sendAnnouncement(String message) throws Exception {
        if (message.length() > 4096) {
            showTooLongMessageError();
            inputField.setText("");
        } else {
            String encodedMessage = Convertor.encodeMessage(new Message(message, new User(username), MessageType.ANNOUNCEMENT));
            out.println(encodedMessage);
            inputField.setText("");
            chatArea.append("[ Announcement ] -> " + message + "\n");
        }
    }

    private void showConnectionWindow() {
        String ipUnsplit = JOptionPane.showInputDialog(this, "Enter IP of Server", "Enter Details", JOptionPane.QUESTION_MESSAGE);
        String[] ip = ipUnsplit.trim().split(":");
        if (ip.length == 2) {
            SERVER_ADDRESS = ip[0];
            SERVER_PORT = Integer.parseInt(ip[1]);
        } else if (ip.length == 1) {
            SERVER_ADDRESS = ip[0];
            SERVER_PORT = 8080;
        } else if (ipUnsplit.isEmpty()) {
            showConnectionWindow();
        } else {
            showConnectionWindow();
        }
    }

    private void showUsernameSelectionWindow() {
        username = JOptionPane.showInputDialog(this, "Enter your Username", "Enter Username", JOptionPane.QUESTION_MESSAGE);
        if (username.isEmpty()) {
            showUsernameSelectionWindow();
        } else if (username.contains("|")) {
            showUsernameSelectionWindow();
        } else if (username.contains(" ")) {
            showUsernameSelectionWindow();
        }
    }

    private void showTooLongMessageError() {
        JOptionPane.showMessageDialog(this, "Your message was too long for the message limit! (4096)", "Error!", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        new Client();
    }
}
