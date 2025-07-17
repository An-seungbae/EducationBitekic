package externaldatabaseconnector.database.impl.dbsource.strategy;

import com.zaxxer.hikari.HikariDataSource;
import externaldatabaseconnector.database.interfaces.IMxDataSourceStrategy;
import externaldatabaseconnector.pojo.ConnectionDetails;

public abstract class MxBaseDataSourceStrategy implements IMxDataSourceStrategy {
  protected HikariDataSource dataSource;
  protected ConnectionDetails connectionDetails;

  public MxBaseDataSourceStrategy(HikariDataSource hikariDataSource, ConnectionDetails connectionDetails){
    this.dataSource = hikariDataSource;
    this.connectionDetails = connectionDetails;
  }

  public abstract HikariDataSource configureDataSource() throws Exception;

  protected void setBasicConnectionProperties(){
    dataSource.setJdbcUrl(connectionDetails.getConnectionString());
    dataSource.setUsername(connectionDetails.getUserName());
    dataSource.setPassword(connectionDetails.getPassword());
  }
}
