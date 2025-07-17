package externaldatabaseconnector.pojo;

import com.google.gson.annotations.SerializedName;

public class MxDatabase {

  @SerializedName("ConnectionDetails")
  private ConnectionDetails connectionDetails;

  @SerializedName("QueryDetails")
  private QueryDetails queryDetails;

  public MxDatabase(ConnectionDetails connectionDetails, QueryDetails queryDetails) {
    this.connectionDetails = connectionDetails;
    this.queryDetails = queryDetails;
  }

  public ConnectionDetails getConnectionDetails() {
    return connectionDetails;
  }

  public QueryDetails getQueryDetails() {
    return queryDetails;
  }
}
