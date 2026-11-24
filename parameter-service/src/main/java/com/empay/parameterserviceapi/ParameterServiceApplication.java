package com.empay.parameterserviceapi;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

//import com.empay.parameterservice.service.Test;

import lombok.extern.slf4j.Slf4j;
@Slf4j
@SpringBootApplication(scanBasePackages = "com.empay")
@EntityScan({"com.empay.*"})
@EnableJpaRepositories({"com.empay.*"})
public class ParameterServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ParameterServiceApplication.class, args);
		//Test test=new Test();
		//test.testLogRollover();
	}

}
