-- Campus Lost & Found SQLite Schema

CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    full_name TEXT NOT NULL,
    campus_email TEXT NOT NULL UNIQUE,
    role TEXT NOT NULL DEFAULT 'STUDENT',
    account_status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(account_status IN ('PENDING_APPROVAL', 'ACTIVE', 'REJECTED', 'SUSPENDED')),
    requested_role TEXT,
    email_verified INTEGER NOT NULL DEFAULT 0,
    approved_by TEXT,
    approved_at TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(campus_email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(account_status);

CREATE TABLE IF NOT EXISTS otp_tokens (
    id TEXT PRIMARY KEY,
    email TEXT NOT NULL,
    otp_hash TEXT NOT NULL,
    salt TEXT NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 5,
    expires_at TEXT NOT NULL,
    consumed INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_otp_email ON otp_tokens(email);
CREATE INDEX IF NOT EXISTS idx_otp_created ON otp_tokens(created_at);

CREATE TABLE IF NOT EXISTS items (
    id TEXT PRIMARY KEY,
    report_type TEXT NOT NULL CHECK(report_type IN ('LOST', 'FOUND')),
    category TEXT NOT NULL,
    description TEXT NOT NULL,
    color TEXT,
    brand TEXT,
    distinctive_features TEXT,
    search_keywords TEXT,
    image_path TEXT,
    location TEXT NOT NULL,
    incident_date TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'OPEN' CHECK(status IN ('OPEN', 'MATCHED', 'CLAIMED', 'RESOLVED', 'CLOSED')),
    reporter_name TEXT NOT NULL,
    reporter_contact TEXT NOT NULL,
    reporter_user_id TEXT,
    private_details TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_items_report_type ON items(report_type);
CREATE INDEX IF NOT EXISTS idx_items_status ON items(status);
CREATE INDEX IF NOT EXISTS idx_items_category ON items(category);
CREATE INDEX IF NOT EXISTS idx_items_created_at ON items(created_at);
CREATE INDEX IF NOT EXISTS idx_items_reporter ON items(reporter_user_id);

CREATE TABLE IF NOT EXISTS claims (
    id TEXT PRIMARY KEY,
    item_id TEXT NOT NULL,
    claimant_user_id TEXT,
    claimant_name TEXT NOT NULL,
    claimant_email TEXT NOT NULL,
    claim_message TEXT NOT NULL,
    proof_details TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING', 'APPROVED', 'REJECTED', 'RESOLVED')),
    resolution_notes TEXT,
    resolved_by TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    FOREIGN KEY(item_id) REFERENCES items(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_claims_item_id ON claims(item_id);
CREATE INDEX IF NOT EXISTS idx_claims_status ON claims(status);
CREATE INDEX IF NOT EXISTS idx_claims_claimant ON claims(claimant_user_id);

CREATE TABLE IF NOT EXISTS matches (
    id TEXT PRIMARY KEY,
    lost_item_id TEXT NOT NULL,
    found_item_id TEXT NOT NULL,
    match_score REAL NOT NULL,
    match_reasons TEXT NOT NULL,
    review_status TEXT NOT NULL DEFAULT 'PENDING' CHECK(review_status IN ('PENDING', 'CONFIRMED', 'DISMISSED')),
    created_at TEXT NOT NULL,
    FOREIGN KEY(lost_item_id) REFERENCES items(id) ON DELETE CASCADE,
    FOREIGN KEY(found_item_id) REFERENCES items(id) ON DELETE CASCADE,
    UNIQUE(lost_item_id, found_item_id)
);

CREATE INDEX IF NOT EXISTS idx_matches_lost ON matches(lost_item_id);
CREATE INDEX IF NOT EXISTS idx_matches_found ON matches(found_item_id);

CREATE TABLE IF NOT EXISTS audit_logs (
    id TEXT PRIMARY KEY,
    actor TEXT NOT NULL,
    action TEXT NOT NULL,
    target_type TEXT NOT NULL,
    target_id TEXT NOT NULL,
    details TEXT,
    created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_logs(created_at);
