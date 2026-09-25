"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { listTickets } from "@/lib/api";
import { resolveApiFailure } from "@/lib/api-error";
import { ApiErrorBanner } from "@/components/ApiErrorBanner";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { PriorityBadge } from "@/components/ui/PriorityBadge";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { TicketListSkeleton } from "@/components/ui/Skeleton";
import { displayOptional, formatDateTime } from "@/lib/format";
import type { Ticket, TicketStatus } from "@/lib/types";
import { TICKET_STATUSES } from "@/lib/types";

export function TicketListView() {
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState<TicketStatus | "">("");
  const [appliedQ, setAppliedQ] = useState("");
  const [appliedStatus, setAppliedStatus] = useState<TicketStatus | "">("");
  const [items, setItems] = useState<Ticket[]>([]);
  const [statsSource, setStatsSource] = useState<Ticket[]>([]);
  const [loading, setLoading] = useState(true);
  const [failure, setFailure] = useState<ReturnType<typeof resolveApiFailure> | null>(
    null
  );

  const load = useCallback(async (q: string, statusFilter: TicketStatus | "") => {
    setLoading(true);
    setFailure(null);
    try {
      const response = await listTickets({
        q: q || undefined,
        status: statusFilter || undefined,
      });
      setItems(response.items);
    } catch (err) {
      setFailure(resolveApiFailure(err));
      setItems([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void listTickets()
      .then((response) => setStatsSource(response.items))
      .catch(() => setStatsSource([]));
  }, []);

  useEffect(() => {
    void load(appliedQ, appliedStatus);
  }, [appliedQ, appliedStatus, load]);

  const stats = useMemo(() => {
    const counts = {
      total: statsSource.length,
      OPEN: 0,
      IN_PROGRESS: 0,
      RESOLVED: 0,
      CLOSED: 0,
    };
    for (const ticket of statsSource) {
      if (ticket.status in counts) {
        counts[ticket.status as keyof typeof counts] += 1;
      }
    }
    return counts;
  }, [statsSource]);

  function applyFilters(event: React.FormEvent) {
    event.preventDefault();
    setAppliedQ(keyword.trim());
    setAppliedStatus(status);
  }

  function clearFilters() {
    setKeyword("");
    setStatus("");
    setAppliedQ("");
    setAppliedStatus("");
  }

  const hasFilters = appliedQ.length > 0 || appliedStatus.length > 0;

  return (
    <div className="stack">
      <PageHeader
        title="Tickets"
        description="Search your queue, spot bottlenecks, and keep every request moving."
      />

      <section className="stats-grid" aria-label="Ticket statistics">
        <div className="card stat-card stat-card-accent stat-card-total">
          <p className="stat-label">Total</p>
          <p className="stat-value">{stats.total}</p>
        </div>
        <div className="card stat-card stat-card-accent stat-card-open">
          <p className="stat-label">Open</p>
          <p className="stat-value">{stats.OPEN}</p>
        </div>
        <div className="card stat-card stat-card-accent stat-card-progress">
          <p className="stat-label">In progress</p>
          <p className="stat-value">{stats.IN_PROGRESS}</p>
        </div>
        <div className="card stat-card stat-card-accent stat-card-resolved">
          <p className="stat-label">Resolved</p>
          <p className="stat-value">{stats.RESOLVED}</p>
        </div>
        <div className="card stat-card stat-card-accent stat-card-closed">
          <p className="stat-label">Closed</p>
          <p className="stat-value">{stats.CLOSED}</p>
        </div>
      </section>

      <form className="card filter-panel filter-panel-toolbar" onSubmit={applyFilters}>
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
            onChange={(e) => setStatus(e.target.value as TicketStatus | "")}
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
          <button type="submit" className="button button-primary">
            Apply filters
          </button>
          <button type="button" className="button button-secondary" onClick={clearFilters}>
            Clear
          </button>
        </div>
      </form>

      {loading && <TicketListSkeleton />}

      {!loading && failure && (
        <div className="stack">
          <ApiErrorBanner failure={failure} />
          <button
            type="button"
            className="button button-secondary"
            onClick={() => load(appliedQ, appliedStatus)}
          >
            Retry
          </button>
        </div>
      )}

      {!loading && !failure && items.length === 0 && (
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

      {!loading && !failure && items.length > 0 && (
        <div className="ticket-table-card card">
          <div className="ticket-table-toolbar">
            <div>
              <h2 className="ticket-table-heading">Your queue</h2>
              <p className="ticket-table-sub">
                {hasFilters ? "Filtered results" : "Latest updates first"}
              </p>
            </div>
            <span className="ticket-table-count">
              {items.length} {items.length === 1 ? "ticket" : "tickets"}
            </span>
          </div>
          <table className="ticket-table">
            <thead>
              <tr>
                <th scope="col">Ticket</th>
                <th scope="col">Status</th>
                <th scope="col">Priority</th>
                <th scope="col">Assignee</th>
                <th scope="col">Updated</th>
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
        </div>
      )}
    </div>
  );
}
