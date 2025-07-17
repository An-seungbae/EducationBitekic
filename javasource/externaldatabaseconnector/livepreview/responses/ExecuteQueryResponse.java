package externaldatabaseconnector.livepreview.responses;

import com.mendix.thirdparty.org.json.JSONObject;

import externaldatabaseconnector.livepreview.constants.IMxResponses;
import externaldatabaseconnector.livepreview.responses.pojo.TableResult;

public class ExecuteQueryResponse extends BaseResponse {
  private TableResult resultSet;

  public TableResult getResultSet() {
    return resultSet;
  }

  public void setResultSet(TableResult resultSet) {
    this.resultSet = resultSet;
  }

  public JSONObject toJson() {
    JSONObject jsonObject = super.toJson();
    if (this.resultSet != null) {
      jsonObject.put(IMxResponses.RESULT_SET, this.resultSet.toJson());
    }
    return jsonObject;
  }
}
