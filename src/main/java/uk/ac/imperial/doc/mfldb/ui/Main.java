package uk.ac.imperial.doc.mfldb.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import uk.ac.imperial.doc.mfldb.util.ResourceURLStreamHandlerFactory;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import static uk.ac.imperial.doc.mfldb.ui.Const.*;

public class Main extends Application {

    private MainWindowController controller;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Add our own URL handling for custom URLs in WebView content.
        URL.setURLStreamHandlerFactory(new ResourceURLStreamHandlerFactory());

        FXMLLoader loader = new FXMLLoader(getClass().getResource(MAIN_WINDOW_FXML));
        Parent root = loader.load();

        List<String> args = getParameters().getRaw();
        String cmd = args.stream().collect(Collectors.joining(" "));
        controller = loader.getController();
        controller.setCmd(cmd);

        Scene scene = new Scene(root, MAIN_WINDOW_WIDTH, MAIN_WINDOW_HEIGHT);
        primaryStage.setTitle(MAIN_WINDOW_TITLE);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        controller.ensureEnded();
        super.stop();
    }
}
