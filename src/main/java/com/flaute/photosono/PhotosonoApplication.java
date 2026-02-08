package com.flaute.photosono;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PhotosonoApplication {

	public static void main(String[] args) {
		SpringApplication.run(PhotosonoApplication.class, args);
	}

}
