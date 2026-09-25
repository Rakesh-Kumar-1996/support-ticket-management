"use client";

import { useState } from "react";
import { addComment, getTicket } from "@/lib/api";
import { resolveApiFailure } from "@/lib/api-error";
import { ApiErrorBanner } from "@/components/ApiErrorBanner";
import type { TicketDetail } from "@/lib/types";

type TicketCommentFormProps = {
  ticketId: string;
  onUpdated: (ticket: TicketDetail) => void;
};

export function TicketCommentForm({ ticketId, onUpdated }: TicketCommentFormProps) {
  const [body, setBody] = useState("");
  const [author, setAuthor] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [failure, setFailure] = useState<ReturnType<typeof resolveApiFailure> | null>(
    null
  );
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setFailure(null);
    setFieldErrors({});

    if (!body.trim()) {
      setFieldErrors({ body: "Comment body is required." });
      return;
    }

    setSubmitting(true);
    try {
      await addComment(ticketId, {
        body,
        author: author.length ? author : undefined,
      });
      const refreshed = await getTicket(ticketId);
      onUpdated(refreshed);
      setBody("");
    } catch (err) {
      const resolved = resolveApiFailure(err);
      setFailure(resolved);
      setFieldErrors(resolved.fieldErrors);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="comment-compose">
      <h3 className="comment-compose-title">Add a comment</h3>
      {failure && <ApiErrorBanner failure={failure} />}

      <form className="stack" onSubmit={handleSubmit}>
        <label className="field">
          <span>Author</span>
          <input
            type="text"
            value={author}
            onChange={(e) => setAuthor(e.target.value)}
            placeholder="Optional display name"
            maxLength={200}
          />
          {fieldErrors.author && (
            <span className="field-error">{fieldErrors.author}</span>
          )}
        </label>

        <label className="field">
          <span>Comment <span className="required" aria-hidden="true">*</span></span>
          <textarea
            rows={4}
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder="Add an update for the team"
            maxLength={5000}
            required
          />
          {fieldErrors.body && <span className="field-error">{fieldErrors.body}</span>}
        </label>

        <div className="form-actions form-actions-end">
          <button type="submit" className="button button-primary" disabled={submitting}>
            {submitting ? "Posting…" : "Add comment"}
          </button>
        </div>
      </form>
    </div>
  );
}
