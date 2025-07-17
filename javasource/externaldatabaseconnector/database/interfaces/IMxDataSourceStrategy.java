package externaldatabaseconnector.database.interfaces;

import com.zaxxer.hikari.HikariDataSource;

public interface IMxDataSourceStrategy {
  HikariDataSource configureDataSource() throws Exception;
}
