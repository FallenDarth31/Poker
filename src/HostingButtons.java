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

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

    }

    @FXML
    private void handleJoinGame() throws IOException {
        Stage primaryStage = new Stage();
        primaryStage.setTitle("Join Game");

        Parent root = FXMLLoader.load(getClass().getResource("ClientWindow.fxml"));
        Scene scene = new Scene(root, 800, 600);

        primaryStage.setScene(scene);
        primaryStage.setWidth(800);
        primaryStage.setHeight(600);
        primaryStage.show();
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
    }
}