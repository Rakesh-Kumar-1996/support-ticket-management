import Link from "next/link";
import type { ReactNode } from "react";

type BackLinkProps = {
  href: string;
  children: ReactNode;
};

export function BackLink({ href, children }: BackLinkProps) {
  return (
    <Link href={href} className="back-link">
      <span aria-hidden="true">←</span>
      {children}
    </Link>
  );
}
