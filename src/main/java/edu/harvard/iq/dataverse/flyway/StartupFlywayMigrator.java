package edu.harvard.iq.dataverse.flyway;

import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.sql.DataSource;

@Component
@TransactionManagement(value = TransactionManagementType.BEAN)
public class StartupFlywayMigrator {

    @Resource(lookup = "java:app/jdbc/dataverse")
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
