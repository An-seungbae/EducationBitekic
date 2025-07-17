package externaldatabaseconnector.exceptions;

public interface IMxErrorMessages {
  String INVALID_COLUMN_NAME =
      "The selected column name '%s' is not valid. Names should start with a letter or underscore and can only contain letters, digits and underscores.";
  String INVALID_PARAMETER_MODE = "Unrecognized parameter mode %s.";
  String INVALID_PARAMETER_TYPE = "The parameter type '%s' is invalid, for the parameter named '%s'.";
  String SQL_DATA_TYPE_NOT_SUPPORTED = "Sql data type '%S' is not supported";
  String BYOD_CLASS_NOT_FOUND = "Error while trying to instantiate the class '%s'. Ensure the namespace is correct.";

  String INVALID_PRIVATE_KEY = "Private key is invalid";

}
