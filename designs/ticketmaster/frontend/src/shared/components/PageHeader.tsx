interface PageHeaderProps {
  eyebrow: string;
  title: string;
  description: string;
}

export function PageHeader({ eyebrow, title, description }: PageHeaderProps) {
  return (
    <header className="page-header">
      <p className="eyebrow">{eyebrow}</p>
      <h2>{title}</h2>
      <p className="page-copy">{description}</p>
    </header>
  );
}
