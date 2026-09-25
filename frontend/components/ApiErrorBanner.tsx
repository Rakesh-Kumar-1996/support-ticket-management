import {
  failureHeading,
  type ApiFailureKind,
  type ResolvedApiFailure,
} from "@/lib/api-error";

type ApiErrorBannerProps = {
  failure: ResolvedApiFailure;
  className?: string;
};

export function ApiErrorBanner({ failure, className = "" }: ApiErrorBannerProps) {
  const variant = bannerVariant(failure.kind);
  return (
    <div className={`alert ${variant} ${className}`.trim()} role="alert">
      <strong>{failureHeading(failure.kind)}</strong>
      <p>{failure.message}</p>
      {Object.keys(failure.fieldErrors).length > 0 && (
        <ul className="field-error-list">
          {Object.entries(failure.fieldErrors).map(([field, message]) => (
            <li key={field}>
              <span className="field-error-label">{field}:</span> {message}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function bannerVariant(kind: ApiFailureKind): string {
  if (kind === "conflict") {
    return "alert-warning";
  }
  return "alert-error";
}
