package externaldatabaseconnector.database.utils;

import externaldatabaseconnector.database.enums.CallableParameterMode;

import java.sql.ParameterMetaData;

public class MxParameterModeUtils {
  public static CallableParameterMode getParameterMode(int parameterMode) {
    switch (parameterMode) {
      case ParameterMetaData.parameterModeIn:
        return CallableParameterMode.IN;
      case ParameterMetaData.parameterModeInOut:
        return CallableParameterMode.INOUT;
      case ParameterMetaData.parameterModeOut:
        return CallableParameterMode.OUT;
      default:
        return CallableParameterMode.UNKNOWN;
    }
  }
}
