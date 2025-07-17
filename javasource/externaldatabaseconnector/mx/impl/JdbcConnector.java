package externaldatabaseconnector.mx.impl;

import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.systemwideinterfaces.core.meta.IMetaObject;

import externaldatabaseconnector.database.exceptions.MxDataTypeNotSupported;
import externaldatabaseconnector.database.impl.connection.ConnectionManagerSingleton;
import externaldatabaseconnector.database.impl.statement.CallableStatementCreatorImpl;
import externaldatabaseconnector.database.impl.statement.PreparedStatementCreatorImpl;
import externaldatabaseconnector.database.interfaces.CallableStatementCreator;
import externaldatabaseconnector.database.interfaces.ConnectionManager;
import externaldatabaseconnector.database.interfaces.PreparedStatementCreator;
import externaldatabaseconnector.database.impl.callablestatement.StatementWrapper;
import externaldatabaseconnector.exceptions.MxException;
import externaldatabaseconnector.mx.impl.service.CallableStatementService;
import externaldatabaseconnector.mx.impl.service.MendixObjectService;
import externaldatabaseconnector.mx.interfaces.ObjectInstantiator;
import externaldatabaseconnector.pojo.ColumnMapping;
import externaldatabaseconnector.pojo.ConnectionDetails;
import externaldatabaseconnector.pojo.QueryParameter;
import externaldatabaseconnector.utils.JsonUtil;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;

/**
 * JdbcConnector implements the execute query (and execute statement)
 * functionality, and returns a {@link Stream} of {@link IMendixObject}s.
 */
public class JdbcConnector {
    private final ILogNode logNode;
    private final ObjectInstantiator objectInstantiator;
    private final ConnectionManager connectionManager;
    public final PreparedStatementCreator preparedStatementCreator;
    public final CallableStatementCreator callableStatementCreator;

    public JdbcConnector(final ILogNode logNode, final ObjectInstantiator objectInstantiator,
                         final ConnectionManager connectionManager, final PreparedStatementCreator preparedStatementCreator, final CallableStatementCreator callableStatementCreator
    ) {
        this.logNode = logNode;
        this.objectInstantiator = objectInstantiator;
        this.connectionManager = connectionManager;
        this.preparedStatementCreator = preparedStatementCreator;
        this.callableStatementCreator = callableStatementCreator;
    }

    public JdbcConnector(final ILogNode logNode) {
        this(logNode, new ObjectInstantiatorImpl(), ConnectionManagerSingleton.getInstance(logNode),
                new PreparedStatementCreatorImpl(), new CallableStatementCreatorImpl());
    }

    public List<IMendixObject> executeQuery(final String connectionDetailsJsonString,
                                            final IMetaObject metaObject, final String sql, final String aQueryParameters,
                                            final String columnMappingJson, final IContext context)
        throws SQLException, DatabaseConnectorException, MxDataTypeNotSupported {

        ConnectionDetails connectionDetails = JsonUtil.fromJson(connectionDetailsJsonString, ConnectionDetails.class);

        if (logNode.isTraceEnabled()) logNode.trace(String.format("executeQuery: %s, %s, %s",
                connectionDetails.getConnectionString(), connectionDetails.getUserName(), sql));

        List<QueryParameter> queryParameters = JsonUtil.getQueryParameters(aQueryParameters);
        try (Connection connection = connectionManager.getConnection(connectionDetails);
             PreparedStatement preparedStatement = preparedStatementCreator.create(sql, queryParameters, connection);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            ColumnMapping columnAttributeMapping = JsonUtil.GetColumnAttributeMapping(columnMappingJson);
            ResultSetReader resultSetReader = new ResultSetReader(resultSet, metaObject, columnAttributeMapping.getColumnAttributeMapping());

            MendixObjectService mendixObjectService = new MendixObjectService(logNode, objectInstantiator);
            return mendixObjectService.createMendixObjects(context, metaObject, resultSetReader.readAll(), null, null);
        }
    }

