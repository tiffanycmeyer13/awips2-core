package com.raytheon.uf.common.jms;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import com.raytheon.uf.common.security.encryption.AESEncryptor;

/**
 *
 * Retrieves and decrypts password for JMS
 *
 * <pre>
*
* SOFTWARE HISTORY
*
* Date          Ticket#  Engineer    Description
* ------------- -------- ----------- --------------------------
* Mar 05, 2021  7899     tbucher     Initial creation
 *
 * </pre>
 *
 * @author tbucher
 */
public class JMSPasswordUtil {

    private static final String JMS_PASSWORD_KEY = "7tOFQgZiJWNBWaG7vCNcFxO6aRXkNO0eIo67TLAjXcwaNRuI53yUUza6h0PNKpmmUfA2DClDLd9V";

    private static final String PROPERTIES_FILE_DIR = "QPID_SSL_CERT_DB";

    public static String getJMSPassword() throws Exception {

        Path certDB = null;
        String certDir = System.getenv(PROPERTIES_FILE_DIR);
        if (certDir == null) {
            String userHome = System.getProperty("user.home");
            if (userHome != null) {
                certDB = Paths.get(userHome).resolve(".qpid");
                if (!Files.isDirectory(certDB)) {
                    certDB = null;
                }
            }
            if (certDB == null) {
                throw new IllegalStateException(
                        "Unable to load properties file for jms password. Consider setting the environmental variable: "
                                + PROPERTIES_FILE_DIR);
            }
        } else {
            certDB = Paths.get(certDir);
        }

        Path propsFile = certDB.resolve("passwords.properties");

        Properties properties = new Properties();
        try (InputStream fis = Files.newInputStream(propsFile)) {
            properties.load(fis);
        }

        AESEncryptor encryptor = new AESEncryptor(JMS_PASSWORD_KEY);
        String decryptedPassword = encryptor
                .decrypt(properties.getProperty("a2.jms.connection.password"));

        return decryptedPassword;
    }

    private static String encryptJMSPassword(String password) throws Exception {
        AESEncryptor encryptor = new AESEncryptor(JMS_PASSWORD_KEY);
        return encryptor.encrypt(password);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException(
                    "Must provide option \"--encrypt\" or \"--decrypt\".");
        }

        if ("--decrypt".equals(args[0])) {
            System.out.println(getJMSPassword());
        } else if ("--encrypt".equals(args[0])) {
            if (args.length >= 2) {
                System.out.println(encryptJMSPassword(args[1]));
            } else {
                throw new IllegalArgumentException(
                        "Must provide a password with option \"--encrypt\".");
            }
        } else {
            throw new IllegalArgumentException(
                    "Option " + args[0] + " not recognized.");
        }
    }
}
