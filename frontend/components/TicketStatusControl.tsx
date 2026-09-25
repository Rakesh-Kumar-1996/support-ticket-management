"use client";

import { useState } from "react";
import { updateTicketStatus } from "@/lib/api";
import { resolveApiFailure } from "@/lib/api-error";
import { ApiErrorBanner } from "@/components/ApiErrorBanner";
import { StatusBadge } from "@/components/ui/StatusBadge";
import {
  allowedTargetStatuses,
  formatStatusLabel,
  isDestructiveTransition,
  transitionActionLabel,
} from "@/lib/transitions";
import type { TicketDetail, TicketStatus } from "@/lib/types";

type TicketStatusControlProps = {
  ticket: TicketDetail;
  onUpdated: (ticket: TicketDetail) => void;
};

export function TicketStatusControl({ ticket, onUpdated }: TicketStatusControlProps) {
  const targets = allowedTargetStatuses(ticket.status);
  const [submitting, setSubmitting] = useState<TicketStatus | null>(null);
  const [failure, setFailure] = useState<ReturnType<typeof resolveApiFailure> | null>(
    null
  );

  async function transitionTo(target: TicketStatus) {
    setFailure(null);
    setSubmitting(target);
    try {
      const updated = await updateTicketStatus(ticket.id, { status: target });
      onUpdated({ ...ticket, ...updated });
    } catch (err) {
      setFailure(resolveApiFailure(err));
    } finally {
      setSubmitting(null);
    }
  }

  return (
    <section className="card status-panel stack">
      <h2 className="panel-title">Status actions</h2>
      <div className="status-current-block">
        <span className="status-current-label">Current status</span>
        <StatusBadge status={ticket.status} />
      </div>

      {failure && <ApiErrorBanner failure={failure} />}

      {targets.length === 0 ? (
        <p className="muted small">
          No further transitions are available from {formatStatusLabel(ticket.status)}.
        </p>
      ) : (
        <div className="status-actions">
          {targets.map((target) => (
            <button
              key={target}
              type="button"
              className={
                isDestructiveTransition(target)
                  ? "button button-secondary"
                  : "button button-primary"
              }
              disabled={submitting !== null}
              onClick={() => transitionTo(target)}
            >
              {submitting === target
                ? "Updating…"
                : transitionActionLabel(ticket.status, target)}
            </button>
          ))}
        </div>
      )}
    </section>
  );
}
