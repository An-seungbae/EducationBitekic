package externaldatabaseconnector.database.constants;

public interface IMxDatabaseSchema {
  String MAIN = "main";
  String BASE_TABLE = "BASE TABLE";
  String TABLE_TYPE = "TABLE";
  String VIEW_TYPE = "VIEW";

  String PROCEDURE_TYPE = "PROCEDURE_TYPE";
  String FUNCTION_TYPE = "FUNCTION_TYPE";
  String PROCEDURE_NAME = "PROCEDURE_NAME";

  String FUNCTION_NAME = "FUNCTION_NAME";
  String TABLE_NAME = "TABLE_NAME";
  String COLUMN_NAME = "COLUMN_NAME";
  String COLUMN_TYPE = "COLUMN_TYPE";
  String TYPE_NAME = "TYPE_NAME";


  // additional properties related to get schema
  String SCHEMA_INFO_PROVIDER_TYPE = "SchemaInfoProviderType";
  String METADATA_BASED_SCHEMA_INFO_PROVIDER_TYPE = "MetaDataBased";
  String BYOD_QUERY_BASED_SCHEMA_INFO_PROVIDER_NAMESPACE = "ByodQueryBasedNamespace";
}
