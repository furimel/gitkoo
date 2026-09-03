package com.furimeo.gitkoo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GitkooApplication {

	public static void main(String[] args) {
		SpringApplication.run(GitkooApplication.class, args);
	}

}
