package externaldatabaseconnector.database.impl.callablestatement;

import externaldatabaseconnector.pojo.QueryParameter;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;

public class SqlParameterTime extends SqlParameterPrimitiveValue<Time> {
  public SqlParameterTime(QueryParameter queryParameter, int index) {
    super(queryParameter, Types.TIME, index);
  }

  public Time getStatementValue(CallableStatement callableStatement) throws SQLException {
    return callableStatement.getTime(this.parameterIndex);
  }

  public void setStatementValue(CallableStatement callableStatement, String value) throws SQLException {
    long epochTime = Long.parseLong(value);
    callableStatement.setTime(this.parameterIndex, new Time(epochTime));
  }
}
