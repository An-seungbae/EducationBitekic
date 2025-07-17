package externaldatabaseconnector.database.utils;

import externaldatabaseconnector.database.enums.CallableParameterMode;
import externaldatabaseconnector.database.exceptions.MxDataTypeNotSupported;
import externaldatabaseconnector.database.impl.callablestatement.SqlParameterPrimitiveValue;
import externaldatabaseconnector.database.impl.parameterFactory.SqlParameterFactory;
import externaldatabaseconnector.livepreview.responses.pojo.CallableParameter;
import externaldatabaseconnector.livepreview.responses.pojo.CallableParameterResult;
import externaldatabaseconnector.pojo.QueryParameter;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MxCallableParameterUtils {
  public static CallableParameterResult readCallableParameterValues(List<QueryParameter> queryParameters,
                                                                    CallableStatement callableStatementResult)
      throws SQLException, MxDataTypeNotSupported {

    List<CallableParameter> callableParameterList = new ArrayList<>();

    int index = 1;
    for (QueryParameter queryParameter : queryParameters) {
      if (queryParameter.getParameterMode().equals(CallableParameterMode.OUT) || queryParameter.getParameterMode()
          .equals(CallableParameterMode.INOUT)) {

        // Read parameter value from the callable statement
        SqlParameterPrimitiveValue sqlParameter = SqlParameterFactory.getSqlParameter(queryParameter, index);
        Object parameterValue = sqlParameter.getStatementValue(callableStatementResult);

        CallableParameter callableParameter =
            new CallableParameter(queryParameter.getDatabaseParameterName(), queryParameter.getSqlDataType(),
                queryParameter.getParameterMode(), parameterValue, queryParameter.getDataType());
        callableParameterList.add(callableParameter);
      }
      index++;
    }
    return new CallableParameterResult(callableParameterList);
  }
}
