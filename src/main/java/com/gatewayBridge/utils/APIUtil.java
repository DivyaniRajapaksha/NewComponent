package com.gatewayBridge.utils;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.*;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;



import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * Class for creating HTTP connection
 */
public final class APIUtil {
    public static HttpClient getHttpClient(int port, String protocol) {
        String maxTotal = "100";
        String defaultMaxPerRoute = "50";
        PoolingHttpClientConnectionManager pool = null;
        try {
            pool = getPoolingHttpClientConnectionManager(protocol);
            // pool = poolingConnectionManager();
        } catch (Exception e) {
            System.out.println("Error while getting http client connection manager");
        }
        pool.setMaxTotal(Integer.parseInt(maxTotal));
        pool.setDefaultMaxPerRoute(Integer.parseInt(defaultMaxPerRoute));

        RequestConfig params = RequestConfig.custom().build();
        return HttpClients.custom().setConnectionManager(pool).setDefaultRequestConfig(params).build();
    }

    /**
     * This method is used to execute an HTTP request
     *
     * @param method     HttpRequest Type
     * @param httpClient HttpClient
     * @return HTTPResponse
     * @throws IOException
     */
    public static CloseableHttpResponse executeHTTPRequest(HttpRequestBase method, HttpClient httpClient)
            throws IOException, Exception {
        CloseableHttpResponse httpResponse = null;
        int retryCount = 0;
        boolean retry;
        do {
            try {
                httpResponse = (CloseableHttpResponse) httpClient.execute(method);
                retry = false;
            } catch (IOException ex) {
                retryCount++;
                if (retryCount < 2) {
                    retry = true;
                    System.out.println("Failed retrieving from remote endpoint: " + ex.getMessage()
                            + ". Retrying after " + 15 +
                            " seconds.");
                    try {
                        Thread.sleep(15 * 1000);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                } else {
                    throw ex;
                }
            }
        } while (retry);

        if (httpResponse.getStatusLine().getStatusCode() == 200) {
            return httpResponse;
        } else {
            httpResponse.close();
            String errorMessage = EntityUtils.toString(httpResponse.getEntity(),
                    "UTF-8");
            throw new Exception(errorMessage + "Event-Hub status code is : "
                    + httpResponse.getStatusLine().getStatusCode());
        }
    }

    /**
     * Return a PoolingHttpClientConnectionManager instance
     *
     * @param protocol- service endpoint protocol. It can be http/https
     * @return PoolManager
     */
    private static PoolingHttpClientConnectionManager getPoolingHttpClientConnectionManager(String protocol)
            throws Exception {

        PoolingHttpClientConnectionManager poolManager;
        if ("https".equals(protocol)) {

            SSLConnectionSocketFactory socketFactory = createSocketFactory();
            System.out.println("Hiiiiii" + socketFactory);
            org.apache.http.config.Registry<ConnectionSocketFactory> socketFactoryRegistry =
                    RegistryBuilder.<ConnectionSocketFactory>create()
                            .register("https", socketFactory).build();
            poolManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        } else {
            poolManager = new PoolingHttpClientConnectionManager();
        }
        return poolManager;
    }

    public static SSLConnectionSocketFactory createSocketFactory() throws Exception {
        SSLContext sslContext;

        String keyStorePath = "/home/user/Desktop/New_Test/gateways/gateway-3/repository/resources/security/client-truststore.jks";
        String keyStorePassword = "wso2carbon";
        try {
            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(new FileInputStream(keyStorePath), keyStorePassword.toCharArray());
            sslContext = SSLContexts.custom().loadTrustMaterial(trustStore).build();


            System.out.println(trustStore);
            X509HostnameVerifier hostnameVerifier;
            String hostnameVerifierOption = "AllowAll";


            if ("AllowAll".equalsIgnoreCase(hostnameVerifierOption)) {
                hostnameVerifier = SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
            } else if ("Strict".equalsIgnoreCase(hostnameVerifierOption)) {
                hostnameVerifier = SSLSocketFactory.STRICT_HOSTNAME_VERIFIER;
            } else {
                hostnameVerifier = SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;
            }
            System.out.println(sslContext + " " + hostnameVerifier);
            return new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
        } catch (KeyStoreException e) {
            System.out.println("Failed to read from Key Store");
        } catch (IOException e) {
            System.out.println("Key Store not found in " + keyStorePath);
        } catch (CertificateException e) {
            System.out.println("Failed to read Certificate");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Failed to load Key Store from " + keyStorePath);
        } catch (KeyManagementException e) {
            System.out.println("Failed to load key from" + keyStorePath);
        }

        return null;
    }

    public static PoolingHttpClientConnectionManager poolingConnectionManager() throws NoSuchAlgorithmException, KeyStoreException {
        SSLContextBuilder builder = new SSLContextBuilder();
        try {
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Pooling Connection Manager Initialisation failure because of " + e.getMessage());
        }

        SSLConnectionSocketFactory sslsf = null;
        try {
            sslsf = new SSLConnectionSocketFactory(builder.build());
        } catch (KeyManagementException e) {
            System.out.println("Pooling Connection Manager Initialisation failure because of " + e.getMessage());
        }

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
                .<ConnectionSocketFactory>create().register("https", sslsf)
                .register("http", new PlainConnectionSocketFactory())
                .build();

        PoolingHttpClientConnectionManager poolingConnectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        poolingConnectionManager.setMaxTotal(100);
        return poolingConnectionManager;
    }

}
