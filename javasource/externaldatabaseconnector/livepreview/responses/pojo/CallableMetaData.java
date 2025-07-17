package externaldatabaseconnector.livepreview.responses.pojo;

import com.mendix.thirdparty.org.json.JSONArray;
import com.mendix.thirdparty.org.json.JSONObject;

import externaldatabaseconnector.livepreview.constants.IMxResponses;

import java.util.List;

public class CallableMetaData extends BaseMetaData{
  private List<CallableParameter> callableParameterList;
  private String returnType;

  public CallableMetaData(String name, List<CallableParameter> callableParameterList, String schemaName, String returnType) {
    super(name, schemaName);
    this.callableParameterList = callableParameterList;
    this.returnType = returnType;
  }

  public JSONObject toJson() {
    JSONObject jsonObject = new JSONObject();

    JSONArray paramJsonList = new JSONArray();
    for(CallableParameter paramMetaData : callableParameterList){
      paramJsonList.put(paramMetaData.toJson());
    }

    jsonObject.put(IMxResponses.NAME, getName());
    jsonObject.put(IMxResponses.CALLABLE_PARAMETER_LIST, paramJsonList);
    jsonObject.put(IMxResponses.SCHEMA_NAME, getSchemaName());
    jsonObject.put(IMxResponses.RETURN_TYPE, returnType);

    return jsonObject;
  }
}
