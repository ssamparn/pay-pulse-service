package com.paypulse.platform.common.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class FlywayMigrationStartupConfig {

    @Bean
    @ConditionalOnMissingBean(Flyway.class)
    Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .defaultSchema("public")
                .schemas("public")
                .createSchemas(true)
                .load();
    }

    @Bean
    ApplicationRunner runFlywayMigrationOnStartup(Flyway flyway) {
        return ignored -> {
            int pendingMigrations = flyway.info().pending().length;
            if (pendingMigrations > 0) {
                log.info("Applying {} pending Flyway migrations", pendingMigrations);
            }
            flyway.migrate();
            log.info("Flyway migration check completed");
        };
    }
}

