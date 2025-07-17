package externaldatabaseconnector.database.impl.callablestatement;

import externaldatabaseconnector.mx.impl.DatabaseConnectorException;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StatementWrapper  implements AutoCloseable {
    final private CallableStatement cStatement;

    public StatementWrapper(final CallableStatement cStatement) {
        this.cStatement = cStatement;
    }

    /**
     * Call Stored procedure returning the resultset for the select queries.
     */
    public ResultSet execute() throws SQLException, DatabaseConnectorException {
        boolean result = this.cStatement.execute();
        if(result)
            return this.cStatement.getResultSet();
        return null;
    }

    /**
     * Call Stored procedure returning the number of affected rows.
     */

    public long executeUpdate() throws SQLException, DatabaseConnectorException {
        boolean result = this.cStatement.execute();
        if(!result)
            return this.cStatement.getUpdateCount();
        return -1;
    }

    @Override
    public void close() throws SQLException {
        this.cStatement.close();
    }

    /**
     * Call Stored procedure returning the callable statement for the stored procedure queries created using
     * Studio Pro 10.13 and above.
     */
    public CallableStatement executeCallable() throws SQLException {
        this.cStatement.execute();
        return this.cStatement;
    }
}