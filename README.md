# Campus Lost & Found AI System

> A modern, full-stack campus application that lets students photograph lost or found items, uses Vision AI to identify key characteristics, transparently ranks matches in a local SQLite database via plain JDBC, and protects personal privacy through verified claims workflows.

---

## 1. Product Vision & Principles

* **Photo-First Workflow:** A student takes a photo or uploads an existing photo. Vision AI analyzes category, brand, colors, and distinguishing physical characteristics.
* **Starts Completely Empty:** The application starts with an initialized SQLite schema and **strictly zero** preloaded dummy items, fabricated users, or synthetic transactions. All entries are created only when real users submit them.
* **Photo ≠ Proof of Ownership:** Distinguishing details (e.g., hidden marks, stickers, serial numbers, screen wallpapers) can be marked private by reporters and claimants so that authorized staff can verify true ownership before returning valuable items.
* **Strict Privacy:** Personal email addresses and telephone numbers are masked on public item listings.

---

## 2. Technology Stack

### Backend
* **Java:** Java 21 LTS
* **Framework:** Spring Boot 3.3.5
* **Security & Auth:** Spring Security (session-based authentication with `HttpOnly` cookie, CSRF protection, and CORS configuration).
* **Database Driver:** SQLite JDBC (`org.xerial:sqlite-jdbc:3.47.0.0`)
* **Persistence Layer:** **Plain JDBC** with `PreparedStatement`, `ResultSet`, try-with-resources, and explicit connection management. *(No JPA, Hibernate, or external ORM as strictly required).*
* **Architecture:** Layered Service & DAO pattern (`controller/`, `service/`, `dao/`, `model/`, `dto/`, `config/`, `exception/`).
* **Validation:** Jakarta Bean Validation (`@NotBlank`, `@NotNull`, `@Email`).
* **JSON Serialization:** Jackson `ObjectMapper`.

### Frontend
* **Core:** React 18, TypeScript, Vite 5.
* **Styling:** Custom modern Mint-Green design system (`#10b981`, soft gradients, Plus Jakarta Sans typography, rounded cards, subtle shadows).
* **Authentication Gate:** First-screen email verification with 6-digit OTP, Student/Staff role toggle, and Pending Staff approval status screen.
* **Icons:** `lucide-react`.
* **API Client:** Fully typed TypeScript client with `credentials: 'include'` communicating with the Java REST backend.

### AI Image Understanding
* **Provider:** Google Gemini Vision (`gemini-3.1-flash-lite` / `gemini-1.5-flash`).
* **Architecture:** Abstracted behind `ItemVisionService` interface.
* **Graceful Fallback:** If `GEMINI_API_KEY` is not set or times out, the backend honestly reports manual mode and enables instant editing without blocking the user.

---

## 3. Authentication & Role System

### Security Principles
* **First-Screen Gate:** Unauthenticated users cannot access application dashboards or report items without authenticating their campus email.
* **Passwordless OTP:** Cryptographically secure 6-digit verification codes generated with `SecureRandom`, stored as salted SHA-256 hashes (never plaintext).
* **Strict Session Management:** Server-managed session stored in an `HttpOnly`, `SameSite=Lax` cookie (`JSESSIONID`). No JWTs or sensitive user credentials stored in browser `localStorage`.
* **Principal-Derived Identity:** Endpoints derive user identity strictly from the authenticated session. Browser identity fields are never trusted for reporter or claimant IDs.
* **Role Immutability on Login:** Existing registered users cannot alter their role or bypass approval via browser requests.

### Roles & Access Matrix

| Role | Status | Capabilities |
| :--- | :--- | :--- |
| **Student** | `ACTIVE` | Report lost & found items, search directory, view own items/claims, delete own items, submit claims (cannot claim own items). |
| **Staff** | `PENDING_APPROVAL` | Initial registration state for campus staff. Cannot access staff tools or view sensitive proofs until approved by an administrator. |
| **Staff** | `ACTIVE` | Access Staff Review portal, inspect private proof details, approve/reject claims, inspect system audit logs. |
| **Admin** | `ACTIVE` | All staff capabilities + manage staff registrations (approve or reject pending staff applications). Automatically bootstrapped via `INITIAL_ADMIN_EMAILS`. |

---

## 4. Matching Engine Logic

