package externaldatabaseconnector.database.impl.dbsource.strategy;

import com.zaxxer.hikari.HikariDataSource;
import externaldatabaseconnector.database.impl.dbsource.helper.MxSnowflakeDataSource;
import externaldatabaseconnector.pojo.ConnectionDetails;
import net.snowflake.client.jdbc.SnowflakeBasicDataSource;

public class MxSnowflakeDataSourceStrategy extends MxBaseDataSourceStrategy {
  public MxSnowflakeDataSourceStrategy(HikariDataSource hikariDataSource, ConnectionDetails connectionDetails){
    super(hikariDataSource, connectionDetails);
  }
  @Override
  public HikariDataSource configureDataSource() throws Exception {
     SnowflakeBasicDataSource snowflakeBasicDataSource = MxSnowflakeDataSource.getSnowflakeDataSource(connectionDetails);
     dataSource.setDataSource(snowflakeBasicDataSource);
     return dataSource;
  }
}
