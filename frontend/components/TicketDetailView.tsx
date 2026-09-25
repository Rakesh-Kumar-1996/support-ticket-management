"use client";

import { useCallback, useEffect, useState } from "react";
import { getTicket } from "@/lib/api";
import { resolveApiFailure } from "@/lib/api-error";
import { ApiErrorBanner } from "@/components/ApiErrorBanner";
import { TicketCommentForm } from "@/components/TicketCommentForm";
import { TicketEditForm } from "@/components/TicketEditForm";
import { TicketStatusControl } from "@/components/TicketStatusControl";
import { BackLink } from "@/components/ui/BackLink";
import { PriorityBadge } from "@/components/ui/PriorityBadge";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { TicketDetailSkeleton } from "@/components/ui/Skeleton";
import { displayOptional, formatDateTime } from "@/lib/format";
import type { TicketDetail } from "@/lib/types";

type TicketDetailViewProps = {
  ticketId: string;
};

function descriptionPreview(description: string | null): string | null {
  if (!description?.trim()) {
    return null;
  }
  const line = description.trim().split("\n")[0];
  return line.length > 160 ? `${line.slice(0, 157)}…` : line;
}

export function TicketDetailView({ ticketId }: TicketDetailViewProps) {
  const [ticket, setTicket] = useState<TicketDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadFailure, setLoadFailure] = useState<ReturnType<
    typeof resolveApiFailure
  > | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setLoadFailure(null);
    try {
      const data = await getTicket(ticketId);
      setTicket(data);
    } catch (err) {
      setTicket(null);
      setLoadFailure(resolveApiFailure(err));
    } finally {
      setLoading(false);
    }
  }, [ticketId]);

  useEffect(() => {
    void load();
  }, [load]);

  const handleUpdated = useCallback((next: TicketDetail) => {
    setTicket(next);
  }, []);

  if (loading) {
    return <TicketDetailSkeleton />;
  }

  if (loadFailure) {
    return (
      <div className="stack">
        <BackLink href="/">Back to tickets</BackLink>
        <div className="card alert-card">
          <ApiErrorBanner failure={loadFailure} />
        </div>
        {loadFailure.kind === "network" && (
          <button type="button" className="button button-secondary" onClick={() => void load()}>
            Retry
          </button>
        )}
      </div>
    );
  }

  if (!ticket) {
    return null;
  }

  const preview = descriptionPreview(ticket.description);

  return (
    <div className="stack detail-page">
      <BackLink href="/">Back to tickets</BackLink>

      <header className="detail-page-header">
        <div className="detail-page-header-main">
          <h1 className="detail-title">{ticket.title}</h1>
          {preview && <p className="detail-subtitle">{preview}</p>}
          <p className="detail-meta-row">
            Ticket ID <span className="mono">{ticket.id}</span>
          </p>
        </div>
        <div className="detail-badges">
          <StatusBadge status={ticket.status} />
          <PriorityBadge priority={ticket.priority} />
        </div>
      </header>

      <div className="detail-layout">
        <div className="detail-main">
          <section className="card description-card">
            <h2 className="panel-title">Description</h2>
            <p className="description-block">{displayOptional(ticket.description)}</p>
          </section>

          <section className="card comments-card">
            <h2 className="panel-title">Comments</h2>
            {ticket.comments.length === 0 ? (
              <div className="comments-empty muted">
                No comments yet. Be the first to add an update for this ticket.
              </div>
            ) : (
              <ol className="comment-timeline">
                {ticket.comments.map((comment) => (
                  <li key={comment.id} className="comment-item">
                    <div className="comment-header">
                      <span className="comment-author">
                        {comment.author?.trim() ? comment.author : "Unknown author"}
                      </span>
                      <time className="comment-time" dateTime={comment.createdAt}>
                        {formatDateTime(comment.createdAt)}
                      </time>
                    </div>
                    <p className="comment-body">{comment.body}</p>
                  </li>
                ))}
              </ol>
            )}
            <TicketCommentForm ticketId={ticket.id} onUpdated={handleUpdated} />
          </section>
        </div>

        <aside className="detail-aside">
          <section className="card info-card">
            <h2 className="panel-title">Ticket details</h2>
            <dl className="info-list">
              <div>
                <dt>Status</dt>
                <dd><StatusBadge status={ticket.status} /></dd>
              </div>
              <div>
                <dt>Priority</dt>
                <dd><PriorityBadge priority={ticket.priority} /></dd>
              </div>
              <div>
                <dt>Assignee</dt>
                <dd className="info-value">{displayOptional(ticket.assignee)}</dd>
              </div>
              <div>
                <dt>Created</dt>
                <dd className="info-value">{formatDateTime(ticket.createdAt)}</dd>
              </div>
              <div>
                <dt>Updated</dt>
                <dd className="info-value">{formatDateTime(ticket.updatedAt)}</dd>
              </div>
            </dl>
          </section>

          <TicketStatusControl ticket={ticket} onUpdated={handleUpdated} />
          <TicketEditForm ticket={ticket} onUpdated={handleUpdated} />
        </aside>
      </div>
    </div>
  );
}
