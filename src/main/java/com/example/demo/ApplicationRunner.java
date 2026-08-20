package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// Spring Boot 启动类：负责启动整个应用并创建 Spring 容器。
public class ApplicationRunner {

	public static void main(String[] args) {
		// 启动后，Spring 会自动扫描并装配各个 @Service、@Component 和 @Configuration。
		SpringApplication.run(ApplicationRunner.class, args);
	}

}
