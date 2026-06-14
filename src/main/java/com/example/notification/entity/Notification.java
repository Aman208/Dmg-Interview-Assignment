package com.example.notification.entity;

import com.example.notification.enums.ChannelType;
import com.example.notification.enums.NotificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A single notification send request submitted by a tenant. This is the
 * unit that the dispatch engine picks up, rate-limits, sends (via a mock
 * channel sender), and tracks through its lifecycle via
 * {@link NotificationStatus}.
 *
 * Idempotency: the combination of (tenant_id, idempotencyKey) is unique when
 * idempotencyKey is provided, preventing duplicate submissions from creating
 * duplicate sends. Duplicate *retries* of the same row are handled by the
 * dispatch engine's attempt counter rather than by creating new rows.
 */
@Entity
@Table(name = "notifications", uniqueConstraints = {
        @UniqueConstraint(name = "uk_notification_tenant_idempotency", columnNames = {"tenant_id", "idempotency_key"})
}, indexes = {
        @Index(name = "idx_notification_status", columnList = "status"),
        @Index(name = "idx_notification_scheduled_at", columnList = "scheduled_at"),
        @Index(name = "idx_notification_tenant_status", columnList = "tenant_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChannelType channel;

    /**
     * Recipient address - email address, phone number, device/push token,
     * or user id (for IN_APP), depending on channel.
     */
    @Column(nullable = false, length = 255)
    private String recipient;

    /**
     * JSON-serialized map of template variable name -> value, used for
     * substitution at send time.
     */
    @Lob
    @Column(name = "template_variables")
    private String templateVariables;

    /**
     * Client-supplied idempotency key, unique per tenant. Used to prevent
     * duplicate notification creation on retried API calls.
     */
    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    /**
     * Null for immediate sends. Set for scheduled sends; the dispatch
     * poller only picks up rows where scheduledAt <= now (or is null).
     */
    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    /**
     * Number of delivery attempts made so far (mirrors the count of
     * NotificationAttempt rows, kept denormalized for fast filtering).
     */
    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    /**
     * Timestamp of the next retry attempt, set by the backoff calculator
     * when status = RETRYING. Null otherwise.
     */
    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    /**
     * Final rendered content actually sent, captured for audit purposes
     * (kept separate from the template so edits to the template after
     * sending don't change history).
     */
    @Lob
    @Column(name = "rendered_content")
    private String renderedContent;

    /**
     * Optimistic locking to guard against the dispatch poller picking up
     * the same row twice across concurrent scheduler runs.
     */
    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long lockVersion = 0L;
}
