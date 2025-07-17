package externaldatabaseconnector.livepreview.responses.pojo;

import com.mendix.thirdparty.org.json.JSONArray;
import com.mendix.thirdparty.org.json.JSONObject;

import externaldatabaseconnector.livepreview.constants.IMxResponses;

import java.util.List;

public class SchemaMetaData {
  private String name;
  private List<TableViewMetaData> tableMetaDataList;
  private List<TableViewMetaData> viewMetaDataList;
  private List<CallableMetaData> procedureMetaDataList;
  private List<CallableMetaData> functionMetaDataList;

  public SchemaMetaData(String name,
                        List<TableViewMetaData> tableMetaDataList,
                        List<TableViewMetaData> viewMetaDataList,
                        List<CallableMetaData> procedureMetaDataList,
                        List<CallableMetaData> functionMetaDataList) {
    this.name = name;
    this.tableMetaDataList = tableMetaDataList;
    this.viewMetaDataList = viewMetaDataList;
    this.procedureMetaDataList = procedureMetaDataList;
    this.functionMetaDataList = functionMetaDataList;
  }

  public String getName() {
    return name;
  }

  public JSONObject toJson() {
    JSONObject jsonObject = new JSONObject();

    JSONArray tableJsonList = new JSONArray();
    for (TableViewMetaData tableMetaData : tableMetaDataList) {
      tableJsonList.put(tableMetaData.toJson());
    }

    JSONArray viewJsonList = new JSONArray();
    for (TableViewMetaData viewMetaData : viewMetaDataList) {
      viewJsonList.put(viewMetaData.toJson());
    }

    JSONArray procedureJsonList = new JSONArray();
    for (CallableMetaData procedureMetaData : procedureMetaDataList) {
      procedureJsonList.put(procedureMetaData.toJson());
    }

    JSONArray functionJsonList = new JSONArray();
    for (CallableMetaData functionMetaData : functionMetaDataList) {
      functionJsonList.put(functionMetaData.toJson());
    }

    jsonObject.put(IMxResponses.NAME, name);
    jsonObject.put(IMxResponses.TABLE_METADATA_LIST, tableJsonList);
    jsonObject.put(IMxResponses.VIEW_METADATA_LIST, viewJsonList);
    jsonObject.put(IMxResponses.PROCEDURE_METADATA_LIST, procedureJsonList);
    jsonObject.put(IMxResponses.FUNCTION_METADATA_LIST, functionJsonList);

    return jsonObject;
  }
}
