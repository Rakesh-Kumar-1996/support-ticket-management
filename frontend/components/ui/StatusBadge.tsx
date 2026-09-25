import type { TicketStatus } from "@/lib/types";
import { formatStatusLabel } from "@/lib/transitions";

type StatusBadgeProps = {
  status: TicketStatus;
  className?: string;
};

export function StatusBadge({ status, className = "" }: StatusBadgeProps) {
  return (
    <span className={`badge badge-status badge-status-${status} ${className}`.trim()}>
      {formatStatusLabel(status)}
    </span>
  );
}
