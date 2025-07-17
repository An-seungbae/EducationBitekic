package externaldatabaseconnector.utils;

import externaldatabaseconnector.pojo.QueryParameter;

public class ParameterUtil {
  private static final String GUID_FOR_HARDCODED_NULL_STRING = "903be4ca-ed8b-ddcb-a339-741c4088484a";

  public static void CheckForHardCodedNullString(String paramObjectValue, QueryParameter paramObject){
    if(paramObjectValue.equals(GUID_FOR_HARDCODED_NULL_STRING))
      paramObject.setValue("null");
  }
}
