package externaldatabaseconnector.livepreview.responses.pojo;

public class BaseMetaData {
  private String name;
  private String schemaName;

  public BaseMetaData(String name, String schemaName) {
    this.name = name;
    this.schemaName = schemaName;
  }

  public String getName() {
    return name;
  }

  public String getSchemaName() {
    return schemaName;
  }

}
