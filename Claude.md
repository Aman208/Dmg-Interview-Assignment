
## Step 1

- Initialize the project with maven as dependency and Use Java 17.
- Add the following Core entities in the project: 
  - **Tenant** – id, name, status, global rate limit override
  - **User** – id, tenant_id (nullable for platform admin), email, password, role (PLATFORM_ADMIN / TENANT_ADMIN)
  - **Channel** – enum: EMAIL, SMS, PUSH, IN_APP (+ per-tenant config table for sender id, API keys placeholder)
  - **Template** – id, tenant_id, name, channel, subject/body with {{variable}} placeholders, version
  - **Notification** (logical request) – id, tenant_id, template_id, recipient, payload (variables), channel, status, scheduled_at, idempotency_key
  -  **NotificationAttempt** – id, notification_id, attempt_number, status, error, timestamps (audit trail)
  - **RateLimitConfig** – per-tenant, per-channel limits (requests/sec or per-minute)