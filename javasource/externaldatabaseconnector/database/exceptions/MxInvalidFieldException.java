package externaldatabaseconnector.database.exceptions;
import externaldatabaseconnector.exceptions.MxException;

import java.util.Map;

public class MxInvalidFieldException extends MxException {

  public MxInvalidFieldException(String msg) {
    super(msg);
  }

  public MxInvalidFieldException(String msg, String messageCode, Map<String, String> messageDataMap, Throwable throwable) {
    super(msg, messageCode, messageDataMap, throwable);
  }

  public MxInvalidFieldException(String msg, Throwable throwable) {
    super(msg, throwable);
  }
}
