package externaldatabaseconnector.livepreview.constants;

public interface IMxResponses {
  //Base response
  String MESSAGE = "Message";
  String STATUS = "ActionStatus";
  String STACKTRACE = "Stacktrace";
  String MESSAGE_CODE = "MessageCode";

  //Test connection response
  String IS_CONNECTION_SUCCESS = "IsConnectionSuccess";

  //Get schemaInfo response
  String DEFAULT_SCHEMA = "DefaultSchema";
  String SCHEMA_METADATA_LIST = "SchemaMetaDataList";

  // metadata common properties
  String NAME = "Name";
  String SCHEMA_NAME = "SchemaName";

  // SchemaMetaData
  String TABLE_METADATA_LIST = "TableMetaDataList";
  String VIEW_METADATA_LIST = "ViewMetaDataList";
  String PROCEDURE_METADATA_LIST = "ProcedureMetaDataList";
  String FUNCTION_METADATA_LIST = "FunctionMetaDataList";

  // TableViewMetaData
  String COLUMN_METADATA_LIST = "ColumnMetaDataList";

  //Column Metadata
  String DATA_TYPE = "DataType";
  String SQL_DATE_TYPE = "SqlDataType";
  String COLUMN_SIZE = "ColumnSize";

  // CallableMetaData
  String CALLABLE_PARAMETER_LIST = "CallableParameterList";
  String RETURN_TYPE = "ReturnType";

  //CallableParameter
  String PARAMETER_MODE = "ParameterMode";
  String PARAMETER_VALUE = "ParameterValue";

  //Execute select response
  String RESULT_SET = "ResultSet";

  //Table Result
  String ROW_DATA_LIST = "RowDataList";
  String RESULT_RECORD_COUNT = "ResultRecordCount";

  //Row Data
  String ROW = "RowList";

  //Execute Statement Response
  String ROWS_AFFECTED = "RowsAffected";

  //Execute Callable response
  String CALLABLE_RESULT = "CallableResult";

  //Callable Parameter Result
  String CALLABLE_PARAMETER_RESULT = "CallableParameterResult";

  //Callable parameters result table header columns
  String PARAMETER_RESULT_COLUMNS = "ParameterResultColumnList";

  //Callable parameters with result value and sqlType
  String PARAMETER_RESULT = "CallableParameterList";

  //Callable Parameter header columns
  String COLUMN_HEADER_MODE = "Mode";
  String COLUMN_HEADER_NAME = "Name";
  String COLUMN_HEADER_VALUE = "Value";
  String DATABASE_PRODUCT_NAME = "DatabaseProductName";
}
