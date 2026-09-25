import type { ListTicketsParams, TicketSortDirection, TicketSortField } from "@/lib/types";

export const DEFAULT_PAGE_SIZE = 20;

export function buildListTicketsQuery(params?: ListTicketsParams): string {
  const search = new URLSearchParams();
  const q = params?.q?.trim();
  const status = params?.status?.trim();
  if (q) {
    search.set("q", q);
  }
  if (status) {
    search.set("status", status);
  }
  if (params?.page !== undefined) {
    search.set("page", String(params.page));
  }
  if (params?.pageSize !== undefined) {
    search.set("size", String(params.pageSize));
  }
  if (params?.sort) {
    search.set("sort", params.sort);
  }
  const query = search.toString();
  return query ? `?${query}` : "";
}

export function toSortParam(field: TicketSortField, direction: TicketSortDirection): string {
  return `${field},${direction}`;
}

export function parseSortParam(sort: string): { field: TicketSortField; direction: TicketSortDirection } {
  const [field, direction] = sort.split(",");
  return {
    field: (field as TicketSortField) || "createdAt",
    direction: direction === "asc" ? "asc" : "desc",
  };
}

export function shouldResetPage(
  previous: Pick<ListTicketsParams, "q" | "status" | "sort">,
  next: Pick<ListTicketsParams, "q" | "status" | "sort">
): boolean {
  return previous.q !== next.q || previous.status !== next.status || previous.sort !== next.sort;
}
