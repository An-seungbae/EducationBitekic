package externaldatabaseconnector.database.impl.dbsource.strategy;

import com.zaxxer.hikari.HikariDataSource;
import externaldatabaseconnector.pojo.ConnectionDetails;

public class MxDefaultDataSourceStrategy extends MxBaseDataSourceStrategy {
  public MxDefaultDataSourceStrategy(HikariDataSource hikariDataSource, ConnectionDetails connectionDetails) {
    super(hikariDataSource, connectionDetails);
  }

  @Override
  public HikariDataSource configureDataSource() {
    this.setBasicConnectionProperties();
    return dataSource;
  }
}
