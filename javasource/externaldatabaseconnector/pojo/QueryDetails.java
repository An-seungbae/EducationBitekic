package externaldatabaseconnector.pojo;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class QueryDetails {

  @SerializedName("Query")
  private String query;

  @SerializedName("QueryType")
  private String queryType;

  @SerializedName("QueryParameters")
  private List<QueryParameter> queryParameters;

  public QueryDetails(String query, String queryType, List<QueryParameter> queryParameters) {
    this.query = query;
    this.queryType = queryType;
    this.queryParameters = queryParameters;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public String getQuery() {
    return query;
  }

  public String getQueryType() {
    return queryType;
  }

  public void setQueryParameters(List<QueryParameter> queryParameters) {
    this.queryParameters = queryParameters;
  }

  public List<QueryParameter> getQueryParameters() {
    return queryParameters;
  }
}
