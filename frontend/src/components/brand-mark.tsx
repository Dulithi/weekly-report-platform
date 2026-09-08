import Link from "next/link";

export function BrandMark({ inverse = false }: { inverse?: boolean }) {
  return (
    <Link
      href="/"
      className="inline-flex items-center gap-3 rounded-lg focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand"
      aria-label="Weekly home"
    >
      <span
        className={`grid size-10 place-items-center rounded-xl shadow-sm ${
          inverse ? "bg-white text-brand-strong" : "bg-brand text-white"
        }`}
        aria-hidden="true"
      >
        <svg viewBox="0 0 24 24" className="size-5" fill="none">
          <path d="M6 7.5h12M6 12h7M6 16.5h5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
          <path d="m15.5 16 1.7 1.7 3.3-4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </span>
      <span className={`text-xl font-semibold tracking-[-0.035em] ${inverse ? "text-white" : "text-slate-950"}`}>
        Weekly
      </span>
    </Link>
  );
}
