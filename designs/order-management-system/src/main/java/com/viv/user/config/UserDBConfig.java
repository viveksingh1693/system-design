package com.viv.user.config;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.viv.user.entity.User;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.viv.user.repository",
        entityManagerFactoryRef = "userEntityManagerFactory",
        transactionManagerRef = "userTransactionManager"
)
public class UserDBConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.user")
    public DataSource userDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "userEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean userEntityManagerFactory(
        EntityManagerFactoryBuilder builder,
        @Qualifier("userDataSource")
        DataSource userDataSource
    ) {
        return builder
                .dataSource(userDataSource)
                .packages(User.class)
                .persistenceUnit("user")
                .build();
    }

    @Bean
    public PlatformTransactionManager userTransactionManager(
        @Qualifier("userEntityManagerFactory")
        EntityManagerFactory userEntityManagerFactory
    ) {
        return new JpaTransactionManager(userEntityManagerFactory);
    }
}
