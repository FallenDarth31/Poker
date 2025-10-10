import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;

import java.awt.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ClientController {

    public javafx.scene.control.TextField IPIn;
    public javafx.scene.control.TextArea DeviceIPArea;
    public javafx.scene.control.ComboBox serverList;


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


}
