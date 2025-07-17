package externaldatabaseconnector.database.impl.dbschema.factory;

import externaldatabaseconnector.database.constants.IMxDatabaseSchema;
import externaldatabaseconnector.database.constants.IMxDatabaseTypes;
import externaldatabaseconnector.database.exceptions.MxByodClassNotFoundException;
import externaldatabaseconnector.database.impl.dbschema.provider.*;
import externaldatabaseconnector.exceptions.IMxErrorMessages;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class MxDbSchemaProviderFactory {

  public static MxBaseSchemaInfoProvider getSchemaInfoProvider(String databaseType, Connection connection,
                                                               Map<String, String> additionalConnectionProperties)
      throws SQLException, MxByodClassNotFoundException {

    switch (databaseType) {
      case IMxDatabaseTypes.MSSQL:
        return new MxMSSQLSchemaInfoProvider(connection);
      case IMxDatabaseTypes.MYSQL:
        return new MxMySQLSchemaInfoProvider(connection);
      case IMxDatabaseTypes.ORACLE:
        return new MxOracleSchemaInfoProvider(connection);
      case IMxDatabaseTypes.POSTGRESQL:
        return new MxPostgresSchemaInfoProvider(connection);
      case IMxDatabaseTypes.SNOWFLAKE:
        return new MxSnowflakeSchemaInfoProvider(connection);
      case IMxDatabaseTypes.BYOD:
        return getBYOJdbcSchemaInfoProvider(connection, additionalConnectionProperties);
      default:
        return new MxBYOJdbcSchemaInfoProvider(connection);
    }
  }

  private static MxBaseSchemaInfoProvider getBYOJdbcSchemaInfoProvider(Connection connection,
                                                                       Map<String, String> additionalConnectionProperties)
      throws SQLException, MxByodClassNotFoundException {
    //String schemaInfoProviderType = additionalConnectionProperties.get(IMxDatabaseSchema.SCHEMA_INFO_PROVIDER_TYPE);

    String byodQueryBasedNameSpace =
        additionalConnectionProperties.get(IMxDatabaseSchema.BYOD_QUERY_BASED_SCHEMA_INFO_PROVIDER_NAMESPACE);

    if (byodQueryBasedNameSpace == null || byodQueryBasedNameSpace.isBlank())
      return new MxBYOJdbcSchemaInfoProvider(connection);

    Object instance;
    try {
      Class<?> clazz = Class.forName(byodQueryBasedNameSpace);
      Constructor<?> constructor = clazz.getConstructor(Connection.class);
      instance = constructor.newInstance(connection);
    } catch (Exception exception) {
      String errorMessage = String.format(IMxErrorMessages.BYOD_CLASS_NOT_FOUND, byodQueryBasedNameSpace);
      String messageCode = "E074";

      Map<String, String> messageDataMap = new HashMap<>();
      messageDataMap.put("namespace", byodQueryBasedNameSpace);
      throw new MxByodClassNotFoundException(errorMessage, messageCode, messageDataMap, exception);
    }

    return (MxBaseSchemaInfoProvider) instance;
  }
}
