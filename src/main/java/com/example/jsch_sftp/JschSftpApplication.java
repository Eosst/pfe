package com.example.jsch_sftp;

import com.dustinredmond.fxtrayicon.FXTrayIcon;
import com.example.jsch_sftp.service.SftpService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.SpringApplication;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Optional;

@SpringBootApplication
public class JschSftpApplication extends Application {

	private ConfigurableApplicationContext springContext;

	@Autowired
	private SftpService sftpService;

	private static ServerSocket singleInstanceSocket;
	private FXTrayIcon trayIcon;

	public static void main(String[] args) {
		System.setProperty("java.awt.headless", "false");

		try {
			singleInstanceSocket = new ServerSocket(65432); // Pick an unused port
		} catch (IOException e) {
			// Exit if another instance is already running
			System.exit(0);
		}
		launch(args);
	}

	@Override
	public void init() throws Exception {
		springContext = SpringApplication.run(JschSftpApplication.class);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
		loader.setControllerFactory(springContext::getBean);
		primaryStage.setScene(new Scene(loader.load()));
		primaryStage.setTitle("SFTP Service");
		primaryStage.show();

		// Set up the system tray using FXTrayIcon
		trayIcon = new FXTrayIcon(primaryStage, getClass().getResource("/static/abb_logo_HD.png"));
		trayIcon.setApplicationTitle("Your Application");
		trayIcon.show();

		trayIcon.addMenuItem("Show app", event -> Platform.runLater(primaryStage::show));
		trayIcon.addExitItem("Exit", event -> exitApplication());

		// Set up close request handler
		primaryStage.setOnCloseRequest(event -> {
			event.consume(); // Prevent the window from closing immediately
			showExitConfirmation(primaryStage);
		});
	}

	private void showExitConfirmation(Stage primaryStage) {
		// Create a confirmation dialog
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("Exit Confirmation");
		alert.setHeaderText(null);
		alert.setContentText("Do you want to exit or minimize to tray?");

		// Add buttons to the dialog
		ButtonType buttonTypeYes = new ButtonType("Exit");
		ButtonType buttonTypeNo = new ButtonType("Minimize");
		alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo);

		// Show the dialog and wait for the user's response
		Optional<ButtonType> result = alert.showAndWait();

		// Determine the user's choice
		boolean userWantsToClose = result.isPresent() && result.get() == buttonTypeYes;

		if (userWantsToClose) {
			exitApplication();
		} else {
			primaryStage.hide(); // Minimize to system tray
		}
	}

	@Override
	public void stop() throws Exception {
		springContext.close();
		Platform.exit();
	}

	private void exitApplication() {
		// Cleanup and exit
		trayIcon.hide(); // Make sure to hide the tray icon when exiting
		Platform.exit();
		System.exit(0);
	}
}
