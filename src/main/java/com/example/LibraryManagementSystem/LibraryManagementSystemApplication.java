package com.example.LibraryManagementSystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication
@EnableScheduling
public class
LibraryManagementSystemApplication {

	public static void main(String[] args) {
		// Create logs directory if it doesn't exist
		try {
			Files.createDirectories(Paths.get("logs"));
		} catch (Exception e) {
			System.err.println("Warning: Could not create logs directory: " + e.getMessage());
		}
		
		SpringApplication.run(LibraryManagementSystemApplication.class, args);
	}

}

