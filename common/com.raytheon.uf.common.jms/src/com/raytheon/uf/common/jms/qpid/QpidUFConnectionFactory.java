/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.jms.qpid;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map.Entry;
import java.util.function.Supplier;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.QueueConnection;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.utils.URIBuilder;
import org.apache.qpid.jms.JmsConnectionExtensions;
import org.apache.qpid.jms.JmsConnectionFactory;

import com.raytheon.uf.common.comm.HttpClient;
import com.raytheon.uf.common.jms.HttpProxyHandlerSslExt;
import com.raytheon.uf.common.jms.JMSConnectionInfo;
import com.raytheon.uf.common.jms.JmsSslConfiguration;

import io.netty.handler.proxy.ProxyHandler;

/**
 * Qpid JMS connection factory
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 11, 2019 7724       tgurney     Initial creation
 * May 27, 2021 8469       dgilling    Pass broker REST service port through
 *                                     JMSConnectionInfo.
 * Aug 06, 2021 22528      smoorthy    Add proxy handler extension to ConnectionFactory
 * </pre>
 *
 * @author tgurney
 */

public class QpidUFConnectionFactory implements ConnectionFactory {
    private final IBrokerRestProvider jmsAdmin;

    private final JmsConnectionFactory connectionFactory;

    private static final String JMS_USERNAME = "guest";

    public QpidUFConnectionFactory(JMSConnectionInfo connectionInfo)
            throws JMSConfigurationException {
        String url = QpidUFConnectionFactory.getConnectionURL(connectionInfo);
        this.connectionFactory = new JmsConnectionFactory(url);


        String proxyAddr = connectionInfo.getProxyAddress();

        if (proxyAddr != null) { //add proxy extension to the Connection Factory
            String proxyHost = null;
            int proxyPort = 0;
            try {
                URI proxyURI = new URI(proxyAddr);
                proxyHost = proxyURI.getHost();
                proxyPort = proxyURI.getPort();
            } catch (URISyntaxException e) {
                throw new JMSConfigurationException("Problem processing proxy address string", e.getCause());
            }

            //get user credentials
            AuthScope authScope = new AuthScope(proxyHost, proxyPort, AuthScope.ANY_REALM,
                    AuthSchemes.BASIC);
            UsernamePasswordCredentials creds = (UsernamePasswordCredentials) HttpClient.getInstance().getCredentials(authScope);
            String username = creds.getUserName();
            String password = creds.getPassword();


            //add the proxy handler extension
            String host = proxyHost;
            int port = proxyPort;
            this.connectionFactory.setExtension(JmsConnectionExtensions.PROXY_HANDLER_SUPPLIER.toString(), (connection, remote) -> {
                SocketAddress proxyAddress = new InetSocketAddress(host, port); //443
                Supplier<ProxyHandler> proxyHandlerFactory = () -> {
                    return new HttpProxyHandlerSslExt(proxyAddress, username, password );
                };
                return proxyHandlerFactory;
            });
        }


        this.jmsAdmin = new QpidBrokerRestImpl(connectionInfo.getHost(),
                connectionInfo.getVhost(), connectionInfo.getServicePort());
    }

    @Override
    public Connection createConnection(String userName, String password)
            throws JMSException {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " "
                + "does not support username/password connections");
    }

    @Override
    public JMSContext createContext(String userName, String password) {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " "
                + "does not support username/password connections");
    }

    @Override
    public JMSContext createContext(String userName, String password,
            int sessionMode) {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " "
                + "does not support username/password connections");
    }

    @Override
    public Connection createConnection() throws JMSException {
        Connection connection = connectionFactory.createConnection();
        return new QpidUFConnection(connection, jmsAdmin);
    }

    @Override
    public JMSContext createContext() {
        return connectionFactory.createContext();
    }

    @Override
    public JMSContext createContext(int sessionMode) {
        return connectionFactory.createContext(sessionMode);
    }

    public static String getConnectionURL(JMSConnectionInfo connectionInfo)
            throws JMSConfigurationException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("amqps");
        uriBuilder.setHost(connectionInfo.getHost());
        uriBuilder.setPort(Integer.parseInt(connectionInfo.getPort()));
        uriBuilder.addParameter("amqp.vhost", connectionInfo.getVhost());
        uriBuilder.addParameter("jms.username", JMS_USERNAME);
        for (Entry<String, String> e : connectionInfo.getParameters()
                .entrySet()) {
            uriBuilder.addParameter(e.getKey(), e.getValue());
        }
        uriBuilder = configureSSL(uriBuilder);

        return uriBuilder.toString();
    }

    public static URIBuilder configureSSL(URIBuilder uriBuilder)
            throws JMSConfigurationException {
        JmsSslConfiguration sslConfig = new JmsSslConfiguration(JMS_USERNAME);
        Path trustStorePath = sslConfig.getJavaTrustStoreFile();
        Path keyStorePath = sslConfig.getJavaKeyStoreFile();
        try {
            String password = sslConfig.getPassword();

            uriBuilder.addParameter("transport.trustStoreLocation",
                    trustStorePath.toString());
            uriBuilder.addParameter("transport.trustStorePassword", password);
            uriBuilder.addParameter("transport.keyStoreLocation",
                    keyStorePath.toString());
            uriBuilder.addParameter("transport.keyStorePassword", password);

            return uriBuilder;
        } catch (Exception e) {
            throw new JMSConfigurationException(
                    "Could not decrypt JMS password.", e);
        }
    }

    public QueueConnection createQueueConnection() throws JMSException {
        return connectionFactory.createQueueConnection();
    }
}
