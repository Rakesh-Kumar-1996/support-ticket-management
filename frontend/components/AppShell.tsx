"use client";

import type { ReactNode } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";

type AppShellProps = {
  children: ReactNode;
};

export function AppShell({ children }: AppShellProps) {
  const pathname = usePathname();

  const isTickets = pathname === "/";
  const isNew = pathname === "/tickets/new";
  const isDetail = pathname.startsWith("/tickets/") && !isNew;

  let breadcrumbLeaf = "Tickets";
  if (isNew) {
    breadcrumbLeaf = "New ticket";
  } else if (isDetail) {
    breadcrumbLeaf = "Ticket detail";
  }

  return (
    <div className="app-shell">
      <a href="#main-content" className="skip-link">Skip to content</a>

      <aside id="app-sidebar" className="app-sidebar" aria-label="Main navigation">
        <div className="sidebar-brand">
          <span className="sidebar-logo" aria-hidden="true">ST</span>
          <div>
            <p className="sidebar-product">Support Tickets</p>
            <p className="sidebar-tagline">Ticket management</p>
          </div>
        </div>

        <nav className="sidebar-nav">
          <Link href="/" className={`sidebar-link ${isTickets ? "is-active" : ""}`}>
            Tickets
          </Link>
          <Link
            href="/tickets/new"
            className={`sidebar-link sidebar-link-cta ${isNew ? "is-active" : ""}`}
          >
            New ticket
          </Link>
        </nav>
      </aside>

      <div className="app-frame">
        <header className="topbar">
          <nav className="topbar-breadcrumb" aria-label="Breadcrumb">
            <Link href="/" className="topbar-breadcrumb-root">Support Tickets</Link>
            <span className="topbar-breadcrumb-sep" aria-hidden="true">/</span>
            <span className="topbar-breadcrumb-current">{breadcrumbLeaf}</span>
          </nav>
          <Link href="/tickets/new" className="button button-primary topbar-cta">
            New ticket
          </Link>
        </header>

        <main id="main-content" className="app-main">
          {children}
        </main>
      </div>
    </div>
  );
}
