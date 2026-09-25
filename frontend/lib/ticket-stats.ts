import type { TicketSummaryResponse } from "@/lib/types";

export interface TicketStats {
  total: number;
  OPEN: number;
  IN_PROGRESS: number;
  RESOLVED: number;
  CLOSED: number;
  CANCELLED: number;
}

export function mapSummaryToStats(summary: TicketSummaryResponse): TicketStats {
  return {
    total: summary.total,
    OPEN: summary.open,
    IN_PROGRESS: summary.inProgress,
    RESOLVED: summary.resolved,
    CLOSED: summary.closed,
    CANCELLED: summary.cancelled,
  };
}

export const EMPTY_STATS: TicketStats = {
  total: 0,
  OPEN: 0,
  IN_PROGRESS: 0,
  RESOLVED: 0,
  CLOSED: 0,
  CANCELLED: 0,
};
