package externaldatabaseconnector.pojo;

import com.google.gson.annotations.SerializedName;
import externaldatabaseconnector.database.enums.CallableParameterMode;

public class QueryParameter extends BaseParameter {
  @SerializedName(value="Value", alternate={"TestValue"})
  private String value;

  @SerializedName("DatabaseParameterName")
  private String databaseParameterName;

  public QueryParameter(String name, String dataType, String sqlDataType, String value, CallableParameterMode parameterMode,
                        String databaseParameterName) {
    super(name, dataType, sqlDataType, parameterMode);
    this.value = value;
    this.databaseParameterName = databaseParameterName;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getDatabaseParameterName() {
    return databaseParameterName;
  }
}
