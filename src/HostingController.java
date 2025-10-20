import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.List;

public class HostingController {

    @FXML
    private TextArea ServerIP;
    @FXML
    private Button ConnectButton;
    public List<String> Players = Collections.synchronizedList(new ArrayList<>());


    @FXML
    public void initialize() {
        try {
            InetAddress myIP = InetAddress.getLocalHost();
            System.out.println(myIP.getHostAddress());
            ServerIP.setText("Server IP: " + myIP.getHostAddress());

        } catch (UnknownHostException e) {
            ServerIP.setText("Server IP: Unknown");
        }
    }


    @FXML
    private void Host() throws IOException {
        System.out.println("Host Game clicked!");
        Hosting H = new Hosting();
        H.setPlayersList(Players);
        H.Start();
        System.out.println("Working");

        Players = H.Players;



    }


}
