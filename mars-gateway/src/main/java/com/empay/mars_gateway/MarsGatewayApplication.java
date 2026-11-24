package com.empay.mars_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.empay")
public class MarsGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(MarsGatewayApplication.class, args);
	}

}
