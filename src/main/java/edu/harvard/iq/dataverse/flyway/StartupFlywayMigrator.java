package edu.harvard.iq.dataverse.flyway;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
//@TransactionManagement(value = TransactionManagementType.BEAN)
public class StartupFlywayMigrator {

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    void migrateDatabase() {


        if (dataSource == null){
            throw new NullPointerException("Failed to migrate, cannot connect to database");
        }

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .load();

        flyway.migrate();
    }
}
