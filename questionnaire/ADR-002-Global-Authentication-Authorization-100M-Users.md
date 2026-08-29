# ADR-002: Global Authentication and Authorization Architecture for 100M+ Users

- **Status:** Proposed / Accepted for reference architecture
- **Date:** 2026-08-23
- **Decision Owners:** Solution Architecture / Security / Platform Engineering
- **Scope:** Global customer authentication, API authorization, workload identity, and low-latency user validation
- **Target Scale:** 100M+ registered users, multi-region APIs

---

## 1. Context

We need to implement authentication and authorization for a globally distributed platform serving more than **100 million registered users**.

The platform may run on AWS, Azure, GCP, or in a multi-cloud environment. APIs are deployed across multiple geographic regions and must provide low latency while maintaining strong security controls.

A naive architecture in which every API request calls a central identity service or user database would create:

- High network latency
- A global availability dependency
- Identity-provider bottlenecks
- Excessive database traffic
- Poor regional resilience
- Higher operational cost

The architecture must therefore separate **login/authentication** from **per-request token validation** and separate **coarse-grained authorization** from **domain/resource-level authorization**.

---

## 2. Decision Drivers

The architecture must support:

- 100M+ registered users
- Global traffic distribution
- Low API latency
- Multi-region deployment
- OAuth 2.0 and OpenID Connect
- Short-lived credentials
- MFA/passkeys/federated login where required
- Fine-grained authorization
- Tenant-aware access control
- Service-to-service authentication
- Token revocation strategy
- Regional resilience
- Key rotation
- Auditability
- Zero-trust principles
- Cloud portability where practical

---

## 3. Core Decision

We will use **OAuth 2.0 + OpenID Connect (OIDC)** as the standard customer authentication model.

After authentication, the identity provider will issue **short-lived signed access tokens**, normally JWTs.

API requests will be authenticated at the nearest regional API gateway by validating the token locally using cached public signing keys rather than synchronously calling the identity provider or user database for every request.

Authorization will be layered:

1. **Gateway-level coarse authorization** — token validity, scopes, roles, tenant and audience.
2. **Domain-level authorization** — resource ownership and business rules.
3. **Dedicated policy/relationship authorization** — introduced when RBAC/ABAC/ReBAC complexity requires centralized policy evaluation.

Service-to-service authentication will use **workload identity and/or mTLS with short-lived service credentials**, independently of end-user authentication.

---

## 4. Authentication vs Authorization

### Authentication

Answers:

> **Who is making the request?**

Typical validation includes:

- Token signature
- Issuer (`iss`)
- Audience (`aud`)
- Expiration (`exp`)
- Not-before (`nbf`) where used
- Token type

### Authorization

Answers:

> **What is this identity allowed to do?**

Authorization may depend on:

- OAuth scopes
- Roles
- Tenant
- Resource ownership
- User/resource attributes
- Business state
- Relationships between identities and resources

---

## 5. High-Level Architecture

```text
                              GLOBAL USERS
                                   │
                                   ↓
                         Global DNS / Anycast
                                   │
                                   ↓
                          CDN / Edge / WAF
                                   │
                 ┌─────────────────┼─────────────────┐
                 ↓                 ↓                 ↓
              Americas           Europe             APAC
                 │                 │                 │
           API Gateway       API Gateway       API Gateway
                 │                 │                 │
           Local JWT         Local JWT         Local JWT
           Validation        Validation        Validation
                 │                 │                 │
                 └─────────────────┼─────────────────┘
                                   │
                            Authorization
                                   │
                         ┌─────────┴─────────┐
                         ↓                   ↓
                  Scope / Role         Domain Policy
                    Checks                Checks
                         │                   │
                         └─────────┬─────────┘
                                   ↓
                         Regional Services
                                   │
                                   ↓
                          Regional Data Stores
```

---

## 6. Login Flow

Login is relatively infrequent compared with API traffic and may involve a centralized or globally distributed identity platform.

```text
User
 │
 ↓
Identity Provider
 │
 ├── Password / Passkey
 ├── MFA
 ├── Social Login
 └── Enterprise Federation
 │
 ↓
OAuth 2.0 / OIDC
 │
 ├── ID Token
 ├── Short-lived Access Token
 └── Refresh Token (where appropriate)
```

