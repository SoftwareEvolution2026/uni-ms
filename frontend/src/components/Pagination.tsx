interface PaginationProps {
  page: number;
  totalPages: number;
  totalElements: number;
  size: number;
  onPage: (page: number) => void;
}

export function Pagination({ page, totalPages, totalElements, size, onPage }: PaginationProps) {
  if (!totalElements) return null;
  const from = page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);
  return <div className="pagination"><span>Showing {from}–{to} of {totalElements}</span><div><button className="button secondary compact" disabled={page === 0} onClick={() => onPage(page - 1)}>← Previous</button><span>Page {page + 1} of {Math.max(totalPages, 1)}</span><button className="button secondary compact" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>Next →</button></div></div>;
}
