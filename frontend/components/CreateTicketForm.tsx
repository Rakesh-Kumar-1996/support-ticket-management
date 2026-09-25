"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { createTicket } from "@/lib/api";
import { resolveApiFailure } from "@/lib/api-error";
import { ApiErrorBanner } from "@/components/ApiErrorBanner";
import { BackLink } from "@/components/ui/BackLink";
import { FormPageHeader } from "@/components/ui/FormPageHeader";
import type { TicketPriority } from "@/lib/types";
import { TICKET_PRIORITIES } from "@/lib/types";

export function CreateTicketForm() {
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<TicketPriority>("MEDIUM");
  const [assignee, setAssignee] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [failure, setFailure] = useState<ReturnType<typeof resolveApiFailure> | null>(
    null
  );
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

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
      const created = await createTicket({
        title: trimmedTitle,
        description: description.length ? description : undefined,
        priority,
        assignee: assignee.length ? assignee : undefined,
      });
      router.push(`/tickets/${created.id}`);
    } catch (err) {
      const resolved = resolveApiFailure(err);
      setFailure(resolved);
      setFieldErrors(resolved.fieldErrors);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="form-page stack">
      <BackLink href="/">Back to tickets</BackLink>
      <FormPageHeader
        title="Create new ticket"
        description="Create a support request for your team. Status and timestamps are set automatically."
      />

      <form className="card form-card stack" onSubmit={handleSubmit}>
        {failure && <ApiErrorBanner failure={failure} />}

        <label className="field">
          <span>Title <span className="required" aria-hidden="true">*</span></span>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Brief summary of the issue"
            maxLength={200}
            required
            aria-invalid={fieldErrors.title ? true : undefined}
          />
          <span className="field-hint">Required. Max 200 characters after trim.</span>
          {fieldErrors.title && <span className="field-error">{fieldErrors.title}</span>}
        </label>

        <label className="field">
          <span>Description</span>
          <textarea
            rows={5}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Steps to reproduce, context, or customer impact"
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
          {fieldErrors.priority && <span className="field-error">{fieldErrors.priority}</span>}
        </label>

        <label className="field">
          <span>Assignee</span>
          <input
            type="text"
            value={assignee}
            onChange={(e) => setAssignee(e.target.value)}
            placeholder="Name or team (optional)"
            maxLength={200}
          />
          {fieldErrors.assignee && <span className="field-error">{fieldErrors.assignee}</span>}
        </label>

        <div className="form-actions form-actions-split">
          <Link href="/" className="button button-secondary">
            Cancel
          </Link>
          <button type="submit" className="button button-primary" disabled={submitting}>
            {submitting ? "Creating…" : "Create ticket"}
          </button>
        </div>
      </form>
    </div>
  );
}
