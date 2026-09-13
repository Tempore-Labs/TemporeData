package org.temporedata.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 轻舟云 temporedata - application entry point.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@ComponentScan(basePackages = "org.temporedata")
@EnableJpaRepositories(basePackages = "org.temporedata")
@EntityScan(basePackages = "org.temporedata")
public class TemporeDataApplication {

    public static void main(String[] args) {
        SpringApplication.run(TemporeDataApplication.class, args);
    }
}