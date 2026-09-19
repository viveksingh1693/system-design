package com.viv.business.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.viv.business.enums.BusinessStatus;

@Entity
@Table(name = "business", indexes = {
        @Index(name = "idx_business_category", columnList = "category_id")
// ,
// @Index(name = "idx_business_active_category", columnList = "category_id",
// // Partial indexes are database-specific, so we don't define
// // the WHERE status = 'ACTIVE' clause here.
// unique = false)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_business_category"))
    private BusinessCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BusinessStatus status;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BusinessLocation> locations = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {

        Instant now = Instant.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = BusinessStatus.ACTIVE;
        }

        if (version == null) {
            version = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void addLocation(BusinessLocation location) {

        locations.add(location);
        location.setBusiness(this);
    }

    public void removeLocation(BusinessLocation location) {

        locations.remove(location);
        location.setBusiness(null);
    }
}