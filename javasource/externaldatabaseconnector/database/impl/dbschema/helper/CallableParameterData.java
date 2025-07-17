package externaldatabaseconnector.database.impl.dbschema.helper;

import externaldatabaseconnector.livepreview.responses.pojo.CallableParameter;

import java.util.List;

public class CallableParameterData {
  private final List<CallableParameter> parameters;
  private final String returnType;

  public CallableParameterData(List<CallableParameter> parameters, String returnType) {
    this.parameters = parameters;
    this.returnType = returnType;
  }

  public List<CallableParameter> getParameters() {
    return parameters;
  }

  public String getReturnType() {
    return returnType;
  }
}
