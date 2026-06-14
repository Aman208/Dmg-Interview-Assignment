package com.example.notification.entity;

import com.example.notification.enums.ChannelType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Per-tenant, per-channel rate limit configuration, expressed as a token
 * bucket: a maximum number of notifications per minute. If no row exists
 * for a given (tenant, channel) pair, the system default
 * (notification.rate-limit.default-capacity-per-minute) applies.
 *
 * A null channel row represents a tenant-wide override across all channels
 * (takes precedence only if no channel-specific row exists).
 */
@Entity
@Table(name = "rate_limit_configs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_rate_limit_tenant_channel", columnNames = {"tenant_id", "channel"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateLimitConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /**
     * Null = applies to all channels for this tenant.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ChannelType channel;

    @Column(name = "capacity_per_minute", nullable = false)
    private Integer capacityPerMinute;
}
