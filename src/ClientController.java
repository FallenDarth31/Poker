import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;

import java.awt.*;
import java.io.*;
import java.net.*;

public class ClientController {

    public javafx.scene.control.TextField IPIn;
    public javafx.scene.control.TextArea DeviceIPArea;
    public javafx.scene.control.ComboBox serverList;
    @FXML
    public javafx.scene.control.TextField UsernameIN;


    String IP = "192.168.0";

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

    @FXML
    public void getIP(){
        IP = IPIn.getText();
        System.out.println(IP);
    }

    @FXML
    public void TestConnection() {
        new Thread(() -> {
            client.start(IP);

            // Update ComboBox on JavaFX thread after scan is done
            javafx.application.Platform.runLater(() -> {
                ObservableList<String> servers = FXCollections.observableArrayList(client.Servers);
                serverList.setItems(servers);
            });
        }).start();
    }

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

    @FXML
    public void Connect() throws IOException {
        String ServerIP = serverList.getValue().toString();

        if (ServerIP != null) {
            if (!UsernameIN.getText().equals("") && !UsernameIN.getText().equals("Enter the username:")) {
                String Username = UsernameIN.getText();
                try {
                    Socket socket = new Socket();
                    SocketAddress address = new InetSocketAddress(ServerIP, 9000);

                    socket.connect(address, 100);

                    System.out.println("Connected to " + ServerIP);


                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    out.println(Username);



                } catch (Exception e) {

                }
            } else {
                UsernameIN.setPromptText("**Enter a username**");

            }

        } else {

        }

    }


}
