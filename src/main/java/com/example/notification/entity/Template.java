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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A tenant-defined message template for a specific channel. Bodies/subjects
 * support variable substitution using the {{variableName}} syntax.
 *
 * Versioning: each update increments the version number rather than mutating
 * history, so previously sent notifications can be traced back to the exact
 * template content used (referenced via templateId + version snapshot stored
 * on the Notification at send time would be a further enhancement; for this
 * demo we keep a single mutable row plus a version counter for visibility).
 */
@Entity
@Table(name = "templates", uniqueConstraints = {
        @UniqueConstraint(name = "uk_template_tenant_name_channel", columnNames = {"tenant_id", "name", "channel"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Template extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChannelType channel;

    /**
     * Optional subject line. Relevant for EMAIL; ignored for SMS/PUSH/IN_APP.
     * Supports {{variable}} placeholders.
     */
    @Column(length = 255)
    private String subject;

    /**
     * Body content with {{variable}} placeholders.
     */
    @Lob
    @Column(nullable = false)
    private String body;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;
}
