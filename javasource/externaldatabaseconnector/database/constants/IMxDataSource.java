package externaldatabaseconnector.database.constants;

public interface IMxDataSource {
  String PKCS12 = "PKCS12";
  String TLS_PROTOCOL = "TLSv1.2";
  String AUTHENTICATION_METHOD = "AuthenticationMethod";
  String SSL_AUTHENTICATION = "SSL_AUTHENTICATION";
  String SSL_MODE_PROPERTY = "sslmode";
  String DEFAULT_SSL_MODE = "prefer";
  String CLIENT_CERTIFICATE_IDENTIFIER = "ClientCertificateIdentifier";
  String SSL_FACTORY_PROPERTY_KEY = "sslfactory";
  String SSL_FACTORY_NAMESPACE = "externaldatabaseconnector.database.impl.dbsource.factory.MxSSLFactory";
}
