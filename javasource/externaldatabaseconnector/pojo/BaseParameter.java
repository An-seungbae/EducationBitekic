package externaldatabaseconnector.pojo;

import com.google.gson.annotations.SerializedName;
import externaldatabaseconnector.database.enums.CallableParameterMode;

public class BaseParameter {
  @SerializedName("Name")
  private final String name;
  @SerializedName("DataType")
  private final String dataType;
  @SerializedName("SqlDataType")
  private final String sqlDataType;
  @SerializedName("ParameterMode")
  private CallableParameterMode parameterMode;

  public BaseParameter(String name, String dataType, String sqlDataType, CallableParameterMode parameterMode) {
    this.name = name;
    this.dataType = dataType;
    this.sqlDataType = sqlDataType;
    this.parameterMode = parameterMode;
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

  public CallableParameterMode getParameterMode() {
    return parameterMode;
  }

}
