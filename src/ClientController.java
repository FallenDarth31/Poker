import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

import java.awt.*;
import java.io.*;
import java.net.*;

// Controller for ClientWindow.fxml
// Allows the user to scan for servers, select one, enter a username, and connect
public class ClientController {

    public javafx.scene.control.TextField IPIn;
    @FXML
    public javafx.scene.control.TextArea DeviceIPArea;
    public javafx.scene.control.ComboBox serverList;
    @FXML
    public javafx.scene.control.TextField UsernameIN;

    private Socket currentSocket;
    private PrintWriter currentOut;
    private BufferedReader currentIn;

    String IP = "192.168.0"; // Default subnet to scan

    // Runs on startup — detects the device's local IP and sets the default scan subnet
    @FXML
    public void initialize() {
        try {
            InetAddress myIP = InetAddress.getLocalHost();
            System.out.println(myIP.getHostAddress());
            IP = First3NumbersOfIP(myIP.getHostAddress());
            DeviceIPArea.setText("Device IP: " + myIP.getHostAddress());
        } catch (UnknownHostException e) {
            DeviceIPArea.setText("Device IP: Unknown");
        }
    }

    Client client = new Client();

    // Updates the subnet to scan based on what the user typed in the IP text field
    @FXML
    public void getIP(){
        IP = IPIn.getText();
        System.out.println(IP);
    }

    // Runs the network scan on a background thread to avoid freezing the UI,
    // then populates the server dropdown with any servers found
    @FXML
    public void TestConnection() {
        new Thread(() -> {
            client.start(IP);

            // Update the ComboBox on the JavaFX thread once the scan is complete
            javafx.application.Platform.runLater(() -> {
                ObservableList<String> servers = FXCollections.observableArrayList(client.Servers);
                serverList.setItems(servers);
            });
        }).start();
    }

    // Extracts the first three octets from an IP address string
    // e.g. "192.168.0.105" returns "192.168.0"
    public static String First3NumbersOfIP(String ip) {
        if (ip == null) return null;
        int from = 0;
        int dotIndex = -1;
        for (int i = 0; i < 3; i++) {
            dotIndex = ip.indexOf('.', from);
            if (dotIndex == -1) {
                return ip;
            }
            from = dotIndex + 1;
        }
        return ip.substring(0, dotIndex);
    }

    // Connects to the selected server, sends the username, then opens the lobby window
    @FXML
    public void Connect() throws IOException {
        String ServerIP = serverList.getValue().toString();

        if (ServerIP != null) {
            if (!UsernameIN.getText().equals("") && !UsernameIN.getText().equals("Enter the username:")) {
                String Username = UsernameIN.getText();
                try {
                    currentSocket = new Socket();
                    SocketAddress address = new InetSocketAddress(ServerIP, 9000);
                    currentSocket.connect(address, 100);

                    System.out.println("Connected to " + ServerIP);

                    currentOut = new PrintWriter(currentSocket.getOutputStream(), true);
                    currentIn = new BufferedReader(new InputStreamReader(currentSocket.getInputStream()));

                    // The first message sent is the username so the server can register the player
                    currentOut.println(Username);

                    FXMLLoader loader = new FXMLLoader(getClass().getResource("LobbyWindow.fxml"));
                    Parent root = loader.load();

                    // Open the lobby in the same screen position as the current window
                    Stage currentStage = (Stage) UsernameIN.getScene().getWindow();
                    double x = currentStage.getX();
                    double y = currentStage.getY();

                    Stage Lobby = new Stage();
                    Lobby.setTitle("Lobby");
                    Lobby.setScene(new Scene(root, 1000, 700));
                    Lobby.setWidth(1000);
                    Lobby.setHeight(700);
                    Lobby.setMinWidth(1000);
                    Lobby.setMinHeight(700);

                    LobbyController lobbyController = loader.getController();
                    lobbyController.PassVariables(currentSocket, Username, Lobby);

                    Lobby.setX(x);
                    Lobby.setY(y);
                    Lobby.toFront();
                    Lobby.requestFocus();

                    Lobby.show();
                    currentStage.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                UsernameIN.setPromptText("**Enter a username**");
            }
        } else {
            // No server selected — could show an alert here
        }
    }
}