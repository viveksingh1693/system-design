package com.viv.order.entity;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "orders")
@Cacheable
@Cache(
    usage = CacheConcurrencyStrategy.READ_WRITE
)
@Schema(description = "Order entity stored in PostgreSQL and cached by Hibernate second-level cache.")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Primary key", example = "10")
    private Long id;

    @Schema(description = "User identifier from the MySQL datasource", example = "1")
    private Long userId;
    @Schema(description = "Order total amount", example = "249.99")
    private Double amount;

}
