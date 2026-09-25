import type {
  ApiErrorBody,
  CreateCommentRequest,
  CreateTicketRequest,
  Ticket,
  TicketDetail,
  TicketListResponse,
  UpdateStatusRequest,
  UpdateTicketRequest,
} from "@/lib/types";

const API_BASE = "/api";

export class ApiError extends Error {
  readonly body: ApiErrorBody;

  constructor(body: ApiErrorBody) {
    super(body.message);
    this.name = "ApiError";
    this.body = body;
  }
}

async function parseJson<T>(response: Response): Promise<T | undefined> {
  const text = await response.text();
  if (!text) {
    return undefined;
  }
  return JSON.parse(text) as T;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...init,
      headers: {
        Accept: "application/json",
        ...(init?.body ? { "Content-Type": "application/json" } : {}),
        ...init?.headers,
      },
    });
  } catch {
    throw new TypeError("Network request failed");
  }

  if (!response.ok) {
    const errorBody = await parseJson<ApiErrorBody>(response);
    if (errorBody?.message) {
      throw new ApiError(errorBody);
    }
    const fallback: ApiErrorBody = {
      status: response.status,
      error: response.statusText || "Error",
      code: response.status >= 500 ? "INTERNAL_ERROR" : "REQUEST_FAILED",
      message:
        response.status >= 500
          ? "The server encountered an error. Please try again later."
          : `Request failed (${response.status}).`,
      path: `${API_BASE}${path}`,
      fieldErrors: [],
    };
    throw new ApiError(fallback);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const data = await parseJson<T>(response);
  if (data === undefined) {
    throw new Error("Empty response from API");
  }
  return data;
}

export function listTickets(params?: {
  q?: string;
  status?: string;
}): Promise<TicketListResponse> {
  const search = new URLSearchParams();
  const q = params?.q?.trim();
  const status = params?.status?.trim();
  if (q) {
    search.set("q", q);
  }
  if (status) {
    search.set("status", status);
  }
  const query = search.toString();
  return request<TicketListResponse>(`/tickets${query ? `?${query}` : ""}`);
}

export function getTicket(id: string): Promise<TicketDetail> {
  return request<TicketDetail>(`/tickets/${id}`);
}

export function createTicket(body: CreateTicketRequest): Promise<Ticket> {
  const payload: CreateTicketRequest = { title: body.title };
  if (body.description?.length) {
    payload.description = body.description;
  }
  if (body.priority) {
    payload.priority = body.priority;
  }
  if (body.assignee?.length) {
    payload.assignee = body.assignee;
  }
  return request<Ticket>("/tickets", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateTicket(id: string, body: UpdateTicketRequest): Promise<Ticket> {
  const payload: UpdateTicketRequest = {};
  if (body.title !== undefined) {
    payload.title = body.title;
  }
  if (body.description !== undefined) {
    payload.description = body.description;
  }
  if (body.priority !== undefined) {
    payload.priority = body.priority;
  }
  if (body.assignee !== undefined) {
    payload.assignee = body.assignee;
  }
  return request<Ticket>(`/tickets/${id}`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export function updateTicketStatus(
  id: string,
  body: UpdateStatusRequest
): Promise<Ticket> {
  return request<Ticket>(`/tickets/${id}/status`, {
    method: "PATCH",
    body: JSON.stringify(body),
  });
}

export function addComment(
  id: string,
  body: CreateCommentRequest
): Promise<{ id: string; ticketId: string; body: string; author: string | null; createdAt: string }> {
  const payload: CreateCommentRequest = { body: body.body };
  if (body.author?.length) {
    payload.author = body.author;
  }
  return request(`/tickets/${id}/comments`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}