    public long executeStatement(final String aConnectionDetailsJsonString, final String aSql, final String aQueryParameters)
        throws SQLException, MxDataTypeNotSupported {

        ConnectionDetails connectionDetails = JsonUtil.fromJson(aConnectionDetailsJsonString, ConnectionDetails.class);

        if (logNode.isTraceEnabled())
            logNode.trace(String.format("executeStatement: %s, %s, %s",
                    connectionDetails.getConnectionString(), connectionDetails.getUserName(), aSql));

        List<QueryParameter> queryParameters = JsonUtil.getQueryParameters(aQueryParameters);

        try (Connection connection = connectionManager.getConnection(connectionDetails);
             PreparedStatement preparedStatement = preparedStatementCreator.create(aSql, queryParameters, connection)) {
            return preparedStatement.executeUpdate();
        }
    }

    public List<IMendixObject> executeCallableQuery(String connectionDetailsJsonString, IMetaObject metaObject, String sql, String queryParameters, String columnMappingJson, IContext context)
        throws SQLException, MxException {

        ConnectionDetails connectionDetails = JsonUtil.fromJson(connectionDetailsJsonString, ConnectionDetails.class);

        if (logNode.isTraceEnabled()) logNode.trace(String.format("executeCallableQuery: %s, %s, %s",
                connectionDetails.getConnectionString(), connectionDetails.getUserName(), sql));

        List<QueryParameter> queryParameterList = JsonUtil.getQueryParameters(queryParameters);

        try (Connection connection = connectionManager.getConnection(connectionDetails);
             StatementWrapper callableStatement = callableStatementCreator.create(sql, queryParameterList, connection);
             ResultSet resultSet = callableStatement.execute();) {

            ColumnMapping columnAttributeMapping = JsonUtil.GetColumnAttributeMapping(columnMappingJson);
            ResultSetReader resultSetReader = new ResultSetReader(resultSet, metaObject, columnAttributeMapping.getColumnAttributeMapping());

            MendixObjectService mendixObjectService = new MendixObjectService(logNode, objectInstantiator);
            return mendixObjectService.createMendixObjects(context, metaObject, resultSetReader.readAll(), null, null);
        }
    }

    public long executeCallableStatement(String connectionDetailsJsonString, String sql, String queryParameters) throws SQLException, MxException {

        ConnectionDetails connectionDetails = JsonUtil.fromJson(connectionDetailsJsonString, ConnectionDetails.class);

        if (logNode.isTraceEnabled())
            logNode.trace(String.format("executeCallableStatement: %s, %s, %s",
                    connectionDetails.getConnectionString(), connectionDetails.getUserName(), sql));

        List<QueryParameter> queryParameterList = JsonUtil.getQueryParameters(queryParameters);

        try (Connection connection = connectionManager.getConnection(connectionDetails);
             StatementWrapper callableStatement = callableStatementCreator.create(sql, queryParameterList, connection)) {
            return callableStatement.executeUpdate();
        }
    }

    public IMendixObject executeCallable(String connectionDetailsJsonString, IMetaObject metaObject, String sql, String queryParameters, String columnMapping, IContext context)
        throws SQLException, MxException {
        ConnectionDetails connectionDetails = JsonUtil.fromJson(connectionDetailsJsonString, ConnectionDetails.class);

        if (logNode.isTraceEnabled()) logNode.trace(String.format("executeCallable: %s, %s, %s",
                connectionDetails.getConnectionString(), connectionDetails.getUserName(), sql));

        List<QueryParameter> queryParameterList = JsonUtil.getQueryParameters(queryParameters);

        try (Connection connection = connectionManager.getConnection(connectionDetails);
             StatementWrapper callableStatement = callableStatementCreator.create(sql, queryParameterList, connection);
             CallableStatement callableStatementResult = callableStatement.executeCallable()) {
            CallableStatementService callableStatementService = new CallableStatementService(logNode, objectInstantiator);
            return callableStatementService.createCallableEntityObject(context, metaObject, callableStatementResult, queryParameters, columnMapping);
        }
    }
}