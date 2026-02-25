import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

// Entry point for the application — loads the main menu window on startup
public class Main extends Application {

    // Called by the JavaFX runtime to set up and display the main window
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("MainWindow.fxml"));
        primaryStage.setTitle("My JavaFX App");
        primaryStage.setScene(new Scene(root, 1000, 700));
        primaryStage.setWidth(1000);
        primaryStage.setHeight(700);
        primaryStage.show();
    }

    // JVM entry point — launches the JavaFX application
    public static void main(String[] args) throws UnknownHostException {
        launch(args);
    }

    // Placeholder method — hosting logic is handled by HostingController and Hosting
    public static void Hosting() throws IOException {
    }
}