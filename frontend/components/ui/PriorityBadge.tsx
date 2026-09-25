import type { TicketPriority } from "@/lib/types";

type PriorityBadgeProps = {
  priority: TicketPriority;
  className?: string;
};

export function PriorityBadge({ priority, className = "" }: PriorityBadgeProps) {
  return (
    <span className={`badge badge-priority badge-priority-${priority} ${className}`.trim()}>
      {priority}
    </span>
  );
}