The login path may perform more expensive identity checks because it is not executed for every business API call.

---

## 7. API Request Authentication

After login:

```text
Client
   │
   │ Authorization: Bearer <access-token>
   ↓
Regional API Gateway
   │
   ├── Parse token
   ├── Validate signature
   ├── Validate issuer
   ├── Validate audience
   ├── Validate expiration
   └── Validate required scopes
   │
   ↓
Backend Service
```

The gateway uses cached signing keys from the identity provider's **JWKS** endpoint.

Therefore, normal API authentication does **not** require:

```text
API → Identity Provider → User Database
```

for every request.

---

## 8. JWT Design

A minimal access-token claim set may include:

```json
{
  "iss": "https://identity.example.com",
  "sub": "user-123456",
  "aud": "order-api",
  "exp": 1787412345,
  "iat": 1787412045,
  "scope": "orders:read orders:create",
  "roles": ["customer"],
  "tenant": "tenant-123"
}
```

Tokens should remain reasonably small.

Do **not** embed large, dynamic authorization datasets such as:

- All accessible orders
- Hundreds/thousands of permissions
- Large organization hierarchies
- Frequently changing resource relationships
- Sensitive user-profile information

Large or highly dynamic authorization state belongs in the authorization/domain layer.

---

## 9. Authorization Architecture

### Layer 1 — Gateway Authorization

The API gateway performs inexpensive coarse-grained checks.

Example:

```text
POST /orders

Required:
  audience = order-api
  scope    = orders:create
```

Possible claims:

- Scope
- Role
- Tenant
- Audience

### Layer 2 — Domain Authorization

Resource-level decisions remain with the owning service.

Example:

```text
User-123
   │
   ↓
GET /orders/987
   │
   ↓
Order Service
   │
   ├── Does Order 987 belong to User-123?
   │
   ├── YES → Allow
   └── NO  → Deny
```

A JWT indicating that a user has the `customer` role does not prove that the user owns a specific order.

### Layer 3 — Centralized Fine-Grained Authorization

When authorization becomes complex, introduce a dedicated policy/relationship authorization layer.

```text
API / Service
      │
      ↓
Authorization Service / Policy Engine
      │
      ├── RBAC
      ├── ABAC
      └── ReBAC
      │
      ↓
Policy / Relationship Store
```

---

## 10. Authorization Models

### RBAC — Role-Based Access Control

```text
User → Role → Permission
```

Examples:

- Customer
- Support Agent
- Operations Admin
- Finance Admin

Suitable when permissions map cleanly to organizational roles.

### ABAC — Attribute-Based Access Control

Example:

```text
user.region == resource.region
AND
user.department == resource.department
```

Useful when decisions depend on identity, resource, environment or business attributes.

### ReBAC — Relationship-Based Access Control

Example:

```text
User
 ↓ member-of
Organization
 ↓ owns
Project
 ↓ contains
Order
```

Useful for complex sharing, hierarchy and resource-relationship models.

---

## 11. Cloud Provider Options

### AWS

Potential architecture:

```text
Amazon Cognito
      │
      ↓
OAuth 2.0 / OIDC
      │
      ↓
JWT
      │
      ↓
Amazon API Gateway
      │
      ↓
EKS / Services
```

Typical capabilities/options:

| Requirement | AWS Option |
|---|---|
| Customer identity | Amazon Cognito User Pools |
| OAuth/OIDC | Cognito |
| API token validation | API Gateway JWT/Cognito authorizers |
| Custom gateway authorization | Lambda Authorizer |
| AWS resource authorization | IAM |
| Workload identity | IAM roles / EKS workload identity patterns |
| Secrets | AWS Secrets Manager |
| Edge/WAF | CloudFront / AWS WAF |

### Azure

Potential architecture:

```text
Microsoft Entra External ID
        │
        ↓
OAuth 2.0 / OIDC
        │
        ↓
JWT
        │
        ↓
Azure API Management
        │
        ↓
AKS / Services
```

Typical capabilities/options:

| Requirement | Azure Option |
|---|---|
| Customer identity | Microsoft Entra External ID |
| Workforce identity | Microsoft Entra ID |
| API gateway | Azure API Management |
| Workload identity | Managed Identity / Workload Identity |
| Secrets | Azure Key Vault |
| Edge/WAF | Azure Front Door / WAF |

