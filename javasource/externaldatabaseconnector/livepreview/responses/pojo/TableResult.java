package externaldatabaseconnector.livepreview.responses.pojo;

import com.mendix.thirdparty.org.json.JSONArray;
import com.mendix.thirdparty.org.json.JSONObject;

import externaldatabaseconnector.livepreview.constants.IMxResponses;

import java.util.List;

public class TableResult {
  private final List<ColumnMetaData> columnMetadataList;
  private final List<RowData> rowDataList;
  private final Integer resultRecordCount;

  public TableResult(List<ColumnMetaData> columnMetadataList, List<RowData> rowDataList, Integer resultRecordCount) {
    this.columnMetadataList = columnMetadataList;
    this.rowDataList = rowDataList;
    this.resultRecordCount = resultRecordCount;
  }

  public List<ColumnMetaData> getColumnMetadataList() {
    return columnMetadataList;
  }

  public List<RowData> getRowDataList() {
    return rowDataList;
  }

  public Integer getResultRecordCount() {
    return resultRecordCount;
  }

  public JSONObject toJson() {
    JSONObject jsonObject = new JSONObject();

    // Convert column metadata list to JSON
    JSONArray columnArray = new JSONArray();
    if (columnMetadataList != null) {
      for (ColumnMetaData column : columnMetadataList) {
        columnArray.put(column.toJson());
      }
    }
    jsonObject.put(IMxResponses.COLUMN_METADATA_LIST, columnArray);

    // Convert row data list to JSON
    JSONArray rowArray = new JSONArray();
    if (rowDataList != null) {
      for (RowData row : rowDataList) {
        rowArray.put(row.toJson());
      }
    }
    jsonObject.put(IMxResponses.ROW_DATA_LIST, rowArray);
    jsonObject.put(IMxResponses.RESULT_RECORD_COUNT, resultRecordCount);
    return jsonObject;
  }
}
