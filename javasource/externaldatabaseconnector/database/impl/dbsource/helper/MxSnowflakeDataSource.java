package externaldatabaseconnector.database.impl.dbsource.helper;

import com.mendix.core.Core;

import externaldatabaseconnector.database.constants.ErrorCode;
import externaldatabaseconnector.database.exceptions.MxInvalidFieldException;
import externaldatabaseconnector.exceptions.IMxErrorMessages;
import externaldatabaseconnector.pojo.ConnectionDetails;
import externaldatabaseconnector.proxies.constants.Constants;
import net.snowflake.client.jdbc.SnowflakeBasicDataSource;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;

import java.io.StringReader;
import java.security.PrivateKey;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

public class MxSnowflakeDataSource {
  private static final String END = "-----END";

  private static final String SNOWFLAKE_APPLICATION_PARAMETER_PREFIX = "Mendix-ExtDbConnector_SP";

  private static String getMultiline(String aKey) {

    // Extract the first line (beginning part)
    String firstLine = aKey.substring(0, aKey.indexOf("-----", 1) + 5);

    int indexOfEnd = aKey.lastIndexOf(END);
    // Extract the last line (ending part)
    String lastLine = aKey.substring(indexOfEnd);

    // Extract the content in between the first and last line
    String content = aKey.substring(firstLine.length(), indexOfEnd).trim();

    // Combine the parts into the desired format
    return firstLine + "\n" + content + "\n" + lastLine;
  }

  public static PrivateKey getPrivateKeyObject(String key, String passphrase)
      throws Exception {
    String formattedKey;
    try {
      formattedKey = getMultiline(key);
    }catch (IndexOutOfBoundsException e){
      String messageCode = ErrorCode.PRIVATE_KEY_INVALID;
      Map<String, String> messageDataMap = new HashMap<>();
      throw new MxInvalidFieldException(IMxErrorMessages.INVALID_PRIVATE_KEY, messageCode, messageDataMap, e);
    }

    PrivateKeyInfo privateKeyInfo = null;

    Security.addProvider(new BouncyCastleProvider());
    PEMParser pemParser = new PEMParser(new StringReader(formattedKey));
    Object pemObject = pemParser.readObject();

    if (pemObject instanceof PKCS8EncryptedPrivateKeyInfo) {
      // Handle the case where the private key is encrypted.
      PKCS8EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = (PKCS8EncryptedPrivateKeyInfo) pemObject;
      InputDecryptorProvider pkcs8Prov = new JceOpenSSLPKCS8DecryptorProviderBuilder().build(passphrase.toCharArray());
      privateKeyInfo = encryptedPrivateKeyInfo.decryptPrivateKeyInfo(pkcs8Prov);
    } else if (pemObject instanceof PrivateKeyInfo) {
      // Handle the case where the private key is unencrypted.
      privateKeyInfo = (PrivateKeyInfo) pemObject;
    }

    pemParser.close();
    JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
    return converter.getPrivateKey(privateKeyInfo);
  }

  public static SnowflakeBasicDataSource getSnowflakeDataSource(final ConnectionDetails connectionDetailsObject) throws Exception {
    SnowflakeBasicDataSource snowflakeBasicDataSource = new SnowflakeBasicDataSource();
    snowflakeBasicDataSource.setUser(connectionDetailsObject.getUserName());

    String connectionUrl = appendApplicationName(connectionDetailsObject.getConnectionString());
    snowflakeBasicDataSource.setUrl(connectionUrl);

    Map<String, String> additionalProperties = connectionDetailsObject.getAdditionalProperties();
    String isKeyPairAuthentication = additionalProperties.get("IsKeyPairAuthentication");

    if (isKeyPairAuthentication != null && isKeyPairAuthentication.equals("true")) {
      snowflakeBasicDataSource.setPrivateKey(getPrivateKeyObject(additionalProperties.getOrDefault("PrivateKey", ""),
          additionalProperties.getOrDefault("Passphrase", "")));
    } else {
      snowflakeBasicDataSource.setPassword(connectionDetailsObject.getPassword());
    }
    return snowflakeBasicDataSource;
  }

  private static String appendApplicationName(String url) {
    String separator = url.contains("?") ? "&" : "?";
    return url + separator + "application=" + getSnowflakeApplicationValue();
  }

  private static String getSnowflakeApplicationValue() {
    String mxVersion = Core.getRuntimeVersion();
    String mpkVersion = Constants.getExternalDatabaseConnectorVersion();

    return SNOWFLAKE_APPLICATION_PARAMETER_PREFIX + "-" + mxVersion + "_MPK-" + mpkVersion;
  }
}
