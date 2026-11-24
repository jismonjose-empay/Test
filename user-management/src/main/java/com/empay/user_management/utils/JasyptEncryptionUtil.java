
package com.empay.user_management.utils;

import javax.sql.DataSource;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class JasyptEncryptionUtil {

	@Value("${JASYPT_ENCRYPTION_PASSWORD:}")
	private String encryptionPassword;

	@Bean("jasyptStringEncryptor")
	public StringEncryptor jasyptStringEncryptor() {

		if (encryptionPassword == null || encryptionPassword.isBlank()) {
			throw new IllegalStateException("JASYPT_ENCRYPTION_PASSWORD is not configured");
		}

		PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
		SimpleStringPBEConfig config = new SimpleStringPBEConfig();

		config.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
		config.setPassword(encryptionPassword);
		config.setKeyObtentionIterations("100000");
		config.setPoolSize("1");
		config.setProviderName("SunJCE");
		config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
		config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
		config.setStringOutputType("base64");
		encryptor.setConfig(config);
		return encryptor;
	}

	@Bean
	@Primary
	public DataSource dataSource(Environment environment,
			@Qualifier("jasyptStringEncryptor") StringEncryptor encryptor) {

		String url = decrypt(environment.getRequiredProperty("spring.datasource.url"), encryptor);

		String username = decrypt(environment.getRequiredProperty("spring.datasource.username"), encryptor);

		String password = decrypt(environment.getRequiredProperty("spring.datasource.password"), encryptor);

		String driverClassName = environment.getProperty("spring.datasource.driver-class-name", environment
				.getProperty("spring.datasource.driverClassName", "com.microsoft.sqlserver.jdbc.SQLServerDriver"));

		HikariConfig hikariConfig = new HikariConfig();

		hikariConfig.setJdbcUrl(url);
		hikariConfig.setUsername(username);
		hikariConfig.setPassword(password);
		hikariConfig.setDriverClassName(driverClassName);

		hikariConfig.setMaximumPoolSize(
				environment.getProperty("spring.datasource.hikari.maximum-pool-size", Integer.class, 10));

		hikariConfig.setMinimumIdle(environment.getProperty("spring.datasource.hikari.minimum-idle", Integer.class, 2));

		hikariConfig.setConnectionTimeout(
				environment.getProperty("spring.datasource.hikari.connection-timeout", Long.class, 30000L));

		return new HikariDataSource(hikariConfig);
	}

	private String decrypt(String value, StringEncryptor encryptor) {

		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Datasource property cannot be empty");
		}

		if (value.startsWith("ENC(") && value.endsWith(")")) {

			String encryptedValue = value.substring(4, value.length() - 1);

			return encryptor.decrypt(encryptedValue);
		}

		return value;
	}
}