import { BrandMark } from "@/components/brand-mark";

export default function AuthenticationLayout({ children }: { children: React.ReactNode }) {
  return (
    <main className="min-h-screen lg:grid lg:grid-cols-[minmax(0,1.04fr)_minmax(480px,0.96fr)]">
      <section className="relative hidden min-h-screen overflow-hidden bg-brand-strong px-12 py-10 text-white lg:flex lg:flex-col xl:px-18">
        <div className="absolute -right-36 -top-32 size-[30rem] rounded-full border border-white/10" />
        <div className="absolute -right-14 -top-12 size-72 rounded-full border border-white/10" />
        <div className="absolute -bottom-32 -left-24 size-96 rounded-full bg-white/[0.035]" />
        <div className="relative z-10">
          <BrandMark inverse />
        </div>

        <div className="relative z-10 my-auto max-w-xl pb-12 pt-24">
          <p className="mb-6 text-sm font-semibold uppercase tracking-[0.18em] text-emerald-200">
            Clarity, every week
          </p>
          <h1 className="max-w-lg text-5xl font-semibold leading-[1.08] tracking-[-0.045em] xl:text-6xl">
            Turn weekly work into shared momentum.
          </h1>
          <p className="mt-7 max-w-lg text-lg leading-8 text-emerald-50/75">
            Capture progress, surface blockers, and keep every review in context—without another status meeting.
          </p>
          <div className="mt-12 flex gap-9 border-t border-white/15 pt-8 text-sm text-emerald-50/70">
            <span>Focused reports</span>
            <span>Clear ownership</span>
            <span>Useful insights</span>
          </div>
        </div>

        <p className="relative z-10 text-sm text-emerald-100/55">
          A calm place for teams to reflect and move forward.
        </p>
      </section>

      <section className="flex min-h-screen flex-col bg-stone-50 px-5 py-6 sm:px-10 lg:px-14 xl:px-20">
        <div className="mb-14 lg:hidden">
          <BrandMark />
        </div>
        <div className="flex flex-1 justify-center">
          <div className="my-auto w-full max-w-md animate-soft-in py-8">{children}</div>
        </div>
        <p className="mt-12 text-center text-xs text-slate-400">
          Your access is protected with short-lived sessions.
        </p>
      </section>
    </main>
  );
}
