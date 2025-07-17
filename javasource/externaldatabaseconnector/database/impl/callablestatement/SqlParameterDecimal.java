package externaldatabaseconnector.database.impl.callablestatement;

import externaldatabaseconnector.pojo.QueryParameter;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;

public class SqlParameterDecimal extends SqlParameterPrimitiveValue<BigDecimal> {
    public SqlParameterDecimal(QueryParameter queryParameter, int index) {
        super(queryParameter, Types.DECIMAL, index);
    }

    public BigDecimal getStatementValue(CallableStatement callableStatement) throws SQLException {
        return callableStatement.getBigDecimal(this.parameterIndex);
    }

    public void setStatementValue(CallableStatement callableStatement, String value) throws SQLException {
        callableStatement.setBigDecimal(this.parameterIndex, new BigDecimal(value));
    }
}
