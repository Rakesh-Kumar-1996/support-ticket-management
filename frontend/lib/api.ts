import { buildListTicketsQuery } from "@/lib/ticket-list-params";
import type {
  ApiErrorBody,
  CreateCommentRequest,
  CreateTicketRequest,
  ListTicketsParams,
  Ticket,
  TicketDetail,
  TicketListResponse,
  TicketSummaryResponse,
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
  try {
    return JSON.parse(text) as T;
  } catch {
    return undefined;
  }
}

async function request<T>(path: string, init?: RequestInit & { signal?: AbortSignal }): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...init,
      headers: {
        Accept: "application/json",
        ...(init?.body ? { "Content-Type": "application/json" } : {}),
        ...init?.headers,
      },
      signal: init?.signal,
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
          ? "The API is unavailable. Start the Spring Boot backend on port 8080 and try again."
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
    throw new ApiError({
      status: response.status,
      error: response.statusText || "Error",
      code: "INVALID_RESPONSE",
      message:
        "The API returned an invalid response. Start the Spring Boot backend on port 8080 and try again.",
      path: `${API_BASE}${path}`,
      fieldErrors: [],
    });
  }
  return data;
}

export function listTickets(params?: ListTicketsParams): Promise<TicketListResponse> {
  const query = buildListTicketsQuery(params);
  return request<TicketListResponse>(`/tickets${query}`, {
    signal: params?.signal,
  });
}

export function getTicketSummary(signal?: AbortSignal): Promise<TicketSummaryResponse> {
  return request<TicketSummaryResponse>("/tickets/summary", { signal });
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
