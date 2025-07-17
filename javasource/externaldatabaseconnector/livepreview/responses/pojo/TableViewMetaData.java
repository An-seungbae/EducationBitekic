package externaldatabaseconnector.livepreview.responses.pojo;

import com.mendix.thirdparty.org.json.JSONArray;
import com.mendix.thirdparty.org.json.JSONObject;

import externaldatabaseconnector.livepreview.constants.IMxResponses;

import java.util.List;

public class TableViewMetaData extends BaseMetaData{
  private List<ColumnMetaData> columnMetaDataList;

  public TableViewMetaData(String name, List<ColumnMetaData> columnMetaDataList, String schemaName) {
    super(name, schemaName);
    this.columnMetaDataList = columnMetaDataList;
  }

  public List<ColumnMetaData> getColumnMetaDataList() {
    return columnMetaDataList;
  }

  public JSONObject toJson() {
    JSONObject jsonObject = new JSONObject();

    JSONArray columnJsonList = new JSONArray();
    for(ColumnMetaData columnMetaData : columnMetaDataList){
      columnJsonList.put(columnMetaData.toJson());
    }

    jsonObject.put(IMxResponses.NAME, getName());
    jsonObject.put(IMxResponses.COLUMN_METADATA_LIST, columnJsonList);
    jsonObject.put(IMxResponses.SCHEMA_NAME, getSchemaName());

    return jsonObject;
  }
}
