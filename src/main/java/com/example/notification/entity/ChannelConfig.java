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
 * Per-tenant, per-channel configuration (e.g. sender id/from address,
 * provider name placeholder, enabled flag). Real provider credentials are
 * out of scope for this demo - this entity exists primarily to demonstrate
 * tenant-level channel management and to gate whether a tenant can use a
 * given channel at all.
 */
@Entity
@Table(name = "channel_configs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_channel_config_tenant_channel", columnNames = {"tenant_id", "channel"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChannelType channel;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /**
     * Display name for the sender, e.g. "from" address for email or
     * sender id for SMS. Free-form, simulated for this demo.
     */
    @Column(name = "sender_identifier", length = 255)
    private String senderIdentifier;
}
