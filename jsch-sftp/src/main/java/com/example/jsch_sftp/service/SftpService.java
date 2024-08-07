package com.example.jsch_sftp.service;

import com.example.jsch_sftp.config.SftpConfig;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;

@Service
public class SftpService {

    @Autowired
    private SftpConfig sftpConfig;

    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MS = 2000; // 2 seconds

    public void uploadFile(String localFilePath, String remoteDir) {
        int attempts = 0;
        boolean isUploaded = false;

        while (!isUploaded && attempts < MAX_RETRIES) {
            Session session = null;
            Channel channel = null;
            ChannelSftp channelSftp = null;

            try {
                JSch jsch = new JSch();
                session = jsch.getSession(sftpConfig.getUsername(), sftpConfig.getHost(), sftpConfig.getPort());
                session.setPassword(sftpConfig.getPassword());

                // Avoid asking for key confirmation
                java.util.Properties config = new java.util.Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);

                session.connect();
                channel = session.openChannel("sftp");
                channel.connect();
                channelSftp = (ChannelSftp) channel;

                File file = new File(localFilePath);
                channelSftp.cd(remoteDir);
                channelSftp.put(new FileInputStream(file), file.getName());

                isUploaded = true; // File uploaded successfully
            } catch (Exception ex) {
                attempts++;
                ex.printStackTrace();
                if (attempts < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS); // Wait before retrying
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            } finally {
                if (channelSftp != null) {
                    channelSftp.exit();
                }
                if (channel != null) {
                    channel.disconnect();
                }
                if (session != null) {
                    session.disconnect();
                }
            }
        }

        if (!isUploaded) {
            throw new RuntimeException("Failed to upload file after " + MAX_RETRIES + " attempts");
        }
    }
}
