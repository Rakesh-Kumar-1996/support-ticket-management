import { ApiError } from "@/lib/api";
import type { FieldErrorItem } from "@/lib/types";

export type ApiFailureKind =
  | "validation"
  | "notFound"
  | "conflict"
  | "network"
  | "server"
  | "unknown";

export type ResolvedApiFailure = {
  kind: ApiFailureKind;
  message: string;
  fieldErrors: Record<string, string>;
  status?: number;
  code?: string;
};

export function fieldErrorsToMap(
  items: FieldErrorItem[] | undefined
): Record<string, string> {
  const map: Record<string, string> = {};
  for (const item of items ?? []) {
    map[item.field] = item.message;
  }
  return map;
}

export function resolveApiFailure(error: unknown): ResolvedApiFailure {
  if (error instanceof ApiError) {
    const { body } = error;
    let kind: ApiFailureKind = "unknown";
    if (body.status === 400 || body.code === "VALIDATION_ERROR") {
      kind = "validation";
    } else if (body.status === 404 || body.code === "TICKET_NOT_FOUND") {
      kind = "notFound";
    } else if (body.status === 409 || body.code === "INVALID_STATUS_TRANSITION") {
      kind = "conflict";
    } else if (body.status >= 500) {
      kind = "server";
    }
    return {
      kind,
      message: body.message,
      fieldErrors: fieldErrorsToMap(body.fieldErrors),
      status: body.status,
      code: body.code,
    };
  }

  if (error instanceof TypeError) {
    return {
      kind: "network",
      message:
        "Could not reach the server. Check that the API is running and try again.",
      fieldErrors: {},
    };
  }

  if (error instanceof Error) {
    return {
      kind: "unknown",
      message: error.message,
      fieldErrors: {},
    };
  }

  return {
    kind: "unknown",
    message: "Something went wrong. Please try again.",
    fieldErrors: {},
  };
}

export function failureHeading(kind: ApiFailureKind): string {
  switch (kind) {
    case "validation":
      return "Validation error";
    case "notFound":
      return "Not found";
    case "conflict":
      return "Action not allowed";
    case "network":
      return "Connection problem";
    case "server":
      return "Server error";
    default:
      return "Error";
  }
}
