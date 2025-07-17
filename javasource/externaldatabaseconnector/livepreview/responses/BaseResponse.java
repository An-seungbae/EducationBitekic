package externaldatabaseconnector.livepreview.responses;

import com.mendix.thirdparty.org.json.JSONObject;

import externaldatabaseconnector.database.exceptions.MxDataTypeNotSupported;
import externaldatabaseconnector.exceptions.MxException;
import externaldatabaseconnector.livepreview.constants.IMxResponses;

import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class BaseResponse {
  private boolean actionStatus = false;
  private boolean isConnectionSuccess = false;
  private String message;
  private String callStackTrace;
  private String messageCode;
  private String databaseProductName;

  public boolean getActionStatus() {
    return actionStatus;
  }

  public void setActionStatus(boolean actionStatus) {
    this.actionStatus = actionStatus;
  }

  public boolean isConnectionSuccess() {
    return isConnectionSuccess;
  }

  public void setConnectionSuccess(boolean connectionSuccess) {
    isConnectionSuccess = connectionSuccess;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public void setMessageCode(String aMessageCode){
    this.messageCode = aMessageCode;
  }

  public void setCallStackTrace(String callStackTrace) {
    this.callStackTrace = callStackTrace;
  }

  public void setDatabaseProductName(String aDatabaseProductName) {
    this.databaseProductName = aDatabaseProductName;
  }

  //TODO:Try to convert POJO into a JSON objects instead of implementing the toJSON method in other responses
  public JSONObject toJson() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(IMxResponses.STATUS, this.actionStatus);
    jsonObject.put(IMxResponses.IS_CONNECTION_SUCCESS, this.isConnectionSuccess);
    jsonObject.put(IMxResponses.MESSAGE, this.message);
    jsonObject.put(IMxResponses.STACKTRACE, this.callStackTrace);
    jsonObject.put(IMxResponses.MESSAGE_CODE, this.messageCode);
    jsonObject.put(IMxResponses.DATABASE_PRODUCT_NAME, this.databaseProductName);
    return jsonObject;
  }

  public void setExceptionDetails(Exception aException) {
    this.message = extractMessage(aException);
    this.callStackTrace = Arrays.stream(aException.getStackTrace())
        .map(StackTraceElement::toString)
        .collect(Collectors.joining("\n"));
  }

  private String extractMessage(Exception aException) {
    // Check if the exception itself is an instance of MxException; otherwise, retrieve its cause.
    Throwable cause = (aException instanceof MxException) ? aException : aException.getCause();

    // If the cause is an instance of MxException, return its extended message with message code and data; otherwise, use the default message.
    return (cause instanceof MxException) ? ((MxException) cause).getMessageExtended() : aException.getMessage();
  }
}
