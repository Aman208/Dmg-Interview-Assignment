package com.example.notification.enums;

/**
 * Roles supported by the platform.
 * PLATFORM_ADMIN  - manages tenants and global limits, not tied to a specific tenant
 * TENANT_ADMIN    - manages templates, channel configuration, and views delivery reports
 *                    for their own tenant
 */
public enum UserRole {
    PLATFORM_ADMIN,
    TENANT_ADMIN
}
