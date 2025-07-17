package externaldatabaseconnector.exceptions;

import com.mendix.thirdparty.org.json.JSONObject;

import java.util.Map;

public class MxException extends Exception {
  private String messageCode;
  private Map<String, String> messageDataMap;

  public MxException(String message) {
    super(message);
    this.messageCode = null;
    this.messageDataMap = null;
  }

  public MxException(String message, String messageCode, Map<String, String> messageDataMap, Throwable throwable) {
    super(message, throwable);
    this.messageCode = messageCode;
    this.messageDataMap = messageDataMap;
  }

  public MxException(String message, Throwable throwable) {
    super(message, throwable);
    this.messageCode = null;
    this.messageDataMap = null;
  }

  public String getMessageExtended() {
    if (messageCode != null && !messageCode.isEmpty()) {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("Code", messageCode);
      jsonObject.put("Data", messageDataMap);
      return jsonObject.toString();
    }
    return getMessage();
  }
}