The matching engine in [`ItemMatchingService.java`](file:///Users/muthuselvam/Downloads/Campus%20Lost%20and%20Found%20AI-%20Antigravity/backend/src/main/java/com/campus/lostfound/service/ItemMatchingService.java) uses a transparent, weighted scoring model (Total: 100 points):

| Signal | Maximum Points | Explanation |
| :--- | :---: | :--- |
| **Category** | 25 pts | Exact or hierarchical category match (e.g., "water bottle") |
| **Distinctive Features** | 20 pts | Token overlap on physical features (scratches, dents, stickers) |
| **Brand / Make** | 15 pts | Matching visible brand (neutral if brand is not visible) |
| **Color Palette** | 15 pts | Jaccard overlap on recognized color tokens |
| **Keywords & Description** | 10 pts | Textual token overlap across descriptions and search tags |
| **Campus Location** | 10 pts | Same building or campus quadrant |
| **Incident Timeline** | 5 pts | Incident dates consistency |

* Threshold: Candidates scoring below 0.20 (20%) are filtered out.
* Self-matching and closed/resolved items are excluded.
* Every match includes human-readable reasons (e.g., *"Matching category: water bottle"*, *"Compatible campus area: near Library"*).

---

## 5. Getting Started on macOS

### Prerequisites
1. **Java 21+** (`java -version`)
2. **Maven 3.9+** (`mvn -version`)
3. **Node.js 20+** and **npm** (`node -v && npm -v`)

---

### Step 1: Configure Environment Variables

Copy the example file:
```bash
cp .env.example .env
```

Key configuration options in `.env`:
* `GEMINI_API_KEY`: API key for Gemini Vision (`gemini-3.1-flash-lite`).
* `AUTH_DEV_OTP_ENABLED=true`: Prints 6-digit OTP codes directly to server logs for easy local testing without SMTP server.
* `INITIAL_ADMIN_EMAILS=admin@campus.edu`: Comma-separated list of bootstrap administrator email addresses.
* `ALLOWED_CAMPUS_DOMAINS`: Restrict registrations to specific campus domains (e.g. `campus.edu`), or leave empty for all.

---

### Step 2: Build and Run the Java Backend

From the project root:
```bash
cd backend
mvn clean package -DskipTests
java -jar target/campus-lost-found-1.0.0.jar
```
*The Spring Boot server will start on port `8080`.*
*The SQLite database file will be created automatically at `./data/campus_lost_found.db`.*

---

### Step 3: Run the Frontend

In a separate terminal:
```bash
cd frontend
npm install
npm run dev
```
Open **[http://localhost:5173](http://localhost:5173)** in your browser.

---

## 6. Running Automated Tests

Run the full suite of unit and integration tests:
```bash
cd backend
mvn clean test
```

### Verified Test Cases:
* **Empty Database Startup:** Asserts that the SQLite database starts with 0 items and 0 claims.
* **OTP Cryptographic Security:** Asserts SHA-256 salted hashing and prevents plaintext storage.
* **Rate-Limiting & Attempt Limits:** Rejects repeated code requests within 60s and invalidates tokens after 5 incorrect attempts.
* **Role Enforcement & Immutability:** Verifies that browser input cannot override registered roles on subsequent logins.
* **Staff Approval Workflow:** Verifies that new staff enter `PENDING_APPROVAL`, cannot access staff tools, and gain access only upon admin approval.
* **Self-Claim Prevention:** Prevents users from submitting claims on items they reported themselves.
* **Ownership-Based Deletion:** Verifies that students can delete only their own reports, while staff/admin can delete any report.
* **Weighted Matching & Ranking:** Verifies that items with identical category, brand, color, and location score highest.

---

## 7. REST API Reference

### Authentication Endpoints
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/auth/request-code` | Request a 6-digit verification code sent via email / dev log |
| `POST` | `/api/auth/verify-code` | Verify 6-digit OTP, establish server session cookie (`JSESSIONID`) |
| `GET` | `/api/auth/me` | Retrieve currently authenticated user profile and roles |
| `POST` | `/api/auth/logout` | Invalidate current session and clear security context |

### Registry & Items Endpoints
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/health` | System and SQLite connectivity health status |
| `POST` | `/api/ai/analyse-image` | Upload item photo and receive structured Vision AI analysis |
| `POST` | `/api/items/found` | Register a found item report (requires authenticated user) |
| `POST` | `/api/items/lost` | Register a lost item report (requires authenticated user) |
| `GET` | `/api/items` | Query items with optional filters (`status`, `type`, `search`) |
| `GET` | `/api/items/my` | Retrieve items reported by current authenticated user |
| `GET` | `/api/items/{id}` | Get item details (privacy masked for public view) |
| `GET` | `/api/items/{id}/matches` | Run matching engine and retrieve ranked counter-reports |
| `PATCH` | `/api/items/{id}` | Update item status (owner or staff/admin) |
| `DELETE` | `/api/items/{id}` | Remove item report (owner or staff/admin) |

### Claims Endpoints
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/claims` | Submit ownership claim (cannot claim own reported item) |
| `GET` | `/api/claims/my` | Retrieve claims submitted by current user |
| `GET` | `/api/claims/{id}` | Get claim details (claimant or staff/admin view) |
| `GET` | `/api/claims/item/{itemId}` | Get claims for an item |

### Administration & Staff Endpoints
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/admin/items` | Staff view: all items including unmasked private details |
| `GET` | `/api/admin/claims` | Staff view: all claims including ownership proof |
| `PATCH` | `/api/admin/claims/{id}/resolve` | Staff decision: approve or reject claim |
| `GET` | `/api/admin/staff-requests` | Admin view: list users who requested staff role |
| `PATCH` | `/api/admin/staff-requests/{id}` | Admin decision: approve (`ACTIVE`) or reject staff request |
| `GET` | `/api/admin/audit-logs` | Retrieve chronological SQLite audit log trail |

---

## 8. Privacy & Security Considerations

1. **HttpOnly Cookie Sessions:** All session tokens are stored in secure `HttpOnly` cookies (`JSESSIONID`) protected from XSS.
2. **Principal Derivation:** Endpoints derive user identity strictly from the authenticated server session, preventing browser spoofing.
3. **Private Distinguishing Features:** When registering an item or submitting a claim, students can provide hidden details that are never displayed in public listings.
4. **Contact Masking:** Email addresses and phone numbers are automatically masked on public APIs (e.g., `j***@campus.edu`).
5. **No Fabricated Data:** The application enforces zero sample data upon startup.
6. **Sanitized File Storage:** Uploaded photos are stored using randomly generated UUIDs, restricting extensions to safe formats (`.jpg`, `.png`, `.webp`) and preventing path traversal attacks.
7. **Safe Database Queries:** All database interactions use `PreparedStatement` with parameterized values. SQL string concatenation is forbidden.
