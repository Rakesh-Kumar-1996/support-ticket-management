"use client";

import { useEffect, useState } from "react";
import { updateTicket } from "@/lib/api";
import { resolveApiFailure } from "@/lib/api-error";
import { ApiErrorBanner } from "@/components/ApiErrorBanner";
import type { TicketDetail, TicketPriority } from "@/lib/types";
import { TICKET_PRIORITIES } from "@/lib/types";

type TicketEditFormProps = {
  ticket: TicketDetail;
  onUpdated: (ticket: TicketDetail) => void;
};

export function TicketEditForm({ ticket, onUpdated }: TicketEditFormProps) {
  const [title, setTitle] = useState(ticket.title);
  const [description, setDescription] = useState(ticket.description ?? "");
  const [priority, setPriority] = useState<TicketPriority>(ticket.priority);
  const [assignee, setAssignee] = useState(ticket.assignee ?? "");
  const [submitting, setSubmitting] = useState(false);
  const [failure, setFailure] = useState<ReturnType<typeof resolveApiFailure> | null>(
    null
  );
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    setTitle(ticket.title);
    setDescription(ticket.description ?? "");
    setPriority(ticket.priority);
    setAssignee(ticket.assignee ?? "");
  }, [ticket]);

  function resetForm() {
    setTitle(ticket.title);
    setDescription(ticket.description ?? "");
    setPriority(ticket.priority);
    setAssignee(ticket.assignee ?? "");
    setFailure(null);
    setFieldErrors({});
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setFailure(null);
    setFieldErrors({});

    const trimmedTitle = title.trim();
    if (!trimmedTitle) {
      setFieldErrors({ title: "Title is required." });
      return;
    }

    setSubmitting(true);
    try {
      const updated = await updateTicket(ticket.id, {
        title: trimmedTitle,
        description: description === "" ? "" : description,
        priority,
        assignee: assignee === "" ? "" : assignee,
      });
      onUpdated({ ...ticket, ...updated });
    } catch (err) {
      const resolved = resolveApiFailure(err);
      setFailure(resolved);
      setFieldErrors(resolved.fieldErrors);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="card form-card stack edit-ticket-panel">
      <h2 className="panel-title">Edit ticket</h2>
      <p className="field-hint">
        Update the ticket details below. Status is managed separately above.
      </p>

      {failure && <ApiErrorBanner failure={failure} />}

      <form className="stack" onSubmit={handleSubmit}>
        <label className="field">
          <span>Title <span className="required" aria-hidden="true">*</span></span>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            maxLength={200}
            required
          />
          {fieldErrors.title && <span className="field-error">{fieldErrors.title}</span>}
        </label>

        <label className="field">
          <span>Description</span>
          <textarea
            rows={4}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Leave empty to clear"
            maxLength={10000}
          />
          {fieldErrors.description && (
            <span className="field-error">{fieldErrors.description}</span>
          )}
        </label>

        <label className="field">
          <span>Priority</span>
          <select
            value={priority}
            onChange={(e) => setPriority(e.target.value as TicketPriority)}
          >
            {TICKET_PRIORITIES.map((value) => (
              <option key={value} value={value}>
                {value.charAt(0) + value.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
          {fieldErrors.priority && (
            <span className="field-error">{fieldErrors.priority}</span>
          )}
        </label>

        <label className="field">
          <span>Assignee</span>
          <input
            type="text"
            value={assignee}
            onChange={(e) => setAssignee(e.target.value)}
            placeholder="Leave empty to clear"
            maxLength={200}
          />
          {fieldErrors.assignee && (
            <span className="field-error">{fieldErrors.assignee}</span>
          )}
        </label>

        <div className="form-actions form-actions-split">
          <button type="button" className="button button-secondary" onClick={resetForm}>
            Cancel
          </button>
          <button type="submit" className="button button-primary" disabled={submitting}>
            {submitting ? "Saving…" : "Save changes"}
          </button>
        </div>
      </form>
    </section>
  );
}
