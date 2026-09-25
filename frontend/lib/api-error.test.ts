import { describe, expect, it } from "vitest";
import { ApiError } from "@/lib/api";
import { fieldErrorsToMap, resolveApiFailure } from "@/lib/api-error";

describe("resolveApiFailure", () => {
  it("maps validation errors with fieldErrors", () => {
    const err = new ApiError({
      status: 400,
      error: "BAD_REQUEST",
      code: "VALIDATION_ERROR",
      message: "Title must not be blank.",
      path: "/api/tickets",
      fieldErrors: [{ field: "title", message: "Title must not be blank." }],
    });
    const resolved = resolveApiFailure(err);
    expect(resolved.kind).toBe("validation");
    expect(resolved.message).toBe("Title must not be blank.");
    expect(resolved.fieldErrors.title).toBe("Title must not be blank.");
  });

  it("maps 404 ticket not found", () => {
    const err = new ApiError({
      status: 404,
      error: "NOT_FOUND",
      code: "TICKET_NOT_FOUND",
      message: "Ticket was not found.",
      path: "/api/tickets/x",
      fieldErrors: [],
    });
    expect(resolveApiFailure(err).kind).toBe("notFound");
  });

  it("maps 409 invalid transition", () => {
    const err = new ApiError({
      status: 409,
      error: "CONFLICT",
      code: "INVALID_STATUS_TRANSITION",
      message: "Transition from CLOSED to OPEN is not allowed.",
      path: "/api/tickets/x/status",
      fieldErrors: [],
    });
    const resolved = resolveApiFailure(err);
    expect(resolved.kind).toBe("conflict");
    expect(resolved.message).toContain("CLOSED");
  });

  it("maps network failures", () => {
    const resolved = resolveApiFailure(new TypeError("Failed to fetch"));
    expect(resolved.kind).toBe("network");
    expect(resolved.message).toContain("Could not reach the server");
  });
});

describe("fieldErrorsToMap", () => {
  it("builds a field map", () => {
    expect(
      fieldErrorsToMap([
        { field: "body", message: "Body must not be blank." },
      ])
    ).toEqual({ body: "Body must not be blank." });
  });
});
