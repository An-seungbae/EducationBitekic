package externaldatabaseconnector.livepreview.responses.pojo;

import com.mendix.thirdparty.org.json.JSONArray;
import com.mendix.thirdparty.org.json.JSONObject;

import externaldatabaseconnector.livepreview.constants.IMxResponses;

import java.util.List;

public class RowData {
  private List<Object> row;

  public List<Object> getRow() {
    return row;
  }

  public void setRow(List<Object> row) {
    this.row = row;
  }

  public JSONObject toJson() {
    JSONObject jsonObject = new JSONObject();
    JSONArray rowArray = new JSONArray();
    if (row != null) {
      for (Object cell : row) {
        rowArray.put(cell);
      }
    }
    jsonObject.put(IMxResponses.ROW, rowArray);
    return jsonObject;
  }
}
