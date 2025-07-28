package com.bakbak.javafx_proj_1_2;

import com.bakbak.javafx_proj_1_2.controller.ChatController;
import com.bakbak.javafx_proj_1_2.controller.LoginController;
import com.bakbak.javafx_proj_1_2.controller.ServerDiscoveryController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ChatApplication extends Application
{
    private static Stage primaryStage;
    private static ChatClient chatClient;

	@Override
	public void start(Stage stage) throws Exception
	{
		primaryStage = stage;
		
		// Start with server discovery instead of automatically connecting
		showServerDiscoveryView();
	}

	public static void showServerDiscoveryView() throws IOException {
		FXMLLoader loader = new FXMLLoader(ChatApplication.class.getResource("fxml/ServerDiscovery.fxml"));
		Parent root = loader.load();

		ServerDiscoveryController controller = loader.getController();

		Scene scene = new Scene(root);

		primaryStage.setTitle("BakBak - Server Discovery");
		primaryStage.setScene(scene);
		
		// Set minimum window size
		primaryStage.setMinWidth(500);
		primaryStage.setMinHeight(400);
		
		primaryStage.show();
	}

	public static void showLoginView() throws IOException {
		FXMLLoader loader = new FXMLLoader(ChatApplication.class.getResource("fxml/Login.fxml"));
		Parent root = loader.load();

		LoginController controller = loader.getController();

		Scene scene = new Scene(root);

		primaryStage.setTitle("BakBak - Login");
		primaryStage.setScene(scene);
		
		// Set minimum window size
		primaryStage.setMinWidth(450);
		primaryStage.setMinHeight(350);
		
		primaryStage.show();
	}

	public static void showChatView(String username) throws IOException {
		FXMLLoader loader = new FXMLLoader(ChatApplication.class.getResource("fxml/ChatWindow.fxml"));
		Parent root = loader.load();

		ChatController controller = loader.getController();
		controller.setCurrentUsername(username);

		Scene scene = new Scene(root, 1000, 700);

		primaryStage.setTitle("BakBak - " + username);
		primaryStage.setScene(scene);
		
		// Set minimum window size to prevent UI elements from disappearing
		primaryStage.setMinWidth(800);
		primaryStage.setMinHeight(600);
		
		primaryStage.show();

		primaryStage.setOnCloseRequest(event -> {
			if (chatClient != null) {
				chatClient.disconnect();
			}
			System.exit(0);
		});
	}

	public static void setChatClient(ChatClient client) {
		chatClient = client;
	}

	public static ChatClient getChatClient() {
		return chatClient;
	}
}