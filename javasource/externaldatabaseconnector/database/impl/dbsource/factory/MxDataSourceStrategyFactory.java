package externaldatabaseconnector.database.impl.dbsource.factory;

import com.zaxxer.hikari.HikariDataSource;
import externaldatabaseconnector.database.constants.IMxDatabaseTypes;
import externaldatabaseconnector.database.impl.dbsource.helper.MxDataSourceContext;
import externaldatabaseconnector.database.impl.dbsource.strategy.MxBaseDataSourceStrategy;
import externaldatabaseconnector.database.impl.dbsource.strategy.MxDefaultDataSourceStrategy;
import externaldatabaseconnector.database.impl.dbsource.strategy.MxPostgresDataSourceStrategy;
import externaldatabaseconnector.database.impl.dbsource.strategy.MxSnowflakeDataSourceStrategy;
import externaldatabaseconnector.pojo.ConnectionDetails;

public class MxDataSourceStrategyFactory {
  public static HikariDataSource createHikariDataSource(ConnectionDetails connectionDetails, Integer connPoolKey) throws Exception {

    HikariDataSource hikariDataSource = new HikariDataSource();
    hikariDataSource.setPoolName(String.format("MxDbConnector-HikaryCP-%d", connPoolKey));
    hikariDataSource.setMinimumIdle(0);

    MxBaseDataSourceStrategy dataSourceStrategy;

    switch (connectionDetails.getDatabaseType()) {
      case IMxDatabaseTypes.POSTGRESQL:
        dataSourceStrategy = new MxPostgresDataSourceStrategy(hikariDataSource, connectionDetails);
        break;
      case IMxDatabaseTypes.SNOWFLAKE:
        dataSourceStrategy = new MxSnowflakeDataSourceStrategy(hikariDataSource, connectionDetails);
        break;
      default:
        dataSourceStrategy = new MxDefaultDataSourceStrategy(hikariDataSource, connectionDetails);
        break;
    }

    MxDataSourceContext dataSourceContext = new MxDataSourceContext(dataSourceStrategy);
    return dataSourceContext.createDataSource();
  }
}