### GCP

Potential architecture:

```text
Google Cloud Identity Platform
          │
          ↓
OAuth 2.0 / OIDC
          │
          ↓
JWT
          │
          ↓
API Gateway
          │
          ↓
GKE / Services
```

Typical capabilities/options:

| Requirement | GCP Option |
|---|---|
| Customer identity | Identity Platform |
| Workforce/cloud identity | Cloud Identity / IAM |
| API gateway | API Gateway / Apigee depending on requirements |
| Workload identity | Workload Identity / Service Accounts |
| Secrets | Secret Manager |
| Edge/WAF | Cloud Load Balancing / Cloud Armor |

---

## 12. Multi-Cloud Strategy

The application should depend primarily on standards at the identity protocol boundary:

```text
OAuth 2.0
   +
OpenID Connect
   +
JWT / standard token formats
```

This reduces direct coupling between application business logic and a specific CIAM implementation.

Provider-specific integrations may still be used for:

- MFA
- Passkeys
- Fraud/risk controls
- Federation
- Lifecycle management
- Audit integration
- Operational tooling

A multi-cloud deployment does **not** require multiple independent identity sources unless there is a clear business or regulatory reason. Multiple authorities can create identity synchronization and account-consistency problems.

---

## 13. Global Latency Strategy

For 100M users, the critical design principle is:

> **Authenticate centrally at login; validate locally on API requests.**

The API path should avoid synchronous cross-region identity lookups.

```text
User
 ↓
Nearest Edge
 ↓
Regional API Gateway
 ↓
Local JWT Signature Validation
 ↓
Local Authorization
 ↓
Regional Service
 ↓
Regional Cache / Database
```

### Illustrative Latency Budget

These numbers are architecture targets and must be measured under actual network and workload conditions.

| Component | Example Target |
|---|---:|
| Edge/WAF processing | 10–20 ms |
| Gateway routing | ~5 ms |
| Local JWT validation | Low single-digit ms target |
| Service processing | 10–20 ms |
| Cache lookup | 2–5 ms |
| Regional database | 5–20 ms |
| Internal network | 5–10 ms |

The objective is to reserve most of the latency budget for business processing rather than remote identity validation.

---

## 14. JWKS and Signing-Key Strategy

Regional gateways/resource servers cache public signing keys.

```text
Identity Provider
       │
       │ JWKS
       ↓
Regional Gateway
       │
       ├── Cached Key A
       └── Cached Key B
       │
       ↓
Local JWT Validation
```

Requirements include:

- Controlled signing-key rotation
- Multiple active keys during rotation
- JWKS caching
- Safe cache refresh
- Unknown `kid` handling
- Monitoring of key-fetch failures
- No dependence on per-request JWKS retrieval

---

## 15. Token Lifetime and Revocation

Stateless JWT validation introduces a trade-off: a token may remain valid until expiration even if the account is disabled after issuance.

We will mitigate this using layered controls.

### Short-Lived Access Tokens

Example policy:

```text
Access Token  → approximately 5–15 minutes, risk-dependent
Refresh Token → longer-lived and protected
```

Exact lifetimes must be determined by security and product requirements.

### Refresh-Token Controls

Refresh tokens may support:

- Rotation
- Revocation
- Device/session tracking
- Risk-based reauthentication

### High-Risk Revocation

For sensitive scenarios, options include:

- Revocation/version cache
- Session invalidation
- Token introspection for selected operations
- Forced reauthentication

Introspection should **not** automatically be added to every high-volume API call because that would reintroduce a central runtime dependency.

---

## 16. User Validation Strategy

Three different forms of validation must not be confused.

### Identity Validation

> Is this token valid and who does it represent?

Handled using JWT/OIDC validation.

### Account-State Validation

> Is the account currently disabled, locked, compromised or restricted?

Handled through short token lifetime plus selective dynamic checks for high-risk actions.

### Domain Validation

> Can this user perform this operation on this resource?

Handled by the owning domain service or authorization system.

Example:

```text
JWT says:
sub = user-123
role = customer

Order Service determines:
Order-987.owner == user-123 ?
```

---

## 17. Caching Authorization Data

For authorization data that changes less frequently, regional caching can reduce latency.

