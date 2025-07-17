package externaldatabaseconnector.livepreview.responses.pojo;

import com.mendix.thirdparty.org.json.JSONObject;

import externaldatabaseconnector.database.enums.CallableParameterMode;
import externaldatabaseconnector.livepreview.constants.IMxResponses;
import externaldatabaseconnector.pojo.BaseParameter;

public class CallableParameter extends BaseParameter {
  private Object parameterValue;

  public CallableParameter(String name, String sqlDataType, CallableParameterMode parameterMode, Object parameterValue, String dataType) {
    super(name, dataType, sqlDataType, parameterMode);
    this.parameterValue = parameterValue;
  }

  public JSONObject toJson() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(IMxResponses.NAME, getName());
    jsonObject.put(IMxResponses.DATA_TYPE, getDataType());
    jsonObject.put(IMxResponses.SQL_DATE_TYPE, getSqlDataType());
    jsonObject.put(IMxResponses.PARAMETER_MODE, getParameterMode().name());
    jsonObject.put(IMxResponses.PARAMETER_VALUE, parameterValue);
    return jsonObject;
  }
}
