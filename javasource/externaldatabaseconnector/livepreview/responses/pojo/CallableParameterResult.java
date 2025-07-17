package externaldatabaseconnector.livepreview.responses.pojo;

import com.mendix.thirdparty.org.json.JSONArray;
import com.mendix.thirdparty.org.json.JSONObject;

import externaldatabaseconnector.livepreview.constants.IMxResponses;

import java.util.Arrays;
import java.util.List;

public class CallableParameterResult {
  private List<String> parameterResultColumnList =
      Arrays.asList(IMxResponses.COLUMN_HEADER_MODE, IMxResponses.COLUMN_HEADER_NAME, IMxResponses.COLUMN_HEADER_VALUE);
  private List<CallableParameter> parameterResultList;

  public CallableParameterResult(List<CallableParameter> parameterResultList) {
    this.parameterResultList = parameterResultList;
  }

  public List<String> getParameterResultColumnList() {
    return parameterResultColumnList;
  }

  public List<CallableParameter> getParameterResultList() {
    return parameterResultList;
  }

  public JSONObject toJson() {
    JSONObject jsonObject = new JSONObject();

    // removing the parameterResultColumnList from toJson as default value is already there in C# when deserialized
    /*
    JSONArray parameterResultColumnJsonArray = new JSONArray(parameterResultColumnList);
    jsonObject.put(IMxResponses.PARAMETER_RESULT_COLUMNS, parameterResultColumnJsonArray);
     */

    JSONArray parameterResultJsonArray = new JSONArray();
    for (CallableParameter callableParameter : parameterResultList) {
      parameterResultJsonArray.put(callableParameter.toJson());
    }
    jsonObject.put(IMxResponses.PARAMETER_RESULT, parameterResultJsonArray);
    return jsonObject;
  }
}
