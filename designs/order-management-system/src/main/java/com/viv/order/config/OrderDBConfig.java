package com.viv.order.config;

import java.util.TimeZone;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.viv.order.entity.Order;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.viv.order.repository",
        entityManagerFactoryRef = "orderEntityManagerFactory",
        transactionManagerRef = "orderTransactionManager"
)
public class OrderDBConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.order")
    public DataSource orderDataSource() {
        // pgjdbc forwards the JVM default timezone during startup, so normalize it here.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "orderEntityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean orderEntityManagerFactory(
        EntityManagerFactoryBuilder builder,
        @Qualifier("orderDataSource")
        DataSource orderDataSource
    ) {
        return builder
                .dataSource(orderDataSource)
                .packages(Order.class)
                .persistenceUnit("order")
                .build();
    }

    @Bean
    @Primary
    public PlatformTransactionManager orderTransactionManager(
        @Qualifier("orderEntityManagerFactory")
        EntityManagerFactory orderEntityManagerFactory
    ) {
        return new JpaTransactionManager(orderEntityManagerFactory);
    }
}
