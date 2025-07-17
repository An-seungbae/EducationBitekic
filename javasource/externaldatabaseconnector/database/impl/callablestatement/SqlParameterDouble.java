package externaldatabaseconnector.database.impl.callablestatement;

import externaldatabaseconnector.pojo.QueryParameter;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;

public class SqlParameterDouble extends SqlParameterPrimitiveValue<BigDecimal> {
  public SqlParameterDouble(QueryParameter queryParameter, int index) {
    super(queryParameter, Types.DOUBLE, index);
  }

  // converting the Double value to BigDecimal as mendix accepts BigDecimal
  public BigDecimal getStatementValue(CallableStatement callableStatement) throws SQLException {
    return BigDecimal.valueOf(callableStatement.getDouble(this.parameterIndex));
  }

  public void setStatementValue(CallableStatement callableStatement, String value) throws SQLException {
    callableStatement.setDouble(this.parameterIndex, Double.parseDouble(value));
  }
}
