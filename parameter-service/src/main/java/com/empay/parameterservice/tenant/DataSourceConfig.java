
package com.empay.parameterservice.tenant;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.empay.parameterservice.utils.JasyptEncryptionUtil;

@Configuration
public class DataSourceConfig {

	@Value("${tenant.datasources.1.url}")
	private String db1001Url;

	@Value("${tenant.datasources.1.username}")
	private String db1001Username;

	@Value("${tenant.datasources.1.password}")
	private String db1001Password;

	@Value("${tenant.datasources.2.url}")
	private String db1002Url;

	@Value("${tenant.datasources.2.username}")
	private String db1002Username;

	@Value("${tenant.datasources.2.password}")
	private String db1002Password;

	private final StringEncryptor encryptor;

	public DataSourceConfig(@Qualifier("jasyptStringEncryptor") StringEncryptor encryptor) {
		this.encryptor = encryptor;
	}

	@Bean
	@Primary
	public DataSource dataSource() {

		Map<Object, Object> dataSources = new HashMap<>();

		dataSources.put("1", createDataSource(db1001Url, db1001Username, db1001Password));

		dataSources.put("2", createDataSource(db1002Url, db1002Username, db1002Password));

		InstitutionRoutingDataSource routing = new InstitutionRoutingDataSource();

		routing.setTargetDataSources(dataSources);
		routing.setDefaultTargetDataSource(dataSources.get("1"));

		routing.afterPropertiesSet();

		return routing;
	}

	private DataSource createDataSource(String url, String username, String password) {

		String decryptedUrl = JasyptEncryptionUtil.decrypt(url, encryptor);

		String decryptedUsername = JasyptEncryptionUtil.decrypt(username, encryptor);

		String decryptedPassword = JasyptEncryptionUtil.decrypt(password, encryptor);

		return DataSourceBuilder.create().url(decryptedUrl).username(decryptedUsername).password(decryptedPassword)
				.driverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver").build();
	}
}
