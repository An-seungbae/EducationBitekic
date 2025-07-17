package externaldatabaseconnector.database.constants;

public interface IMxMendixDataTypes {
  String BOOLEAN = "BOOLEAN";
  String INTEGER = "INTEGER";
  String LONG = "LONG";
  String DECIMAL = "DECIMAL";
  String STRING = "STRING";
  String DATETIME = "DATETIME";

  //Following types are not supported in external database connector yet, included to use in future
  String ENUM = "ENUM";
  String AUTONUMBER = "AUTONUMBER";
  String BINARY = "BINARY";
  String HASHSTRING = "HASHSTRING";
}
