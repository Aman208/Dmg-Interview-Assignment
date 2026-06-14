package com.example.notification.enums;

/**
 * Outcome of a single delivery attempt, recorded in the audit trail.
 */
public enum AttemptStatus {
    SUCCESS,
    TRANSIENT_FAILURE,
    PERMANENT_FAILURE
}
