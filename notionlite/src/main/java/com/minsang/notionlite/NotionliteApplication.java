package com.minsang.notionlite;

// Spring Boot 앱을 시작할 때 필요한 실행 유틸리티입니다.
import org.springframework.boot.SpringApplication;
// 이 클래스가 "스프링 부트 시작점"이라는 표시입니다.
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
		// 자바 프로그램의 시작 지점입니다.
		// 스프링 컨테이너를 띄우고, 내장 웹 서버도 함께 시작합니다.
		SpringApplication.run(NotionliteApplication.class, args);
	}

}
