export type TicketStatus =
  | "OPEN"
  | "IN_PROGRESS"
  | "RESOLVED"
  | "CLOSED"
  | "CANCELLED";

export type TicketPriority = "LOW" | "MEDIUM" | "HIGH";

export interface Ticket {
  id: string;
  title: string;
  description: string | null;
  status: TicketStatus;
  priority: TicketPriority;
  assignee: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Comment {
  id: string;
  ticketId: string;
  body: string;
  author: string | null;
  createdAt: string;
}

export interface TicketDetail extends Ticket {
  comments: Comment[];
}

export interface TicketListResponse {
  items: Ticket[];
  totalElements?: number;
  totalPages?: number;
  page?: number;
  pageSize?: number;
  sort?: string;
}

export interface TicketSummaryResponse {
  total: number;
  open: number;
  inProgress: number;
  resolved: number;
  closed: number;
  cancelled: number;
}

export type TicketSortField = "createdAt" | "updatedAt" | "title" | "priority" | "status";
export type TicketSortDirection = "asc" | "desc";

export interface ListTicketsParams {
  q?: string;
  status?: string;
  page?: number;
  pageSize?: number;
  sort?: string;
  signal?: AbortSignal;
}

export interface CreateTicketRequest {
  title: string;
  description?: string;
  priority?: TicketPriority;
  assignee?: string;
}

export interface UpdateTicketRequest {
  title?: string;
  description?: string | null;
  priority?: TicketPriority;
  assignee?: string | null;
}

export interface UpdateStatusRequest {
  status: TicketStatus;
}

export interface CreateCommentRequest {
  body: string;
  author?: string;
}

export interface FieldErrorItem {
  field: string;
  message: string;
}

export interface ApiErrorBody {
  status: number;
  error: string;
  code: string;
  message: string;
  path: string;
  fieldErrors?: FieldErrorItem[];
}

export const TICKET_STATUSES: TicketStatus[] = [
  "OPEN",
  "IN_PROGRESS",
  "RESOLVED",
  "CLOSED",
  "CANCELLED",
];

export const TICKET_PRIORITIES: TicketPriority[] = ["LOW", "MEDIUM", "HIGH"];
