package com.example.jsch_sftp.controller;

import com.example.jsch_sftp.service.SftpService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Controller
public class MainController {
    @FXML
    private TextField sftpHostField;
    @FXML
    private TextField sftpPortField;
    @FXML
    private TextField sftpUsernameField;
    @FXML
    private PasswordField sftpPasswordField;
    @FXML
    private TextField localDirField;
    @FXML
    private TextField remoteDirField;
    @FXML
    private TextField checkIntervalField;
    @FXML
    private TextArea logArea;
    @FXML
    private Button startServiceButton;
    @FXML
    private Button stopServiceButton;

    @Autowired
    private SftpService sftpService;
    @Autowired
    private Environment environment;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private boolean running = false;

    private static final Pattern IP_ADDRESS_PATTERN =
            Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");
    private static final Pattern PORT_PATTERN =
            Pattern.compile("^\\d+$");

    @FXML
    public void initialize() {
        // Load initial values from the application.properties
        sftpHostField.setText(environment.getProperty("sftp.host", "192.168.1.2"));
        sftpPortField.setText(environment.getProperty("sftp.port", "22"));
        sftpUsernameField.setText(environment.getProperty("sftp.username", "eosst"));
        sftpPasswordField.setText(environment.getProperty("sftp.password", "1234"));
    }

    @FXML
    public void handleSaveSettings() {
        String host = sftpHostField.getText();
        String portText = sftpPortField.getText();
        String username = sftpUsernameField.getText();
        String password = sftpPasswordField.getText();

        // Validate inputs
        if (!validateIpAddress(host)) {
            logArea.appendText("Error: Invalid IP address format.\n");
            return;
        }
        if (!validatePort(portText)) {
            logArea.appendText("Error: Invalid port number.\n");
            return;
        }
        if (username.isEmpty()) {
            logArea.appendText("Error: Username cannot be empty.\n");
            return;
        }
        if (password.isEmpty()) {
            logArea.appendText("Error: Password cannot be empty.\n");
            return;
        }

        int port = Integer.parseInt(portText);

        // Save the validated settings
        Properties props = new Properties();
        props.setProperty("sftp.host", host);
        props.setProperty("sftp.port", String.valueOf(port));
        props.setProperty("sftp.username", username);
        props.setProperty("sftp.password", password);

        try (OutputStream output = new FileOutputStream("application.properties")) {
            props.store(output, null);
        } catch (IOException e) {
            logArea.appendText("Failed to save settings: " + e.getMessage() + "\n");
            return;
        }

        logArea.appendText("Settings saved successfully.\n");
    }

    @FXML
    public void handleStartService() {
        if (running) {
            logArea.appendText("Service is already running.\n");
            return;
        }

        String localDir = localDirField.getText();
        String remoteDir = remoteDirField.getText();
        String checkIntervalText = checkIntervalField.getText();
        String host = sftpHostField.getText();
        String portText = sftpPortField.getText();

        // Validate necessary fields before starting the service
        if (!validateIpAddress(host)) {
            logArea.appendText("Error: Invalid IP address format.\n");
            return;
        }
        if (!validatePort(portText)) {
            logArea.appendText("Error: Invalid port number.\n");
            return;
        }
        if (localDir.isEmpty()) {
            logArea.appendText("Error: Local directory is not selected.\n");
            return;
        }
        if (remoteDir.isEmpty()) {
            logArea.appendText("Error: Remote directory is not selected.\n");
            return;
        }
        if (checkIntervalText.isEmpty()) {
            logArea.appendText("Error: Check interval is not set.\n");
            return;
        }

        int port = Integer.parseInt(portText);
        int checkInterval;
        try {
            checkInterval = Integer.parseInt(checkIntervalText) * 1000;
        } catch (NumberFormatException e) {
            logArea.appendText("Error: Check interval is not a valid number.\n");
            return;
        }

        running = true;
        startServiceButton.setDisable(true);
        stopServiceButton.setDisable(false);
        logArea.appendText("Starting service...\n");

        executorService.submit(() -> {
            File lastUploadedFile = null;
            Platform.runLater(() -> logArea.appendText("Connecting to " + host + " on port " + port + "...\n"));

            boolean isConnected = sftpService.connect();

            if (isConnected) {
                Platform.runLater(() -> logArea.appendText("Connected successfully.\n"));
                while (running) {
                    try {
                        File latestFile = getLatestModifiedFile(Paths.get(localDir));
                        if (latestFile != null && !latestFile.equals(lastUploadedFile)) {
                            long fileSize = latestFile.length();
                            Platform.runLater(() -> logArea.appendText("Uploading file: " + latestFile.getName() + " (Size: " + fileSize + " bytes)\n"));
                            sftpService.uploadFile(latestFile.getAbsolutePath(), remoteDir);
                            lastUploadedFile = latestFile;
                            Platform.runLater(() -> logArea.appendText("Uploaded file: " + latestFile.getName() + "\n"));
                        } else {
                            Platform.runLater(() -> logArea.appendText("No new files to upload.\n"));
                        }
                        Thread.sleep(checkInterval);
                    } catch (Exception e) {
                        if(running){
                        Platform.runLater(() -> logArea.appendText("Error: " + e.getMessage() + "\n"));
                        running = false;
                        System.out.println("its the start service one");}
                    }
                }
            } else {
                Platform.runLater(() -> logArea.appendText("Can't connect, verify server settings.\n"));
                this.handleStopService();
            }
        });
    }

    private boolean validateIpAddress(String ip) {
        return IP_ADDRESS_PATTERN.matcher(ip).matches() &&
                Stream.of(ip.split("\\.")).allMatch(segment -> {
                    int value = Integer.parseInt(segment);
                    return value >= 0 && value <= 255;
                });
    }

    private boolean validatePort(String port) {
        return PORT_PATTERN.matcher(port).matches() &&
                Integer.parseInt(port) > 0 && Integer.parseInt(port) <= 65535;
    }

    @FXML
    public void handleBrowseLocal() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(null);
        if (selectedDirectory != null) {
            localDirField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    @FXML
    public void handleStopService() {
        if (!running) {
            logArea.appendText("Service is not running.\n");
            return;
        }
        executorService.shutdownNow();

        // Reinitialize the executor service for future use
        executorService = Executors.newSingleThreadExecutor();

        running = false;
        startServiceButton.setDisable(false);
        stopServiceButton.setDisable(true);
        logArea.appendText("Service stopped.\n");
    }

    private File getLatestModifiedFile(Path dirPath) {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dirPath)) {
            Optional<File> latestFile = StreamSupport.stream(directoryStream.spliterator(), false)
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .max((file1, file2) -> {
                        try {
                            FileTime fileTime1 = Files.readAttributes(file1.toPath(), BasicFileAttributes.class).lastModifiedTime();
                            FileTime fileTime2 = Files.readAttributes(file2.toPath(), BasicFileAttributes.class).lastModifiedTime();
                            return fileTime1.compareTo(fileTime2);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return 0;
                        }
                    });

            return latestFile.orElse(null);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
