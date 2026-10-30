import { ApiResponse, Item, ItemAnalysisResponse, ItemMatch, Claim, AuditLog, ClaimStatus, AuthUser, RequestCodeResponse } from '../types';

const API_BASE = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/+$/, '') + '/api';

async function handleResponse<T>(res: Response): Promise<T> {
  const json: ApiResponse<T> = await res.json();
  if (!res.ok || !json.success) {
    throw new Error(json.message || `Request failed with status ${res.status}`);
  }
  return json.data;
}

export const api = {
  async checkHealth(): Promise<{ status: string; service: string; database: string }> {
    const res = await fetch(`${API_BASE}/health`, { credentials: 'include' });
    return handleResponse(res);
  },

  // Auth Endpoints
  async requestCode(email: string, fullName?: string, role?: string): Promise<RequestCodeResponse> {
    const res = await fetch(`${API_BASE}/auth/request-code`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ email, fullName, role }),
    });
    const json: ApiResponse<RequestCodeResponse> = await res.json();
    if (!res.ok || !json.success) {
      throw new Error(json.message || 'Failed to send verification code');
    }
    return json.data;
  },

  async verifyCode(email: string, code: string, role?: string, fullName?: string): Promise<AuthUser> {
    const res = await fetch(`${API_BASE}/auth/verify-code`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ email, code, role, fullName }),
    });
    return handleResponse<AuthUser>(res);
  },

  async getMe(): Promise<AuthUser | null> {
    try {
      const res = await fetch(`${API_BASE}/auth/me`, { credentials: 'include' });
      if (res.status === 401 || !res.ok) return null;
      const json: ApiResponse<AuthUser> = await res.json();
      return json.success ? json.data : null;
    } catch {
      return null;
    }
  },

  async logout(): Promise<void> {
    await fetch(`${API_BASE}/auth/logout`, {
      method: 'POST',
      credentials: 'include',
    });
  },

  // AI & Upload
  async analyzeImage(file: File): Promise<{ imagePath: string; analysis: ItemAnalysisResponse }> {
    const formData = new FormData();
    formData.append('file', file);

    const res = await fetch(`${API_BASE}/ai/analyse-image`, {
      method: 'POST',
      credentials: 'include',
      body: formData,
    });
    return handleResponse(res);
  },

  // Item Management
  async reportLostItem(data: Partial<Item>): Promise<Item> {
    const res = await fetch(`${API_BASE}/items/lost`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(data),
    });
    return handleResponse(res);
  },

  async reportFoundItem(data: Partial<Item>): Promise<Item> {
    const res = await fetch(`${API_BASE}/items/found`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(data),
    });
    return handleResponse(res);
  },

  async getItems(status?: string, type?: string, search?: string): Promise<Item[]> {
    const params = new URLSearchParams();
    if (status) params.append('status', status);
    if (type) params.append('type', type);
    if (search) params.append('search', search);

    const res = await fetch(`${API_BASE}/items?${params.toString()}`, { credentials: 'include' });
    return handleResponse(res);
  },

  async getMyItems(): Promise<Item[]> {
    const res = await fetch(`${API_BASE}/items/my`, { credentials: 'include' });
    return handleResponse(res);
  },

  async getItem(id: string): Promise<Item> {
    const res = await fetch(`${API_BASE}/items/${id}`, { credentials: 'include' });
    return handleResponse(res);
  },

  async getItemMatches(id: string): Promise<ItemMatch[]> {
    const res = await fetch(`${API_BASE}/items/${id}/matches`, { credentials: 'include' });
    return handleResponse(res);
  },

  async updateItemStatus(id: string, status: string): Promise<boolean> {
    const res = await fetch(`${API_BASE}/items/${id}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ status }),
    });
    return handleResponse(res);
  },

  async deleteItem(id: string): Promise<boolean> {
    const res = await fetch(`${API_BASE}/items/${id}`, {
      method: 'DELETE',
      credentials: 'include',
    });
    return handleResponse(res);
  },

  // Claim Management
  async submitClaim(claimData: {
    itemId: string;
    claimantName?: string;
    claimantEmail?: string;
    claimMessage: string;
    proofDetails: string;
  }): Promise<Claim> {
    const res = await fetch(`${API_BASE}/claims`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(claimData),
    });
    return handleResponse(res);
  },

  async getMyClaims(): Promise<Claim[]> {
    const res = await fetch(`${API_BASE}/claims/my`, { credentials: 'include' });
    return handleResponse(res);
  },

  async getClaim(id: string): Promise<Claim> {
    const res = await fetch(`${API_BASE}/claims/${id}`, { credentials: 'include' });
    return handleResponse(res);
  },

  // Staff / Admin Endpoints
  async getAdminItems(status?: string, type?: string, search?: string): Promise<Item[]> {
    const params = new URLSearchParams();
    if (status) params.append('status', status);
    if (type) params.append('type', type);
    if (search) params.append('search', search);

    const res = await fetch(`${API_BASE}/admin/items?${params.toString()}`, { credentials: 'include' });
    return handleResponse(res);
  },

  async getAdminClaims(status?: string): Promise<Claim[]> {
    const params = new URLSearchParams();
    if (status) params.append('status', status);

    const res = await fetch(`${API_BASE}/admin/claims?${params.toString()}`, { credentials: 'include' });
    return handleResponse(res);
  },

  async resolveClaim(claimId: string, status: ClaimStatus, resolutionNotes: string, adminActor: string): Promise<Claim> {
    const res = await fetch(`${API_BASE}/admin/claims/${claimId}/resolve`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ status, resolutionNotes, adminActor }),
    });
    return handleResponse(res);
  },

  async getStaffRequests(): Promise<AuthUser[]> {
    const res = await fetch(`${API_BASE}/admin/staff-requests`, { credentials: 'include' });
    return handleResponse(res);
  },

  async resolveStaffRequest(userId: string, status: 'ACTIVE' | 'REJECTED', reason?: string): Promise<AuthUser> {
    const res = await fetch(`${API_BASE}/admin/staff-requests/${userId}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ status, reason }),
    });
    return handleResponse(res);
  },

  async getAuditLogs(limit: number = 50): Promise<AuditLog[]> {
    const res = await fetch(`${API_BASE}/admin/audit-logs?limit=${limit}`, { credentials: 'include' });
    return handleResponse(res);
  }
};
