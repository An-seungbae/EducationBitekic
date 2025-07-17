package externaldatabaseconnector.livepreview.responses.pojo;

import com.mendix.thirdparty.org.json.JSONObject;

import externaldatabaseconnector.livepreview.constants.IMxResponses;

public class ColumnMetaData {
  private String name;

  //Mendix Data types, Integer, String, DATETIME etc
  private String dataType;

  private String sqlDataType;

  private int columnSize;

  public ColumnMetaData(String name, String dataType, String sqlDataType, int columnSize) {
    this.name = name;
    this.dataType = dataType;
    this.sqlDataType = sqlDataType;
    this.columnSize = columnSize;
  }

  public String getName() {
    return name;
  }

  public String getDataType() {
    return dataType;
  }

  public String getSqlDataType() {
    return sqlDataType;
  }

  public int getColumnSize() {
    return columnSize;
  }

  public JSONObject toJson() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(IMxResponses.NAME, this.name);
    jsonObject.put(IMxResponses.DATA_TYPE, this.dataType);
    jsonObject.put(IMxResponses.SQL_DATE_TYPE, this.sqlDataType);
    jsonObject.put(IMxResponses.COLUMN_SIZE, this.columnSize);
    return jsonObject;
  }
}
