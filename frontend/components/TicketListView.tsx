"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { getTicketSummary, listTickets } from "@/lib/api";
import { resolveApiFailure } from "@/lib/api-error";
import { ApiErrorBanner } from "@/components/ApiErrorBanner";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { PriorityBadge } from "@/components/ui/PriorityBadge";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { TicketListSkeleton } from "@/components/ui/Skeleton";
import { displayOptional, formatDateTime } from "@/lib/format";
import {
  DEFAULT_PAGE_SIZE,
  parseSortParam,
  toSortParam,
} from "@/lib/ticket-list-params";
import { EMPTY_STATS, mapSummaryToStats } from "@/lib/ticket-stats";
import type { Ticket, TicketSortField, TicketStatus } from "@/lib/types";
import { TICKET_STATUSES } from "@/lib/types";

const DEBOUNCE_MS = 300;
const SORTABLE_COLUMNS: { field: TicketSortField; label: string }[] = [
  { field: "title", label: "Ticket" },
  { field: "status", label: "Status" },
  { field: "priority", label: "Priority" },
];

export function TicketListView() {
  const [keyword, setKeyword] = useState("");
  const [debouncedQ, setDebouncedQ] = useState("");
  const [status, setStatus] = useState<TicketStatus | "">("");
  const [page, setPage] = useState(1);
  const [sort, setSort] = useState("createdAt,desc");
  const [items, setItems] = useState<Ticket[]>([]);
  const [stats, setStats] = useState(EMPTY_STATS);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [initialLoading, setInitialLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [statsLoading, setStatsLoading] = useState(true);
  const [statsFailure, setStatsFailure] = useState<ReturnType<typeof resolveApiFailure> | null>(
    null
  );
  const [failure, setFailure] = useState<ReturnType<typeof resolveApiFailure> | null>(null);
  const hasLoadedOnce = useRef(false);
  const listAbortRef = useRef<AbortController | null>(null);
  const statsAbortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setDebouncedQ(keyword.trim());
    }, DEBOUNCE_MS);
    return () => window.clearTimeout(timer);
  }, [keyword]);

  const loadSummary = useCallback(async () => {
    statsAbortRef.current?.abort();
    const controller = new AbortController();
    statsAbortRef.current = controller;
    setStatsLoading(true);
    setStatsFailure(null);
    try {
      const summary = await getTicketSummary(controller.signal);
      setStats(mapSummaryToStats(summary));
    } catch (err) {
      if (controller.signal.aborted) {
        return;
      }
      setStatsFailure(resolveApiFailure(err));
      setStats(EMPTY_STATS);
    } finally {
      if (!controller.signal.aborted) {
        setStatsLoading(false);
      }
    }
  }, []);

  const loadTickets = useCallback(async () => {
    listAbortRef.current?.abort();
    const controller = new AbortController();
    listAbortRef.current = controller;

    if (!hasLoadedOnce.current) {
      setInitialLoading(true);
    } else {
      setRefreshing(true);
    }
    setFailure(null);

    try {
      const response = await listTickets({
        q: debouncedQ || undefined,
        status: status || undefined,
        page,
        pageSize: DEFAULT_PAGE_SIZE,
        sort,
        signal: controller.signal,
      });
      setItems(response.items);
      setTotalElements(response.totalElements ?? response.items.length);
      setTotalPages(response.totalPages ?? 1);
      hasLoadedOnce.current = true;
      void loadSummary();
    } catch (err) {
      if (controller.signal.aborted) {
        return;
      }
      setFailure(resolveApiFailure(err));
      if (!hasLoadedOnce.current) {
        setItems([]);
      }
    } finally {
      if (!controller.signal.aborted) {
        setInitialLoading(false);
        setRefreshing(false);
      }
    }
  }, [debouncedQ, status, page, sort, loadSummary]);

  useEffect(() => {
    void loadSummary();
    return () => statsAbortRef.current?.abort();
  }, [loadSummary]);

  useEffect(() => {
    void loadTickets();
    return () => listAbortRef.current?.abort();
  }, [loadTickets]);

  useEffect(() => {
    setPage(1);
  }, [debouncedQ]);

  const sortState = useMemo(() => parseSortParam(sort), [sort]);
  const hasFilters = debouncedQ.length > 0 || status.length > 0;

  function clearFilters() {
    setKeyword("");
    setDebouncedQ("");
    setStatus("");
    setPage(1);
  }

  function toggleSort(field: TicketSortField) {
    const nextDirection =
      sortState.field === field && sortState.direction === "desc" ? "asc" : "desc";
    setSort(toSortParam(field, nextDirection));
    setPage(1);
  }

  function sortIndicator(field: TicketSortField) {
    if (sortState.field !== field) {
      return "↕";
    }
    return sortState.direction === "asc" ? "↑" : "↓";
  }

  const showingFrom = totalElements === 0 ? 0 : (page - 1) * DEFAULT_PAGE_SIZE + 1;
  const showingTo = Math.min(page * DEFAULT_PAGE_SIZE, totalElements);

  return (
    <div className="stack">
      <PageHeader
        title="Tickets"
        description="Search your queue, spot bottlenecks, and keep every request moving."
      />

      <section className="stats-grid" aria-label="Ticket statistics" aria-busy={statsLoading}>
        <div className="card stat-card stat-card-accent stat-card-total">
          <p className="stat-label">Total</p>
          <p className="stat-value">{statsLoading ? "—" : stats.total}</p>
        </div>
        <div className="card stat-card stat-card-accent stat-card-open">
          <p className="stat-label">Open</p>
          <p className="stat-value">{statsLoading ? "—" : stats.OPEN}</p>
        </div>
        <div className="card stat-card stat-card-accent stat-card-progress">
          <p className="stat-label">In progress</p>
          <p className="stat-value">{statsLoading ? "—" : stats.IN_PROGRESS}</p>
        </div>
        <div className="card stat-card stat-card-accent stat-card-resolved">
          <p className="stat-label">Resolved</p>
          <p className="stat-value">{statsLoading ? "—" : stats.RESOLVED}</p>
        </div>
        <div className="card stat-card stat-card-accent stat-card-closed">
          <p className="stat-label">Closed</p>
          <p className="stat-value">{statsLoading ? "—" : stats.CLOSED}</p>
        </div>
        <div className="card stat-card stat-card-accent stat-card-cancelled">
          <p className="stat-label">Cancelled</p>
          <p className="stat-value">{statsLoading ? "—" : stats.CANCELLED}</p>
        </div>
      </section>

      {statsFailure && (
        <div className="stack">
          <ApiErrorBanner failure={statsFailure} />
          <button type="button" className="button button-secondary" onClick={() => void loadSummary()}>
            Retry stats
          </button>
        </div>
      )}

      <form
        className="card filter-panel filter-panel-toolbar"
        onSubmit={(event) => event.preventDefault()}
      >
        <p className="filter-panel-label">Find tickets</p>
        <label className="field search-field">
          <span>Search</span>
          <span className="search-icon" aria-hidden="true">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="11" cy="11" r="7" />
              <path d="M20 20l-3-3" />
            </svg>
          </span>
          <input
            type="search"
            placeholder="Search title or description"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
          />
        </label>
        <label className="field">
          <span>Status</span>
          <select
            value={status}
            onChange={(e) => {
              setStatus(e.target.value as TicketStatus | "");
              setPage(1);
            }}
          >
            <option value="">All statuses</option>
            {TICKET_STATUSES.map((value) => (
              <option key={value} value={value}>
                {value.replaceAll("_", " ")}
              </option>
            ))}
          </select>
        </label>
        <div className="form-actions">
          <button type="button" className="button button-secondary" onClick={clearFilters}>
            Clear
          </button>
        </div>
      </form>

      {initialLoading && <TicketListSkeleton />}

      {!initialLoading && failure && (
        <div className="stack">
          <ApiErrorBanner failure={failure} />
          <button type="button" className="button button-secondary" onClick={() => void loadTickets()}>
            Retry
          </button>
        </div>
      )}

      {!initialLoading && !failure && items.length === 0 && (
        <EmptyState
          title={hasFilters ? "No tickets match your filters" : "No tickets yet"}
          description={
            hasFilters
              ? "Try adjusting your search or status filter to find what you need."
              : "Create your first support ticket to start tracking work."
          }
          action={
            <Link href="/tickets/new" className="button button-primary">
              {hasFilters ? "Create ticket" : "Create your first ticket"}
            </Link>
          }
        />
      )}

      {!initialLoading && !failure && items.length > 0 && (
        <div className={`ticket-table-card card ${refreshing ? "is-refreshing" : ""}`}>
          <div className="ticket-table-toolbar">
            <div>
              <h2 className="ticket-table-heading">Your queue</h2>
              <p className="ticket-table-sub">
                {hasFilters ? "Filtered results" : "Newest first"}
              </p>
            </div>
            <span className="ticket-table-count" aria-live="polite">
              {refreshing ? "Refreshing…" : `${showingFrom}–${showingTo} of ${totalElements}`}
            </span>
          </div>
          <table className="ticket-table">
            <thead>
              <tr>
                {SORTABLE_COLUMNS.map((column) => (
                  <th key={column.field} scope="col">
                    <button
                      type="button"
                      className={`sort-button ${sortState.field === column.field ? "is-active" : ""}`}
                      onClick={() => toggleSort(column.field)}
                    >
                      {column.label}
                      <span className="sort-indicator" aria-hidden="true">
                        {sortIndicator(column.field)}
                      </span>
                    </button>
                  </th>
                ))}
                <th scope="col">Assignee</th>
                <th scope="col">
                  <button
                    type="button"
                    className={`sort-button ${sortState.field === "updatedAt" ? "is-active" : ""}`}
                    onClick={() => toggleSort("updatedAt")}
                  >
                    Updated
                    <span className="sort-indicator" aria-hidden="true">
                      {sortIndicator("updatedAt")}
                    </span>
                  </button>
                </th>
                <th scope="col">
                  <span className="sr-only">View</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {items.map((ticket) => (
                <tr key={ticket.id}>
                  <td>
                    <Link href={`/tickets/${ticket.id}`} className="ticket-table-link">
                      <p className="ticket-cell-title">{ticket.title}</p>
                      {ticket.description && (
                        <p className="ticket-cell-snippet">{ticket.description}</p>
                      )}
                    </Link>
                  </td>
                  <td>
                    <StatusBadge status={ticket.status} />
                  </td>
                  <td>
                    <PriorityBadge priority={ticket.priority} />
                  </td>
                  <td className="small">{displayOptional(ticket.assignee)}</td>
                  <td className="small muted">{formatDateTime(ticket.updatedAt)}</td>
                  <td>
                    <Link href={`/tickets/${ticket.id}`} className="view-link">
                      View
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="ticket-mobile-list">
            {items.map((ticket) => (
              <Link key={ticket.id} href={`/tickets/${ticket.id}`} className="ticket-mobile-card">
                <div className="detail-badges" style={{ marginBottom: "0.5rem" }}>
                  <StatusBadge status={ticket.status} />
                  <PriorityBadge priority={ticket.priority} />
                </div>
                <p className="ticket-cell-title">{ticket.title}</p>
                {ticket.description && (
                  <p className="ticket-cell-snippet">{ticket.description}</p>
                )}
                <p className="small muted" style={{ marginTop: "0.5rem" }}>
                  {displayOptional(ticket.assignee)} · Updated {formatDateTime(ticket.updatedAt)}
                </p>
              </Link>
            ))}
          </div>

          {totalPages > 1 && (
            <nav className="pagination-bar" aria-label="Ticket list pagination">
              <button
                type="button"
                className="button button-secondary"
                disabled={page <= 1 || refreshing}
                onClick={() => setPage((current) => Math.max(1, current - 1))}
              >
                Previous
              </button>
              <span className="pagination-status">
                Page {page} of {totalPages}
              </span>
              <button
                type="button"
                className="button button-secondary"
                disabled={page >= totalPages || refreshing}
                onClick={() => setPage((current) => Math.min(totalPages, current + 1))}
              >
                Next
              </button>
            </nav>
          )}
        </div>
      )}
    </div>
  );
}
