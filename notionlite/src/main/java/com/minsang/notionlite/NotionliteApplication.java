package com.minsang.notionlite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the DB learning playground.
 *
 * This app intentionally exposes simple REST endpoints for studying
 * storage/index internals rather than production feature APIs.
 */
@SpringBootApplication
public class NotionliteApplication {

	public static void main(String[] args) {
		// Boot the application context and embedded web server.
		SpringApplication.run(NotionliteApplication.class, args);
	}

}
