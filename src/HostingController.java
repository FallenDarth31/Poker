import javafx.application.Platform;
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

    Hosting H = new Hosting();

    @FXML
    private TextArea UsernameList;
    @FXML
    private TextArea ServerIP;
    @FXML
    private Button ConnectButton;
    
    public List<String> Players = Collections.synchronizedList(new ArrayList<>());

    @FXML
    public void initialize() {

        new Thread(()-> {
            System.out.println("Working");
            while (true) {
                StringBuilder UsernameBox = new StringBuilder();

                synchronized (H.Usernames) {
                    for (int i = 0; i < H.Usernames.size(); i++) {
                        UsernameBox.append(H.Usernames.get(i)).append("\n");
                    }
                }

                String finalText = UsernameBox.toString();
                Platform.runLater(() -> {
                    UsernameList.setText(finalText);
                });

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }).start();
    }


    @FXML
    private void Host() throws IOException {
        System.out.println("Host Game clicked!");
        H.setPlayersList(Players);
        H.Start();
        System.out.println("Working");

        Players = H.Players;
    }

    @FXML
    private void StartGame() {
        try {
            H.broadcastMessage("StartingGame");
        } catch (Exception e) {
            System.out.println("Broadcast Error (1)");
        }

    }

}
