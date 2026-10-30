export type ReportType = 'LOST' | 'FOUND';

export type ItemStatus = 'OPEN' | 'MATCHED' | 'CLAIMED' | 'RESOLVED' | 'CLOSED';

export type ClaimStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'RESOLVED';

export type AccountStatus = 'PENDING_APPROVAL' | 'ACTIVE' | 'REJECTED' | 'SUSPENDED';

export type UserRole = 'STUDENT' | 'STAFF' | 'ADMIN';

export interface AuthUser {
  id: string;
  fullName: string;
  campusEmail: string;
  role: UserRole;
  accountStatus: AccountStatus;
  requestedRole?: string;
  emailVerified: boolean;
}

export interface RequestCodeResponse {
  devMode: boolean;
  devOtp?: string | null;
  emailSent: boolean;
  message: string;
  cooldownSeconds: number;
  expiryMinutes: number;
}

export interface ItemAnalysisResponse {
  category: string;
  colour: string[];
  brand: string | null;
  description: string;
  distinctiveFeatures: string[];
  searchKeywords: string[];
  confidence: number;
  fallback: boolean;
  note?: string;
}

export interface Item {
  id: string;
  reportType: ReportType;
  category: string;
  description: string;
  color?: string;
  brand?: string;
  distinctiveFeatures: string[];
  searchKeywords: string[];
  imagePath?: string;
  location: string;
  incidentDate: string;
  status: ItemStatus;
  reporterName: string;
  reporterContact: string;
  privateDetails?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ItemMatch {
  candidateItemId: string;
  item: Item;
  matchScore: number;
  matchPercentage: number;
  matchReasons: string[];
  confidenceLabel: string;
}

export interface Claim {
  id: string;
  itemId: string;
  claimantName: string;
  claimantEmail: string;
  claimMessage: string;
  proofDetails: string;
  status: ClaimStatus;
  resolutionNotes?: string;
  resolvedBy?: string;
  createdAt: string;
  updatedAt: string;
  item?: Item;
}

export interface AuditLog {
  id: string;
  actor: string;
  action: string;
  targetType: string;
  targetId: string;
  details: string;
  createdAt: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}
