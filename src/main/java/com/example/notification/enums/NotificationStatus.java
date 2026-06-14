package com.example.notification.enums;

/**
 * State machine for a notification's lifecycle.
 *
 * PENDING          -> created, waiting to be picked up (immediate) or waiting for scheduled_at (scheduled)
 * PROCESSING       -> picked up by a dispatch worker, send in progress
 * SENT             -> terminal success state
 * RETRYING         -> a transient failure occurred, waiting for next backoff attempt
 * FAILED_PERMANENT -> terminal failure state after exhausting max retries
 * CANCELLED        -> terminal state if a scheduled notification was cancelled before dispatch
 */
public enum NotificationStatus {
    PENDING,
    PROCESSING,
    SENT,
    RETRYING,
    FAILED_PERMANENT,
    CANCELLED
}
