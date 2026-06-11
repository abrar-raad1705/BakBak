package com.bakbak.javafx_proj_1_2;

import com.bakbak.javafx_proj_1_2.controller.ChatController;
import com.bakbak.javafx_proj_1_2.controller.LoginController;
import com.bakbak.javafx_proj_1_2.controller.ServerDiscoveryController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

import java.io.IOException;
import javafx.scene.control.Label;
import javafx.stage.Popup;

public class ChatApplication extends Application
{
    private static Stage primaryStage;
    private static ChatClient chatClient;
    private static boolean darkModeEnabled = false;
    private static Scene mainScene;
    private static double currentWidth = 900;
    private static double currentHeight = 800;

    public static boolean isDarkModeEnabled() {
        return darkModeEnabled;
    }

    public static void setDarkModeEnabled(boolean enabled) {
        darkModeEnabled = enabled;
    }

	@Override
	public void start(Stage stage) throws Exception
	{
		primaryStage = stage;
		
		// Set default initial sizes to keep window consistent
		primaryStage.setWidth(900);
		primaryStage.setHeight(800);
		currentWidth = 900;
		currentHeight = 800;
		
		// Start with server discovery instead of automatically connecting
		showServerDiscoveryView();
	}

	public static void showServerDiscoveryView() throws IOException {
		FXMLLoader loader = new FXMLLoader(ChatApplication.class.getResource("fxml/ServerDiscovery.fxml"));
		Parent root = loader.load();

		ServerDiscoveryController controller = loader.getController();

		primaryStage.setTitle("BakBak - Server Discovery");
		
		transitionTo(root, null);
		
		primaryStage.setMinWidth(750);
		primaryStage.setMinHeight(700);
		primaryStage.show();
	}

	public static void showLoginView() throws IOException {
		FXMLLoader loader = new FXMLLoader(ChatApplication.class.getResource("fxml/Login.fxml"));
		Parent root = loader.load();

		LoginController controller = loader.getController();

		primaryStage.setTitle("BakBak - Login");
		
		transitionTo(root, null);
		
		primaryStage.setMinWidth(750);
		primaryStage.setMinHeight(700);
		primaryStage.show();
	}

	public static void showChatView(String username) throws IOException {
		FXMLLoader loader = new FXMLLoader(ChatApplication.class.getResource("fxml/ChatWindow.fxml"));
		Parent root = loader.load();

		ChatController controller = loader.getController();
		controller.setCurrentUsername(username);

		primaryStage.setTitle("BakBak - " + username);
		
		transitionTo(root, ChatApplication.class.getResource("fxml/ChatWindowStyle.css").toExternalForm());
		
		primaryStage.setMinWidth(750);
		primaryStage.setMinHeight(700);
		primaryStage.show();

		primaryStage.setOnCloseRequest(event -> {
			if (chatClient != null) {
				chatClient.disconnect();
			}
			System.exit(0);
		});
	}

	private static void saveCurrentWindowSize() {
		if (primaryStage != null && primaryStage.isShowing()) {
			currentWidth = primaryStage.getWidth();
			currentHeight = primaryStage.getHeight();
		}
	}

	private static void applyCurrentWindowSize() {
		if (primaryStage != null) {
			primaryStage.setWidth(currentWidth);
			primaryStage.setHeight(currentHeight);
		}
	}

	private static void transitionTo(Parent newRoot, String stylesheetUrl) {
		saveCurrentWindowSize();
		
		if (darkModeEnabled) {
			if (!newRoot.getStyleClass().contains("dark-mode")) {
				newRoot.getStyleClass().add("dark-mode");
			}
		} else {
			newRoot.getStyleClass().remove("dark-mode");
		}

		if (mainScene == null) {
			mainScene = new Scene(newRoot, currentWidth, currentHeight);
			if (stylesheetUrl != null) {
				mainScene.getStylesheets().add(stylesheetUrl);
			}
			primaryStage.setScene(mainScene);
		} else {
			Parent oldRoot = mainScene.getRoot();
			
			FadeTransition fadeOut = new FadeTransition(Duration.millis(200), oldRoot);
			fadeOut.setFromValue(1.0);
			fadeOut.setToValue(0.0);
			fadeOut.setOnFinished(e -> {
				mainScene.getStylesheets().clear();
				if (stylesheetUrl != null) {
					mainScene.getStylesheets().add(stylesheetUrl);
				}
				mainScene.setRoot(newRoot);
				newRoot.setOpacity(0.0);
				
				FadeTransition fadeIn = new FadeTransition(Duration.millis(200), newRoot);
				fadeIn.setFromValue(0.0);
				fadeIn.setToValue(1.0);
				fadeIn.play();
			});
			fadeOut.play();
		}
		
		if (mainScene != null) {
			if (darkModeEnabled) {
				mainScene.setFill(javafx.scene.paint.Color.web("#0e1621"));
			} else {
				mainScene.setFill(javafx.scene.paint.Color.web("#eef2f7"));
			}
		}
		
		applyCurrentWindowSize();
	}

	public static void setChatClient(ChatClient client) {
		chatClient = client;
	}

	public static ChatClient getChatClient() {
		return chatClient;
	}

	public static Stage getPrimaryStage() {
		return primaryStage;
	}

	public static void showToast(String message) {
		if (primaryStage == null || !primaryStage.isShowing()) {
			return;
		}
		
		// Run on JavaFX application thread
		javafx.application.Platform.runLater(() -> {
			try {
				Popup popup = new Popup();
				popup.setAutoHide(true);
				
				Label label = new Label(message);
				label.setWrapText(true);
				label.setMaxWidth(350);
				label.setStyle(
					"-fx-background-color: rgba(30, 30, 30, 0.9);" +
					"-fx-text-fill: #ffffff;" +
					"-fx-padding: 10 16 10 16;" +
					"-fx-background-radius: 18;" +
					"-fx-font-size: 13px;" +
					"-fx-font-family: 'Segoe UI', system-ui, sans-serif;" +
					"-fx-alignment: center;" +
					"-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 8, 0, 0, 3);"
				);
				
				popup.getContent().add(label);
				
				// Calculate position
				popup.setOnShown(e -> {
					popup.setX(primaryStage.getX() + (primaryStage.getWidth() - popup.getWidth()) / 2);
					popup.setY(primaryStage.getY() + primaryStage.getHeight() - 120);
				});
				
				popup.show(primaryStage);
				
				// Fade out transition after 2.5 seconds
				FadeTransition fadeOut = new FadeTransition(Duration.millis(400), label);
				fadeOut.setFromValue(1.0);
				fadeOut.setToValue(0.0);
				fadeOut.setDelay(Duration.seconds(2.5));
				fadeOut.setOnFinished(e -> popup.hide());
				fadeOut.play();
			} catch (Exception e) {
				System.err.println("Failed to display toast: " + e.getMessage());
			}
		});
	}
}