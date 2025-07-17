package externaldatabaseconnector.database.impl.dbschema.provider;

import java.sql.Connection;
import java.sql.SQLException;

public class MxBYOJdbcSchemaInfoProvider extends MxMetaDataBasedSchemaInfoProvider{
  public MxBYOJdbcSchemaInfoProvider(Connection connection) throws SQLException {
    super(connection);
  }
}
