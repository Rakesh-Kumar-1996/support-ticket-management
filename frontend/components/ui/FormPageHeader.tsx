type FormPageHeaderProps = {
  title: string;
  description: string;
};

export function FormPageHeader({ title, description }: FormPageHeaderProps) {
  return (
    <header className="form-page-header">
      <h1 className="page-title">{title}</h1>
      <p className="page-description">{description}</p>
    </header>
  );
}
