package externaldatabaseconnector.livepreview.responses;

import com.mendix.thirdparty.org.json.JSONObject;

import externaldatabaseconnector.livepreview.constants.IMxResponses;

public class ExecuteStatementResponse extends BaseResponse {
  private Integer rowsAffected;

  public Integer getRowsAffected() {
    return rowsAffected;
  }

  public void setRowsAffected(Integer rowsAffected) {
    this.rowsAffected = rowsAffected;
  }

  public JSONObject toJson() {
    JSONObject jsonObject = super.toJson();
    jsonObject.put(IMxResponses.ROWS_AFFECTED, this.rowsAffected);
    return jsonObject;
  }

}
