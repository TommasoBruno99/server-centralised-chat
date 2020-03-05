import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ScatteringByteChannel;
import java.util.Arrays;

public class ChatClient {

    static JList<String> users = new JList<>();
    static JFrame chatWindow = new JFrame("Chat Application");
    static JTextArea chatArea = new JTextArea();
    static JTextField textField = new JTextField(40);
    static JButton sendButton = new JButton("Send");
    static BufferedReader in;
    static PrintWriter out;
    static JLabel nameLabel = new JLabel("");
    static JLabel countdownLabel = new JLabel("");
    static JLabel onlineLabel = new JLabel("");
    static String ipAddress;
    static String username;
    static String name = "";
    static DefaultListModel<String> defaultListModel = new DefaultListModel<>();

    ChatClient() {
        chatWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        chatArea.addKeyListener(new ExitListener());
        textField.addKeyListener(new ExitListener());
        users.addKeyListener(new ExitListener());

        chatWindow.setSize(new Dimension(700, 600));

        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setPreferredSize(new Dimension(400, 570));

        textField.setBackground(new Color(153, 170, 181));
        textField.setForeground(Color.WHITE);
        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.add(textField, BorderLayout.CENTER);
        chatArea.setBorder(BorderFactory.createEmptyBorder());
        textPanel.add(sendButton, BorderLayout.EAST);
        textPanel.setPreferredSize(new Dimension(400, 30));

        nameLabel.setPreferredSize(new Dimension(400, 20));
        chatArea.setForeground(Color.WHITE);
        chatArea.setBackground(new Color(44, 47, 51));
        chatPanel.add(nameLabel, BorderLayout.NORTH);
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        chatPanel.add(textPanel, BorderLayout.SOUTH);

        JPanel usersPanel = new JPanel(new BorderLayout());
        usersPanel.setPreferredSize(new Dimension(100, 600));
        usersPanel.setBackground(new Color(35, 39, 42));
        usersPanel.setForeground(Color.WHITE);
        usersPanel.add(users, BorderLayout.CENTER);
        users.setForeground(Color.WHITE);
        users.setBackground(new Color(35, 39, 42));
        users.setOpaque(false);

        countdownLabel.setForeground(Color.WHITE);
        countdownLabel.setHorizontalAlignment(JLabel.CENTER);
        countdownLabel.setPreferredSize(new Dimension(100, 30));
        countdownLabel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.WHITE));
        usersPanel.add(countdownLabel, BorderLayout.SOUTH);

        onlineLabel.setForeground(Color.WHITE);
        onlineLabel.setPreferredSize(new Dimension(100, 20));
        onlineLabel.setHorizontalAlignment(JLabel.CENTER);
        onlineLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.WHITE));
        usersPanel.add(onlineLabel, BorderLayout.NORTH);


        chatWindow.add(usersPanel, BorderLayout.EAST);

        chatWindow.add(chatPanel);
        chatWindow.setVisible(true);
        textField.setEditable(false);
        chatWindow.setLocationRelativeTo(null);
        chatArea.setEditable(false);

        try {
            //create the font to use. Specify the size!
            Font customFont = Font.createFont(Font.TRUETYPE_FONT, new File("fonts/Avenir.ttf")).deriveFont(13f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            //register the font
            ge.registerFont(customFont);
            chatArea.setFont(customFont);
            onlineLabel.setFont(customFont);
            users.setFont(customFont);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }

        sendButton.addActionListener(new Listener());
        textField.addActionListener(new Listener());
    }

    public static void main(String[] args) {

        ChatClient client;
        client = new ChatClient();
        try {

            client.startChat();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    void startChat() throws Exception {

        ipAddress = JOptionPane.showInputDialog(
                chatWindow,
                "Enter IP Address:",
                "IP Address Required!",
                JOptionPane.PLAIN_MESSAGE);
        Socket soc = null;
        try {
            soc = new Socket(ipAddress, 9806);
        } catch(SocketException e) {
            e.printStackTrace();
        }
        assert soc != null;
        in = new BufferedReader(new InputStreamReader(soc.getInputStream()));
        out = new PrintWriter(soc.getOutputStream(), true);

        Thread newThread = new Thread(() -> {
            while (true) {
                String str = null;
                try {
                    str = in.readLine();
                } catch (IOException e) {
                    new ChatServer();
                    e.printStackTrace();
                }
                assert str != null;
                if (str.equals("NAMEREQUIRED")) {

                    while (name.equals("") && !name.matches("^[a-zA-Z0-9]+$")) {
                        name = JOptionPane.showInputDialog(
                                chatWindow,
                                "Enter an unique name:",
                                "Name required!",
                                JOptionPane.PLAIN_MESSAGE);
                        if (!name.matches("^[a-zA-Z0-9]+$")) {
                            JOptionPane.showMessageDialog(chatWindow,
                                    "Name must contain only alphanumeric characters",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                            name = "";
                        }

                    }
                    username = name;
                    out.println(name);
                } else if (str.equals("NAMEALREADYEXISTS") ) {
                    String name = "";
                    while (name.equals("") && !name.matches("^[a-zA-Z0-9]+$")) {
                        name = JOptionPane.showInputDialog(
                                chatWindow,
                                "Enter a different name:",
                                "Name already exists!",
                                JOptionPane.WARNING_MESSAGE);

                        if (!name.matches("^[a-zA-Z0-9]+$")) {
                            JOptionPane.showMessageDialog(chatWindow,
                                    "Name must contain only alphanumeric characters",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                            name = "";
                        }
                    }
                    username = name;
                    out.println(name);
                } else if (str.startsWith("NAMEACCEPTED")) {
                    textField.setEditable(true);
                    String name = str.substring(12);
                    nameLabel.setText("You are logged in as: " + name + "\n");
                } else if (str.equals("PING")) {
                    Thread pingThread = new Thread(() -> {
                        String name = nameLabel.getText().substring(22);
                        System.out.println(nameLabel.getText());
                        out.println("PONG" + name);
                        System.out.println(name + "IS SENDING A PONG");
                    });
                    pingThread.start();

                } else if (str.startsWith("//")) {
                    String finalStr = str;
                    Thread listThread = new Thread(() -> {
                        String[] arrayNames = finalStr.substring(2).split(",");
                        System.out.println(Arrays.asList(arrayNames));

                        arrayNames[0] = "@" + arrayNames[0];

                        defaultListModel.clear();
                        defaultListModel.addAll(Arrays.asList(arrayNames));
                        users.setModel(defaultListModel);
                    });
                    listThread.start();

                } else if (str.startsWith("NEWADMIN")) {
                    System.out.println("Changing admin");
                    chatArea.append(">> [Previous admin " + str.substring(8).split("/")[0] +
                            " has disconnected, new admin is: " + str.substring(8).split("/")[1] + "] <<" + "\n");
                } else if (str.equals("FIRST")) {
                    JOptionPane.showMessageDialog(chatWindow, "You are the first user in the chat");
                } else if (str.startsWith("OFFLINE")) {
                    chatArea.append(">> [" + str.substring(7) + " is offline] <<" + "\n" + "");
                    defaultListModel.removeElement(str.substring(7));
                    users.setModel(defaultListModel);
                } else if (str.startsWith("COUNT")) {
                    countdownLabel.setText("Update in " + str.substring(5));
                } else if (str.startsWith("SIZE")) {

                    onlineLabel.setText("Online: " + str.substring(4) + "/ 20");
                    System.out.println("Total users: " + str.substring(4));
                } else if (str.startsWith("FULL")) {
                    String[] options = {"Exit"};
                    int select = JOptionPane.showOptionDialog(chatWindow, "What do you want to do?", "Server is full", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, null);
                    if (select == 0) {
                        System.exit(0);
                    }
                } else if (str.startsWith("NICKUPDATED")) {
                    ChatClient.nameLabel.setText("You are logged in as: " + str.substring(11).split("/")[0] + "\n");
                    ChatClient.chatArea.append(">> [Your nickname is now: " + str.substring(11).split("/")[0] + "] << " + "\n");

                } else if (str.startsWith("NICKNAME")) {
                    String[] params = str.substring(8).split("/");

                    String previous = params[1];
                    String newName = params[0];

                    int index = defaultListModel.indexOf(previous);
                    if (index != -1) {
                        defaultListModel.setElementAt(newName, index);
                        users.setModel(defaultListModel);
                    }
                    ChatClient.chatArea.append(">> [" + previous + " has changed its nickname to: " + newName + "] <<" + "\n");
                } else if (str.startsWith("BACKONLINE")) {
                    int index = defaultListModel.indexOf(str.substring(10) + "(A)");
                    if (index == -1) {
                        index = defaultListModel.indexOf("@" + str.substring(10) + "(A)");
                        defaultListModel.setElementAt("@" + str.substring(10), index);
                    } else defaultListModel.setElementAt(str.substring(10), index);
                    users.setModel(defaultListModel);
                    chatArea.append(">> [" + str.substring(10) + " is now back from being away] <<" + "\n");
                } else if (str.startsWith("AFK")) {
                    int index = defaultListModel.indexOf(str.substring(3));
                    if (index == -1) {
                        index = defaultListModel.indexOf("@" + str.substring(3));
                        defaultListModel.setElementAt("@" + str.substring(3) + "(A)", index);
                    } else defaultListModel.setElementAt(str.substring(3) + "(A)", index);
                    users.setModel(defaultListModel);
                }
                else {
                    chatArea.append(str + "\n");
                }
            }
        });
        newThread.start();

    }
}

class Listener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
        if (ChatClient.textField.getText().startsWith("/")) {
            if (ChatClient.textField.getText().substring(1).trim().equals("clear")) {
                ChatClient.chatArea.setText("");
            } else if (ChatClient.textField.getText().substring(1).equals("help")) {

                ChatClient.chatArea.append("-----------------" + "\n" + "/help - to access the various list of command " + "\n" +
                        "/clear - to clear the chat client side /whois - to get information about an user " + "\n" +
                        "/admin - to give admin to another member " + "\n" +
                        "/quit - to quit the chat " + "\n" +
                        "/msg - to send a private message to a member " + "\n" +
                        "/nickname - to change nickname " + "\n" +
                        "/away - to set status away " + "\n" +
                        "/online - to come online after being away " + "\n" +
                        "/credits - to see application credits and creators " + "\n" +
                        "/info - to see personal IP and PORT " + "\n" +
                        "/register - to register your nickname " + "\n" +
                        "/identify - to identify your previous registereld nickname " + "\n" +
                        "/logs - open logs client side " + "\n" +
                        "/clearlogs - clear logs client side" + "\n" + "-----------------" + "\n");
            }

            else if (ChatClient.textField.getText().substring(1).equals("credits")) {
                ChatClient.chatArea.append("MIT Copyright " + "\n" + "Copyright (c) 2020 Alessandro Buonerba & Tommaso Bruno" + "\n");

            }

            else if (ChatClient.textField.getText().substring(1).equals("quit")) {
                System.exit(0);
            }

            else if (ChatClient.textField.getText().substring(1).startsWith("whois")) {
                if (ChatClient.textField.getText().substring(6).trim().length() > 0) {
                    String param = ChatClient.textField.getText().substring(6);
                    System.out.println(param);
                    ChatClient.out.println("WHOIS" + ChatClient.nameLabel.getText().substring(22) + "/" + param);
                } else  ChatClient.chatArea.append(">> [The command whois required a param] <<" + "\n");
            }


            else if (ChatClient.textField.getText().substring(1).startsWith("msg")) {
                String[] param = ChatClient.textField.getText().substring(5).split(" ");

                System.out.println(param[0]);
                if (param[0].equals(ChatClient.name)) {
                    ChatClient.chatArea.append(">> [You can't send a message to yourself] <<" + "\n");
                } else {
                    param[1] = ChatClient.textField.getText().substring(7 + param[0].length() - 1);
                    System.out.println(param[0] + " " + param[1]);
                    ChatClient.out.println("WHISPER" + ChatClient.name + "/" + param[0] + "/" + param[1]);
                    //System.out.println(ChatClient.nameLabel.getText().substring(22) + " is sending a message to " + param[1] + ". Message is: " + param[2]);
                }
            }

            else if (ChatClient.textField.getText().substring(1).startsWith("nickname")) {
                String name = ChatClient.textField.getText().substring(10);
                System.out.println(name);
                if (!name.matches("^[a-zA-Z0-9]+$")) {
                    ChatClient.chatArea.append(">> [Name must contain only alphanumeric characters] <<" + "\n");
                } else {
                    String param = ChatClient.textField.getText().substring(10);
                    System.out.println(param);
                    ChatClient.out.println("NICKCHANGE" + param + "/" + ChatClient.name);
                }
            }

            else if(ChatClient.textField.getText().substring(1).startsWith("away")) {
                String reason =  ChatClient.textField.getText().substring(6);
                ChatClient.out.println("AWAY" + reason);
            }

            else if (ChatClient.textField.getText().substring(1).startsWith("online")) {
                ChatClient.out.println("GOONLINE" + ChatClient.name);
            }

            else if (ChatClient.textField.getText().substring(1).startsWith("clearlogs")) {
                String admin = ChatClient.defaultListModel.get(0).substring(1);
                System.out.println("Admin user = " + admin);
                if (!ChatClient.name.equals(admin)) {
                    ChatClient.chatArea.append(">> [You are not an admin, can't clear the logs] <<" + "\n");
                } else {
                    ChatClient.out.println("CLEARLOGS");
                    ChatClient.chatArea.append(">> [Logs have been cleared] <<" + "\n");
                }
            }

            else if (ChatClient.textField.getText().substring(1).trim().equals("info")) {
                ChatClient.out.println("INFO" + ChatClient.name);
            } else ChatClient.chatArea.append("Command not found, type " +
                    "/help to see all the commands" + "\n");
        }

        else ChatClient.out.println(ChatClient.textField.getText());
        ChatClient.textField.setText("");
    }
}

