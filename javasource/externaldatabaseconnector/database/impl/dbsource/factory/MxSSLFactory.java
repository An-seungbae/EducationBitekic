package externaldatabaseconnector.database.impl.dbsource.factory;

import com.mendix.http.ICertificateInfo;

import externaldatabaseconnector.database.constants.IMxDataSource;
import externaldatabaseconnector.utils.CertificateUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Properties;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class MxSSLFactory extends SSLSocketFactory {
  SSLSocketFactory sslSocketFactory;

  public MxSSLFactory(Properties properties) throws Exception {
    ICertificateInfo certificateInfo =
        CertificateUtil.getClientCertificateInfo(properties.getProperty(IMxDataSource.CLIENT_CERTIFICATE_IDENTIFIER));

    KeyManager keyManagers[] = null;
    // create the key manager for the client certificate only if the certificate is there in configuration, else add only the default trustStore to the socketFactory
    if (certificateInfo != null) {
      String certFilePath = certificateInfo.getCertificateFile().getPath();
      String certPassword = certificateInfo.getPassword();

      //1. client cert and key
      KeyStore keyStore = KeyStore.getInstance(IMxDataSource.PKCS12);
      try (FileInputStream clientCertFile = new FileInputStream(certFilePath)) {
        keyStore.load(clientCertFile, certPassword.toCharArray());
      }
      //2. create key manager
      KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyManagerFactory.init(keyStore, certPassword.toCharArray());

      keyManagers = keyManagerFactory.getKeyManagers();
    }

    //3. load java's default trust store
    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init((KeyStore) null);

    //4. create TLS1.2 instance
    var context = SSLContext.getInstance(IMxDataSource.TLS_PROTOCOL);
    context.init(keyManagers, trustManagerFactory.getTrustManagers(), new SecureRandom());
    sslSocketFactory = context.getSocketFactory();
  }

  @Override
  public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
    return sslSocketFactory.createSocket(host, port);
  }

  @Override
  public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
    return sslSocketFactory.createSocket(host, port, localHost, localPort);
  }

  @Override
  public Socket createSocket(InetAddress host, int port) throws IOException {
    return sslSocketFactory.createSocket(host, port);
  }

  @Override
  public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
    return sslSocketFactory.createSocket(address, port, localAddress, localPort);
  }

  @Override
  public String[] getDefaultCipherSuites() {
    return sslSocketFactory.getDefaultCipherSuites();
  }

  @Override
  public String[] getSupportedCipherSuites() {
    return sslSocketFactory.getSupportedCipherSuites();
  }

  @Override
  public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
    return sslSocketFactory.createSocket(s, host, port, autoClose);
  }
}
