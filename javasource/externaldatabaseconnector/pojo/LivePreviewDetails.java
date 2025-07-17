package externaldatabaseconnector.pojo;

public class LivePreviewDetails {
  private String token;
  private MxDatabase database;

  public LivePreviewDetails(String token, MxDatabase database) {
    this.token = token;
    this.database = database;
  }

  public String getToken() {
    return token;
  }

  public MxDatabase getDatabase() {
    return database;
  }
}
