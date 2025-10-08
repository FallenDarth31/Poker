import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;

import java.awt.*;

public class ClientController {

    public javafx.scene.control.TextField IPIn;

    String IP = "192.168.0";

    Client client = new Client();

    @FXML
    public void getIP(){
        IP = IPIn.getText();
        System.out.println(IP);
    }

    @FXML
    public void TestConnection() {
        client.start(IP);

    }

}