Possible cache keys:

```text
tenant:user:roles
user:policy-version
resource:authorization-metadata
```

Requirements:

- Short/appropriate TTL
- Explicit invalidation for critical changes
- Versioned policies where useful
- Fail-open vs fail-closed decision documented per operation

Security-sensitive authorization should generally fail closed unless business requirements explicitly justify otherwise.

---

## 18. Service-to-Service Authentication

User authentication and workload authentication are separate concerns.

```text
User
 │ User JWT
 ↓
API Gateway
 │
 ↓
Order Service
 │
 │ Workload Identity / mTLS
 ↓
Inventory Service
```

Inventory Service may need both:

1. **Caller identity:** Order Service
2. **End-user context:** User-123

These identities should not be conflated.

Cloud-native options include:

- AWS IAM/workload identity patterns
- Azure Managed Identity / Workload Identity
- GCP Workload Identity / Service Accounts
- mTLS/service mesh where appropriate

Service credentials should be short-lived whenever possible.

---

## 19. Security Controls

The architecture should include:

- TLS everywhere
- mTLS where service trust requirements justify it
- WAF
- DDoS protection
- Rate limiting
- Bot/abuse protection
- OAuth scopes
- Least privilege
- Secrets management
- Key rotation
- MFA/passkeys where appropriate
- Audit logging
- Security event monitoring
- Credential-stuffing protections
- Risk-based authentication where available

---

## 20. Rate Limiting and Abuse Protection

Authentication does not replace abuse protection.

Rate limits can be applied at multiple levels:

```text
IP
 ↓
Device
 ↓
User
 ↓
Tenant
 ↓
API / Operation
```

Example:

```text
Login API        → Strict brute-force protection
Order creation   → User/business rate limits
Read APIs        → Higher thresholds
Admin APIs       → Stronger authentication + restrictive policies
```

---

## 21. Failure Scenarios

### Identity Provider Unavailable

Existing unexpired access tokens should continue to be locally verifiable while cached signing keys remain valid.

New login/token-refresh operations may be affected.

This isolates many business APIs from an identity-provider outage.

### JWKS Endpoint Unavailable

Use cached keys until refresh is required according to the defined security policy.

Monitor and alert on refresh failures.

### Authorization Service Unavailable

Behavior depends on operation sensitivity.

For sensitive operations:

```text
Authorization unavailable
        ↓
Fail closed
```

Low-risk read scenarios may use carefully bounded cached decisions if explicitly approved.

### Region Failure

```text
Region A X
    │
    ↓
Global Traffic Manager
    │
    ↓
Region B
```

The alternate region must be able to validate the same trusted token issuer/signing keys and access required regional/global authorization state.

---

## 22. Observability

Monitor:

### Authentication

- Login success/failure
- MFA success/failure
- Token issuance latency
- Refresh failures
- Suspicious login patterns

### Gateway

- JWT validation failures
- Expired tokens
- Invalid issuer/audience
- Unknown signing-key IDs
- Authorization denials
- Rate-limit events

### Authorization

- Policy evaluation latency
- Allow/deny counts
- Cache hit ratio
- Policy-store latency
- Authorization-service availability

### End-to-End

- API p50/p95/p99
- Regional latency
- Authentication contribution to request latency
- Error rate
- Cross-region calls

---

## 23. Alternatives Considered

### Option A — Central Auth Service Call on Every API Request

**Rejected.**

Problems:

- Adds network latency
- Creates global bottleneck
- Creates central availability dependency
- Scales poorly for very high request volumes

### Option B — User Database Lookup on Every Request

**Rejected.**

Problems:

- Database becomes part of every request path
- High read load
- Cross-region latency
- Poor fault isolation

### Option C — Long-Lived JWTs Only

**Rejected as the default.**

Benefit:

- Very cheap validation

Problems:

- Slow account revocation
- Stale authorization claims
- Increased risk if tokens are compromised

### Option D — Opaque Tokens + Introspection for Every Request

**Not selected for the general high-volume API path.**

Benefit:

- Centralized real-time token-state control

Cost:

- Additional network call
- Availability dependency
- Higher latency and infrastructure load

May still be appropriate for selected high-risk use cases.

### Option E — Short-Lived Signed Tokens + Local Validation

