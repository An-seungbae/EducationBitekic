package externaldatabaseconnector.livepreview.responses;

import com.mendix.thirdparty.org.json.JSONArray;
import com.mendix.thirdparty.org.json.JSONObject;

import externaldatabaseconnector.livepreview.constants.IMxResponses;
import externaldatabaseconnector.livepreview.responses.pojo.SchemaMetaData;

import java.util.ArrayList;
import java.util.List;

public class MxGetSchemaInfoResponse extends BaseResponse {
  private String defaultSchema;
  private List<SchemaMetaData> schemaMetaDataList = new ArrayList<>();

  public String getDefaultSchema() {
    return defaultSchema;
  }

  public List<SchemaMetaData> getSchemaMetaDataList() {
    return schemaMetaDataList;
  }

  public void setDefaultSchema(String defaultSchema) {
    this.defaultSchema = defaultSchema;
  }
  public void setSchemaMetaDataList(List<SchemaMetaData> schemaMetaDataList) {
    this.schemaMetaDataList = schemaMetaDataList;
  }

  public JSONObject toJson() {
    JSONObject jsonObject = super.toJson();

    jsonObject.put(IMxResponses.DEFAULT_SCHEMA, defaultSchema);

    JSONArray schemaJsonList = new JSONArray();
    for(SchemaMetaData schemaMetaData : schemaMetaDataList){
      schemaJsonList.put(schemaMetaData.toJson());
    }
    jsonObject.put(IMxResponses.SCHEMA_METADATA_LIST, schemaJsonList);

    return jsonObject;
  }

}
