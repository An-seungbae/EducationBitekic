package externaldatabaseconnector.database.exceptions;

import externaldatabaseconnector.exceptions.MxException;

import java.util.Map;

public class MxDataTypeNotSupported extends MxException {
  public MxDataTypeNotSupported(String message) {
    super(message);
  }

  public MxDataTypeNotSupported(String message, String messageCode, Map<String, String> messageDataMap) {
    super(message, messageCode, messageDataMap, null);
  }

  public MxDataTypeNotSupported(String message, String messageCode, Map<String, String> messageDataMap, Throwable throwable) {
    super(message, messageCode, messageDataMap, throwable);
  }

  public MxDataTypeNotSupported(String msg, Throwable throwable) {
    super(msg, throwable);
  }
}
