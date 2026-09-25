export function formatDateTime(iso: string): string {
  try {
    return new Date(iso).toLocaleString(undefined, {
      dateStyle: "medium",
      timeStyle: "short",
    });
  } catch {
    return iso;
  }
}

export function displayOptional(value: string | null | undefined): string {
  if (value == null || value === "") {
    return "—";
  }
  return value;
}