**Selected.**

Provides:

- Low-latency validation
- Regional resilience
- Horizontal scalability
- Reduced central dependencies
- Standards-based interoperability

---

## 24. Consequences

### Positive

- Authentication validation scales horizontally.
- Normal API calls do not require user-database access.
- Regional gateways can validate users locally.
- Identity-provider outages have reduced impact on already authenticated sessions.
- OAuth/OIDC improves interoperability.
- Authorization responsibilities are clearly separated.
- Service identities are separated from user identities.

### Negative

- Revocation is not instantaneous for stateless access tokens.
- Key rotation must be carefully managed.
- Authorization can become distributed and complex.
- Claims can become stale.
- Multi-region policy/data replication needs careful design.
- Fine-grained authorization may require an additional policy platform.

---

## 25. Risks and Mitigations

| Risk | Mitigation |
|---|---|
| Stolen access token | Short lifetime, TLS, secure client storage, risk controls |
| Disabled user retains temporary access | Short-lived access token + selective revocation/dynamic checks |
| Stale roles/permissions | Short token lifetime, policy versioning, domain checks |
| Large JWT | Keep claims minimal |
| Signing-key rotation failure | Overlapping keys + JWKS caching + monitoring |
| Authorization-service latency | Regional deployment + caching |
| Central IdP outage | Local JWT validation for existing sessions |
| Credential stuffing | MFA/passkeys, throttling, bot/risk detection |
| Cross-region latency | Edge routing + regional gateways/services/data |
| Service credential leakage | Workload identity + short-lived credentials |

---

## 26. Success Criteria

The architecture is successful when:

- 100M+ registered identities can be supported.
- API authentication does not require a central DB/IdP call per request.
- Authentication validation adds only a small fraction of the total API latency budget.
- APIs remain regionally resilient.
- Authorization correctly protects resource ownership and business operations.
- Account disable/revocation meets defined security SLAs.
- Signing-key rotation occurs without service interruption.
- Service-to-service access follows least privilege.
- Authentication and authorization are observable and auditable.

---

## 27. Key Architecture Decisions

| Decision | Rationale |
|---|---|
| OAuth 2.0 + OIDC | Standards-based identity federation/authentication |
| Short-lived access tokens | Balance scalability and revocation risk |
| Local JWT validation | Avoid central call per API request |
| Cached JWKS | Low-latency signature verification |
| Regional API gateways | Reduce global API latency |
| Gateway scope/role checks | Cheap coarse-grained authorization |
| Domain authorization | Resource/business rules belong to domain owner |
| Policy engine when needed | Handle complex RBAC/ABAC/ReBAC |
| Workload identity | Separate service identity from user identity |
| Regional data access | Avoid cross-region request-path latency |
| Selective introspection | Preserve real-time checks for high-risk operations without penalizing all APIs |

---

## 28. Architecture Principle

> **For 100 million users, authentication is scaled by making login relatively infrequent and making per-request validation cheap, local and cryptographic—not by scaling a central identity database to participate in every API request.**

The request path should therefore follow:

```text
Authenticate at login
        ↓
Issue short-lived token
        ↓
Validate locally at regional edge/gateway
        ↓
Perform coarse authorization
        ↓
Perform domain/resource authorization
        ↓
Access regional business data
```

---

## 29. Interview Summary

> “For a platform serving 100 million global users, I would standardize on OAuth 2.0 and OpenID Connect using a CIAM provider such as Amazon Cognito, Microsoft Entra External ID, or Google Cloud Identity Platform depending on the cloud ecosystem and requirements. After login, clients receive short-lived signed access tokens.
>
> The nearest regional API gateway validates the JWT locally using cached public signing keys and checks issuer, audience, expiry and scopes. This keeps the high-volume API path independent of the central identity provider and user database.
>
> I would perform coarse-grained authorization at the gateway and resource-level authorization in the domain service. If permissions become sufficiently complex, I would add a regionalized policy engine supporting RBAC, ABAC or relationship-based authorization.
>
> Service-to-service authentication would use workload identity and potentially mTLS rather than relying solely on the end-user token. For latency, I would combine edge routing, regional gateways, local token validation, regional services, caches and regional data stores. The key principle is to authenticate centrally at login but validate locally on every API request.”
