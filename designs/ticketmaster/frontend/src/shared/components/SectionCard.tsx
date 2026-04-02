import type { PropsWithChildren, ReactNode } from "react";

interface SectionCardProps extends PropsWithChildren {
  title: string;
  description?: string;
  actions?: ReactNode;
}

export function SectionCard({ title, description, actions, children }: SectionCardProps) {
  return (
    <section className="section-card">
      <div className="section-card__header">
        <div>
          <h3>{title}</h3>
          {description ? <p>{description}</p> : null}
        </div>
        {actions ? <div className="section-card__actions">{actions}</div> : null}
      </div>
      {children}
    </section>
  );
}
