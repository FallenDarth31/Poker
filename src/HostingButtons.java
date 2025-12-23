import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HostingButtons {

    @FXML
    private void handleHostGame() throws IOException {
        Stage primaryStage = new Stage();
        Parent root = FXMLLoader.load(getClass().getResource("HostingWindow.fxml"));

        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);

    }

    @FXML
    private void handleJoinGame() throws IOException {
        Stage primaryStage = new Stage();
        primaryStage.setTitle("Join Game");

        Parent root = FXMLLoader.load(getClass().getResource("ClientWindow.fxml"));
        Scene scene = new Scene(root, 1000, 700);

        primaryStage.setScene(scene);
        primaryStage.setWidth(1000);
        primaryStage.setHeight(700);
        primaryStage.show();
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
    }
}