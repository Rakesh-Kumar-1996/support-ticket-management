import { describe, expect, it } from "vitest";
import {
  allowedTargetStatuses,
  isDestructiveTransition,
  transitionActionLabel,
} from "@/lib/transitions";

describe("allowedTargetStatuses", () => {
  it("exposes the five valid edges from the state machine", () => {
    expect(allowedTargetStatuses("OPEN")).toEqual(["IN_PROGRESS", "CANCELLED"]);
    expect(allowedTargetStatuses("IN_PROGRESS")).toEqual(["RESOLVED", "CANCELLED"]);
    expect(allowedTargetStatuses("RESOLVED")).toEqual(["CLOSED"]);
  });

  it("returns no targets for terminal statuses", () => {
    expect(allowedTargetStatuses("CLOSED")).toEqual([]);
    expect(allowedTargetStatuses("CANCELLED")).toEqual([]);
  });
});

describe("transitionActionLabel", () => {
  it("uses human-readable action labels for valid edges", () => {
    expect(transitionActionLabel("OPEN", "IN_PROGRESS")).toBe("Start progress");
    expect(transitionActionLabel("RESOLVED", "CLOSED")).toBe("Close ticket");
  });
});

describe("isDestructiveTransition", () => {
  it("marks cancel transitions as destructive styling", () => {
    expect(isDestructiveTransition("CANCELLED")).toBe(true);
    expect(isDestructiveTransition("IN_PROGRESS")).toBe(false);
  });
});
