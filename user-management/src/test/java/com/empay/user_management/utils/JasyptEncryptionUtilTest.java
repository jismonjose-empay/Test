package com.empay.user_management.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

class JasyptEncryptionUtilTest {

    @Test
    @DisplayName("jasyptStringEncryptor throws IllegalStateException when password is null or blank")
    void testJasyptStringEncryptor_ThrowsWhenNoPassword() {
        JasyptEncryptionUtil util = new JasyptEncryptionUtil();
        assertThrows(IllegalStateException.class, util::jasyptStringEncryptor);

        ReflectionTestUtils.setField(util, "encryptionPassword", "   ");
        assertThrows(IllegalStateException.class, util::jasyptStringEncryptor);
    }

    @Test
    @DisplayName("jasyptStringEncryptor creates valid encryptor and encrypts/decrypts correctly")
    void testJasyptStringEncryptor_EncryptAndDecrypt() {
        JasyptEncryptionUtil util = new JasyptEncryptionUtil();
        ReflectionTestUtils.setField(util, "encryptionPassword", "TestSecretKey123!@#");

        StringEncryptor encryptor = util.jasyptStringEncryptor();
        assertNotNull(encryptor);

        String plaintext = "jdbc:sqlserver://192.168.1.7:1433;databaseName=MARS3_AI";
        String encrypted = encryptor.encrypt(plaintext);
        assertNotNull(encrypted);

        String decrypted = encryptor.decrypt(encrypted);
        assertEquals(plaintext, decrypted);
    }

    @Test
    @DisplayName("decrypt decodes ENC(...) values and leaves plain values untouched")
    void testDecrypt_ValidAndPlainValues() {
        JasyptEncryptionUtil util = new JasyptEncryptionUtil();
        ReflectionTestUtils.setField(util, "encryptionPassword", "TestSecretKey123!@#");
        StringEncryptor encryptor = util.jasyptStringEncryptor();

        String encrypted = encryptor.encrypt("MySecretPassword");
        String decrypted = ReflectionTestUtils.invokeMethod(util, "decrypt", "ENC(" + encrypted + ")", encryptor);
        assertEquals("MySecretPassword", decrypted);

        String plain = ReflectionTestUtils.invokeMethod(util, "decrypt", "plain_user", encryptor);
        assertEquals("plain_user", plain);
    }

    @Test
    @DisplayName("decrypt throws IllegalArgumentException on null or blank value")
    void testDecrypt_ThrowsOnNullOrBlank() {
        JasyptEncryptionUtil util = new JasyptEncryptionUtil();
        ReflectionTestUtils.setField(util, "encryptionPassword", "TestSecretKey123!@#");
        StringEncryptor encryptor = util.jasyptStringEncryptor();

        assertThrows(IllegalArgumentException.class, () ->
                ReflectionTestUtils.invokeMethod(util, "decrypt", null, encryptor));
        assertThrows(IllegalArgumentException.class, () ->
                ReflectionTestUtils.invokeMethod(util, "decrypt", "   ", encryptor));
    }

    @Test
    @DisplayName("dataSource decrypts ENC(...) values and handles plain values using mock driver")
    void testDataSource_CreationAndDecryption() throws SQLException {
        Driver mockDriver = new MockDriver();
        DriverManager.registerDriver(mockDriver);

        try {
            JasyptEncryptionUtil util = new JasyptEncryptionUtil();
            ReflectionTestUtils.setField(util, "encryptionPassword", "TestSecretKey123!@#");
            StringEncryptor encryptor = util.jasyptStringEncryptor();

            Environment env = mock(Environment.class);
            when(env.getRequiredProperty("spring.datasource.url")).thenReturn("jdbc:mockdb:test");
            when(env.getRequiredProperty("spring.datasource.username")).thenReturn("sa");
            when(env.getRequiredProperty("spring.datasource.password")).thenReturn("ENC(" + encryptor.encrypt("Pass123!") + ")");
            when(env.getProperty("spring.datasource.driver-class-name", "com.microsoft.sqlserver.jdbc.SQLServerDriver"))
                    .thenReturn(MockDriver.class.getName());
            when(env.getProperty("spring.datasource.hikari.maximum-pool-size", Integer.class, 10)).thenReturn(2);
            when(env.getProperty("spring.datasource.hikari.minimum-idle", Integer.class, 2)).thenReturn(1);
            when(env.getProperty("spring.datasource.hikari.connection-timeout", Long.class, 30000L)).thenReturn(5000L);

            try (var ds = (com.zaxxer.hikari.HikariDataSource) util.dataSource(env, encryptor)) {
                assertNotNull(ds);
                assertEquals("jdbc:mockdb:test", ds.getJdbcUrl());
                assertEquals("sa", ds.getUsername());
                assertEquals("Pass123!", ds.getPassword());
                assertEquals(2, ds.getMaximumPoolSize());
            }
        } finally {
            DriverManager.deregisterDriver(mockDriver);
        }
    }

    public static class MockDriver implements Driver {
        @Override
        public Connection connect(String url, Properties info) {
            return mock(Connection.class);
        }

        @Override
        public boolean acceptsURL(String url) {
            return url != null && url.startsWith("jdbc:mockdb:");
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }
    }
}
