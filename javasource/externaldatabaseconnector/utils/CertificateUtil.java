package externaldatabaseconnector.utils;

import com.mendix.http.HttpConfiguration;
import com.mendix.http.ICertificateInfo;

public class CertificateUtil {
  public static ICertificateInfo getClientCertificateInfo(String identifier) {
    ICertificateInfo certificateInfo = HttpConfiguration.getInstance().getServiceClientCertificates()
        .stream()
        .filter(cert -> cert.getIdentifier().equals(identifier))
        .findFirst()
        .orElse(null);
    return certificateInfo;
  }
}
