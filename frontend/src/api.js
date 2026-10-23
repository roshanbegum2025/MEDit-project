// API client for MedIt backend REST API with demo session headers

const BASE_URL = '/api';

export const getHeaders = (persona) => {
  return {
    'Content-Type': 'application/json',
    'X-Demo-Role': persona.role,
    'X-Demo-User-Id': persona.id,
    'X-Demo-User-Name': persona.name,
  };
};

export const api = {
  // Health
  checkHealth: async () => {
    const res = await fetch(`${BASE_URL}/health`);
    return res.json();
  },

  // Stats
  getStats: async () => {
    const res = await fetch(`${BASE_URL}/stats`);
    return res.json();
  },

  // Patients
  getPatients: async (search = '', persona) => {
    const query = search ? `?search=${encodeURIComponent(search)}` : '';
    const res = await fetch(`${BASE_URL}/patients${query}`, {
      headers: getHeaders(persona),
    });
    return res.json();
  },

  getPatient: async (patientId, persona) => {
    const res = await fetch(`${BASE_URL}/patients/${patientId}`, {
      headers: getHeaders(persona),
    });
    return res.json();
  },

  // Unified Timeline Records
  getPatientRecords: async (patientId, filters = {}, persona) => {
    const params = new URLSearchParams();
    if (filters.type && filters.type !== 'all') params.append('type', filters.type);
    if (filters.provider && filters.provider !== 'all') params.append('provider', filters.provider);
    if (filters.search) params.append('search', filters.search);
    if (filters.start_date) params.append('start_date', filters.start_date);
    if (filters.end_date) params.append('end_date', filters.end_date);

    const queryString = params.toString() ? `?${params.toString()}` : '';
    const res = await fetch(`${BASE_URL}/patients/${patientId}/records${queryString}`, {
      headers: getHeaders(persona),
    });

    const data = await res.json();
    if (!res.ok) {
      return { ok: false, status: res.status, ...data };
    }
    return { ok: true, status: 200, ...data };
  },

  // Add Record
  createRecord: async (recordData, persona) => {
    const res = await fetch(`${BASE_URL}/records`, {
      method: 'POST',
      headers: getHeaders(persona),
      body: JSON.stringify(recordData),
    });
    const data = await res.json();
    return { ok: res.ok, status: res.status, ...data };
  },

  // Access Requests
  getAccessRequests: async (params = {}) => {
    const query = new URLSearchParams(params).toString();
    const res = await fetch(`${BASE_URL}/access-requests${query ? `?${query}` : ''}`);
    return res.json();
  },

  createAccessRequest: async (requestData, persona) => {
    const res = await fetch(`${BASE_URL}/access-requests`, {
      method: 'POST',
      headers: getHeaders(persona),
      body: JSON.stringify(requestData),
    });
    const data = await res.json();
    return { ok: res.ok, status: res.status, ...data };
  },

  decideAccessRequest: async (requestId, decisionData, persona) => {
    const res = await fetch(`${BASE_URL}/access-requests/${requestId}/decision`, {
      method: 'POST',
      headers: getHeaders(persona),
      body: JSON.stringify(decisionData),
    });
    const data = await res.json();
    return { ok: res.ok, status: res.status, ...data };
  },

  // Permissions
  getPermissions: async (patientId) => {
    const query = patientId ? `?patient_id=${patientId}` : '';
    const res = await fetch(`${BASE_URL}/permissions${query}`);
    return res.json();
  },

  revokePermission: async (permissionId, persona) => {
    const res = await fetch(`${BASE_URL}/permissions/${permissionId}/revoke`, {
      method: 'POST',
      headers: getHeaders(persona),
    });
    const data = await res.json();
    return { ok: res.ok, status: res.status, ...data };
  },

  // Audit Logs
  getAuditLogs: async (patientId = null, limit = 100) => {
    const params = new URLSearchParams();
    if (patientId) params.append('patient_id', patientId);
    if (limit) params.append('limit', limit);
    const res = await fetch(`${BASE_URL}/audit-logs?${params.toString()}`);
    return res.json();
  },

  // Providers
  getProviders: async () => {
    const res = await fetch(`${BASE_URL}/providers`);
    return res.json();
  },

  // Auth
  login: async (username, password) => {
    const res = await fetch(`${BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });
    const data = await res.json();
    return { ok: res.ok, status: res.status, ...data };
  },

  register: async (registerData) => {
    const res = await fetch(`${BASE_URL}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(registerData),
    });
    const data = await res.json();
    return { ok: res.ok, status: res.status, ...data };
  },

  // Prescription OCR
  ocrPrescription: async (payload = {}) => {
    const res = await fetch(`${BASE_URL}/prescriptions/ocr`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    const data = await res.json();
    return { ok: res.ok, status: res.status, ...data };
  },

  // Update & Delete Record
  deleteRecord: async (recordId, persona) => {
    const res = await fetch(`${BASE_URL}/records/${recordId}`, {
      method: 'DELETE',
      headers: getHeaders(persona),
    });
    const data = await res.json();
    return { ok: res.ok, status: res.status, ...data };
  },

  updateRecord: async (recordId, recordData, persona) => {
    const res = await fetch(`${BASE_URL}/records/${recordId}`, {
      method: 'PUT',
      headers: getHeaders(persona),
      body: JSON.stringify(recordData),
    });
    const data = await res.json();
    return { ok: res.ok, status: res.status, ...data };
  },

  // Reset Seed
  resetDemoSeed: async () => {
    const res = await fetch(`${BASE_URL}/seed/reset`, {
      method: 'POST',
    });
    return res.json();
  },
};

