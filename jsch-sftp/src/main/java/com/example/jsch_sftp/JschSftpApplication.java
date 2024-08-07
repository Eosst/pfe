/*
package com.example.jsch_sftp;

import com.example.jsch_sftp.service.SftpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.nio.file.*;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

@SpringBootApplication
public class JschSftpApplication implements CommandLineRunner {

	@Autowired
	private SftpService sftpService;

	private static final String LOCAL_DIR = "C:\\Users\\SIMO\\Documents\\pfe_abb\\test\\files";
	private static final String REMOTE_DIR = "/C:/Users/eosst/Downloads";
	private static final long CHECK_INTERVAL = 5000; // 5 minutes in milliseconds

	public static void main(String[] args) {
		SpringApplication.run(JschSftpApplication.class, args);
	}

	@Override
	public void run(String... args) {
		System.out.println("Starting directory monitoring...");

		int lastUploadedFileNumber = -1;

		while (true) {
			try {
				int highestFileNumber = getHighestFileNumber(Paths.get(LOCAL_DIR));

				if (highestFileNumber > lastUploadedFileNumber) {
					String highestFileName = "test_" + highestFileNumber + ".txt";
					String localFilePath = LOCAL_DIR + "/" + highestFileName;
					sftpService.uploadFile(localFilePath, REMOTE_DIR);
					lastUploadedFileNumber = highestFileNumber;
					System.out.println("Uploaded file: " + highestFileName);
				} else {
					System.out.println("No new files to upload.");
				}

				Thread.sleep(CHECK_INTERVAL);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private int getHighestFileNumber(Path dirPath) {
		Pattern pattern = Pattern.compile("test_(\\d+)\\.txt");

		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dirPath)) {
			Optional<Integer> maxNumber = StreamSupport.stream(directoryStream.spliterator(), false)
					.map(path -> {
						Matcher matcher = pattern.matcher(path.getFileName().toString());
						if (matcher.matches()) {
							return Integer.parseInt(matcher.group(1));
						}
						return -1;
					})
					.max(Comparator.naturalOrder());

			return maxNumber.orElse(-1);

		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}
}
*/


package com.example.jsch_sftp;

import com.example.jsch_sftp.service.SftpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import java.util.stream.StreamSupport;

@SpringBootApplication
public class JschSftpApplication implements CommandLineRunner {

	@Autowired
	private SftpService sftpService;

	private static final String LOCAL_DIR = "C:\\Users\\SIMO\\Documents\\pfe_abb\\test\\files";
	private static final String REMOTE_DIR = "/C:/Users/eosst/Downloads";
	private static final long CHECK_INTERVAL = 5000; // 5 seconds in milliseconds

	public static void main(String[] args) {
		SpringApplication.run(JschSftpApplication.class, args);
	}

	@Override
	public void run(String... args) {
		System.out.println("Starting directory monitoring...");

		File lastUploadedFile = null;
		int nonewfiles = 0;

		while (true) {
			try {
				File latestFile = getLatestModifiedFile(Paths.get(LOCAL_DIR));

				if (latestFile != null && !latestFile.equals(lastUploadedFile)) {
					System.out.println("Uploading file: " + latestFile.getAbsolutePath());
					sftpService.uploadFile(latestFile.getAbsolutePath(), REMOTE_DIR);
					lastUploadedFile = latestFile;
					System.out.println("Uploaded file: " + latestFile.getName());
				} else {
					System.out.println("No new files to upload.");
					nonewfiles++;
				}
				if(nonewfiles==5){
					System.out.println("finishing program since there are no new files being created !");
					break;
				}

				Thread.sleep(CHECK_INTERVAL);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
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
