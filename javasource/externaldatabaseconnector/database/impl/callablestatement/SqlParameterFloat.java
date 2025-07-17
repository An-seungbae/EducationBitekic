package externaldatabaseconnector.database.impl.callablestatement;

import externaldatabaseconnector.pojo.QueryParameter;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.SQLException;

public class SqlParameterFloat extends SqlParameterPrimitiveValue<BigDecimal> {

  public SqlParameterFloat(QueryParameter queryParameter, int index, int sqlType) {
    super(queryParameter, sqlType, index);
  }
  
  // converting the Float value to BigDecimal as mendix accepts BigDecimal
  public BigDecimal getStatementValue(CallableStatement callableStatement) throws SQLException {
    return BigDecimal.valueOf(callableStatement.getFloat(this.parameterIndex));
  }

  public void setStatementValue(CallableStatement callableStatement, String value) throws SQLException {
    callableStatement.setFloat(this.parameterIndex, Float.parseFloat(value));
  }
}
