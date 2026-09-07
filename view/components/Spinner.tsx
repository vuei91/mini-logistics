export function Spinner({ label = "불러오는 중..." }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-3 py-12 text-sm text-zinc-500">
      <span
        className="inline-block h-5 w-5 animate-spin rounded-full border-2 border-zinc-300 border-t-[var(--cj-red)]"
        aria-hidden
      />
      <span>{label}</span>
    </div>
  );
}
