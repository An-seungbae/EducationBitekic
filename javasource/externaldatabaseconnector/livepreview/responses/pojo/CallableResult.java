package externaldatabaseconnector.livepreview.responses.pojo;

import com.mendix.thirdparty.org.json.JSONObject;

import externaldatabaseconnector.livepreview.constants.IMxResponses;

public class CallableResult {
  private Integer rowsAffected;
  private TableResult resultSet;
  private CallableParameterResult callableParameterResult;

  public CallableResult(Integer rowsAffected, TableResult resultSet, CallableParameterResult callableParameterResult) {
    this.rowsAffected = rowsAffected;
    this.resultSet = resultSet;
    this.callableParameterResult = callableParameterResult;
  }

  public Integer getRowsAffected() {
    return rowsAffected;
  }

  public TableResult getResultSet() {
    return resultSet;
  }

  public CallableParameterResult getCallableParameterResult() {
    return callableParameterResult;
  }

  public JSONObject toJson() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(IMxResponses.ROWS_AFFECTED, this.rowsAffected);

    if (resultSet != null)
      jsonObject.put(IMxResponses.RESULT_SET, resultSet.toJson());

    if (callableParameterResult != null)
      jsonObject.put(IMxResponses.CALLABLE_PARAMETER_RESULT, this.callableParameterResult.toJson());
    
    return jsonObject;
  }
}
