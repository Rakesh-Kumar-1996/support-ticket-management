import type { TicketStatus } from "@/lib/types";

/**
 * Allowed status edges (mirrors backend state machine for UI affordances only).
 * Backend remains authoritative for validation.
 */
const ALLOWED_TARGETS: Record<TicketStatus, TicketStatus[]> = {
  OPEN: ["IN_PROGRESS", "CANCELLED"],
  IN_PROGRESS: ["RESOLVED", "CANCELLED"],
  RESOLVED: ["CLOSED"],
  CLOSED: [],
  CANCELLED: [],
};

export function allowedTargetStatuses(from: TicketStatus): TicketStatus[] {
  return ALLOWED_TARGETS[from] ?? [];
}

export function formatStatusLabel(status: TicketStatus): string {
  return status.replaceAll("_", " ");
}

const TRANSITION_ACTION_LABELS: Partial<Record<TicketStatus, Partial<Record<TicketStatus, string>>>> = {
  OPEN: {
    IN_PROGRESS: "Start progress",
    CANCELLED: "Cancel ticket",
  },
  IN_PROGRESS: {
    RESOLVED: "Resolve",
    CANCELLED: "Cancel ticket",
  },
  RESOLVED: {
    CLOSED: "Close ticket",
  },
};

export function isDestructiveTransition(target: TicketStatus): boolean {
  return target === "CANCELLED";
}

export function transitionActionLabel(from: TicketStatus, to: TicketStatus): string {
  return (
    TRANSITION_ACTION_LABELS[from]?.[to] ?? `Move to ${formatStatusLabel(to)}`
  );
}
