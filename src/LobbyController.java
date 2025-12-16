import javafx.fxml.FXML;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class LobbyController {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    private String WaitingFor = "StartingGame";

    public void PassVariables(Socket socket, String username) throws IOException {
        this.socket = socket;
        this.username = username;

        initialise();

    }


    @FXML
    public void initialise() throws IOException {

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        new Thread(() -> {
            String message;
            while (true) {
                try {
                    if ((message = in.readLine()) == null) break;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("Server says: " + message);
                if (message.equals("StartingGame")) {


                }
            }
        }).start();

    }


    public void cleanup() {
    }
}
