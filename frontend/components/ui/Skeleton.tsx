type SkeletonProps = {
  className?: string;
};

export function Skeleton({ className = "" }: SkeletonProps) {
  return <div className={`skeleton ${className}`.trim()} aria-hidden="true" />;
}

export function TicketListSkeleton() {
  return (
    <div className="ticket-table-card card" aria-busy="true" aria-label="Loading tickets">
      <div className="skeleton-table">
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="skeleton-row">
            <Skeleton className="skeleton-line skeleton-line-lg" />
            <Skeleton className="skeleton-pill" />
            <Skeleton className="skeleton-pill" />
            <Skeleton className="skeleton-line skeleton-line-sm" />
          </div>
        ))}
      </div>
    </div>
  );
}

export function TicketDetailSkeleton() {
  return (
    <div className="detail-skeleton stack" aria-busy="true" aria-label="Loading ticket">
      <Skeleton className="skeleton-line skeleton-line-xl" />
      <div className="detail-skeleton-grid">
        <Skeleton className="skeleton-block" />
        <Skeleton className="skeleton-block skeleton-block-short" />
      </div>
    </div>
  );
}
