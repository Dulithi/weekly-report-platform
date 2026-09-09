"use client";

import Link from "next/link";
import { useEffect, useRef, useState, type FormEvent } from "react";

import { useAuth } from "@/components/auth/auth-provider";
import { WeekPicker } from "@/components/ui/week-picker";
import { formatDateRange } from "@/components/reports/report-history";
import { ApiError } from "@/lib/api/client";
import { isMonday, type AssistantResponse, type AssistantSource } from "@/lib/reports/model";

type ChatRole = "USER" | "ASSISTANT";
interface ChatMessage {
  id: string;
  role: ChatRole;
  content: string;
  result?: AssistantResponse;
}

const suggestions = [
  "Summarize the key blockers and who reported them.",
  "What were the main achievements across the team?",
  "Which projects received the most reported time?",
  "Where does submitted report data suggest delivery risk?",
];

export function ReportAssistant() {
  const { user, request } = useAuth();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [question, setQuestion] = useState("");
  const [weekStart, setWeekStart] = useState("");
  const [weekCount, setWeekCount] = useState(4);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const conversationEnd = useRef<HTMLDivElement>(null);
  const allowed = user?.role === "MANAGER" || user?.role === "ADMIN";

  useEffect(() => {
    conversationEnd.current?.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }, [messages, submitting]);

  if (!allowed) return <AccessDenied />;

  async function ask(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const trimmed = question.trim();
    if (!trimmed) {
      setError("Enter a question about submitted weekly reports.");
      return;
    }
    if (trimmed.length > 500) {
      setError("Keep the question to 500 characters or fewer.");
      return;
    }
    if (weekStart && !isMonday(weekStart)) {
      setError("Choose a Monday so the date matches a reporting week.");
      return;
    }

    setSubmitting(true);
    setError("");
    try {
      const result = await request<AssistantResponse>("/manager/assistant-responses", {
        method: "POST",
        body: JSON.stringify({
          question: trimmed,
          weekStart: weekStart || null,
          weekCount,
          history: boundedHistory(messages),
        }),
      });
      setMessages(current => [
        ...current,
        { id: crypto.randomUUID(), role: "USER", content: trimmed },
        { id: crypto.randomUUID(), role: "ASSISTANT", content: result.answer, result },
      ]);
      setWeekStart(result.throughWeek);
      setQuestion("");
    } catch (caught) {
      setError(assistantError(caught));
    } finally {
      setSubmitting(false);
    }
  }

  return <section className="mx-auto max-w-6xl animate-soft-in">
    <header className="flex flex-col gap-5 border-b border-slate-200 pb-7 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <p className="text-sm font-semibold text-brand">Manager workspace</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-[-0.04em] text-slate-950 sm:text-4xl">Report assistant</h1>
        <p className="mt-3 max-w-2xl leading-7 text-slate-500">Ask questions grounded in the team&apos;s submitted weekly reports. Each report-based claim should link back to its source.</p>
      </div>
      {messages.length > 0 && <button type="button" className="report-secondary self-start lg:self-auto" onClick={() => { setMessages([]); setError(""); }}>Clear conversation</button>}
    </header>

    <div className="mt-6 grid gap-6 xl:grid-cols-[minmax(0,1fr)_300px]">
      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-[0_1px_2px_rgba(15,23,42,0.03)]">
        <div className="min-h-[420px] p-4 sm:p-6" aria-live="polite" aria-label="Assistant conversation">
          {messages.length === 0 ? <EmptyConversation onChoose={setQuestion} /> : <div className="space-y-5">
            {messages.map(message => <Message key={message.id} message={message} />)}
          </div>}
          {submitting && <div role="status" className="mt-5 flex max-w-xl items-center gap-3 rounded-2xl rounded-bl-md bg-slate-100 px-4 py-3 text-sm text-slate-500"><span className="flex gap-1" aria-hidden="true"><i className="size-1.5 animate-pulse rounded-full bg-brand" /><i className="size-1.5 animate-pulse rounded-full bg-brand [animation-delay:120ms]" /><i className="size-1.5 animate-pulse rounded-full bg-brand [animation-delay:240ms]" /></span>Reviewing submitted reports…</div>}
          <div ref={conversationEnd} />
        </div>

        <form onSubmit={ask} className="border-t border-slate-200 bg-slate-50/70 p-4 sm:p-5">
          <label htmlFor="assistant-question" className="text-sm font-semibold text-slate-800">Question</label>
          <textarea id="assistant-question" className="report-textarea mt-2 min-h-24" maxLength={500} value={question} disabled={submitting} onChange={event => { setQuestion(event.target.value); setError(""); }} placeholder="Ask about blockers, achievements, workload or delivery risks…" />
          <div className="mt-2 flex items-start justify-between gap-4 text-xs text-slate-400"><span>Answers can be inaccurate. Check the linked reports.</span><span>{question.length}/500</span></div>
          {error && <p role="alert" className="mt-3 rounded-xl border border-red-200 bg-red-50 p-3 text-sm leading-6 text-red-700">{error}</p>}
          <div className="mt-4 flex justify-end"><button type="submit" className="report-primary w-full sm:w-auto" disabled={submitting || !question.trim()}>{submitting ? "Reviewing reports…" : "Ask assistant"}</button></div>
        </form>
      </div>

      <aside className="space-y-5">
        <div className="rounded-2xl border border-slate-200 bg-white p-5">
          <h2 className="font-semibold text-slate-900">Reporting period</h2>
          <div className="mt-4"><WeekPicker label="Through week" value={weekStart} disabled={submitting} allowClear id="assistant-week" onChange={value => { setWeekStart(value); setError(""); }} /></div>
          <p className="mt-1.5 text-xs leading-5 text-slate-400">Choose a Monday. Leave blank to use the current reporting week.</p>
          <label className="mt-4 block text-sm font-medium text-slate-700">Look back
            <select className="report-select mt-1" value={weekCount} disabled={submitting} onChange={event => setWeekCount(Number(event.target.value))}>
              <option value={1}>1 week</option><option value={4}>4 weeks</option><option value={8}>8 weeks</option><option value={12}>12 weeks</option>
            </select>
          </label>
          {messages.length > 0 && <p className="mt-4 rounded-xl bg-brand-soft px-3 py-2.5 text-xs leading-5 text-brand-strong">Changing the period applies to the next question. Clear the conversation when starting a different analysis.</p>}
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white p-5">
          <h2 className="font-semibold text-slate-900">What is included</h2>
          <ul className="mt-3 space-y-2 text-sm leading-6 text-slate-500"><li>Submitted report versions only</li><li>Server-computed workload totals</li><li>At most 40 reports over 12 weeks</li><li>No private drafts, email or security data</li></ul>
          <p className="mt-4 border-t border-slate-100 pt-4 text-xs leading-5 text-slate-400">Conversation history stays in this browser tab and is cleared on refresh. A configured provider processes each question and bounded context.</p>
        </div>
      </aside>
    </div>
  </section>;
}

function EmptyConversation({ onChoose }: { onChoose(value: string): void }) {
  return <div className="mx-auto grid min-h-[370px] max-w-2xl content-center text-center">
    <span className="mx-auto grid size-12 place-items-center rounded-2xl bg-brand-soft text-brand" aria-hidden="true"><SparkIcon /></span>
    <h2 className="mt-5 text-xl font-semibold text-slate-900">Start with a report question</h2>
    <p className="mx-auto mt-2 max-w-lg text-sm leading-6 text-slate-500">The backend selects authorized submitted data before the model is called and returns links only for verified report sources.</p>
    <div className="mt-6 grid gap-2 text-left sm:grid-cols-2">{suggestions.map(suggestion => <button key={suggestion} type="button" onClick={() => onChoose(suggestion)} className="rounded-xl border border-slate-200 bg-white p-3 text-left text-sm leading-5 text-slate-600 transition hover:border-brand/30 hover:bg-brand-soft/40 hover:text-brand-strong focus-visible:outline-2 focus-visible:outline-brand">{suggestion}</button>)}</div>
  </div>;
}

function Message({ message }: { message: ChatMessage }) {
  if (message.role === "USER") return <div className="ml-auto max-w-2xl rounded-2xl rounded-br-md bg-brand px-4 py-3 text-sm leading-6 text-white">{message.content}</div>;
  return <article className="max-w-3xl rounded-2xl rounded-bl-md border border-slate-200 bg-slate-50 px-4 py-4 sm:px-5">
    <p className="whitespace-pre-wrap text-sm leading-7 text-slate-700">{message.content}</p>
    {message.result && <div className="mt-4 border-t border-slate-200 pt-3">
      <p className="text-xs text-slate-400">Reviewed {message.result.reportsConsidered} submitted {message.result.reportsConsidered === 1 ? "report" : "reports"} · {formatDateRange(message.result.fromWeek, endOfWeek(message.result.throughWeek))}</p>
      {message.result.sources.length > 0 && <div className="mt-3 flex flex-wrap gap-2">{message.result.sources.map(source => <SourceLink key={source.sourceKey} source={source} />)}</div>}
    </div>}
  </article>;
}

function SourceLink({ source }: { source: AssistantSource }) {
  if (!/^\/manager\/reports\/[0-9a-f-]{36}$/i.test(source.href)) return null;
  return <Link href={source.href} className="rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-xs font-semibold text-brand hover:border-brand/30 hover:bg-brand-soft/50">{source.sourceKey} · {source.memberName} · {source.weekStart} · v{source.versionNumber}</Link>;
}

function boundedHistory(messages: ChatMessage[]) {
  const selected: Array<{ role: ChatRole; content: string }> = [];
  let characters = 0;
  for (let index = messages.length - 1; index >= 0 && selected.length < 6; index -= 1) {
    const message = messages[index];
    const content = message.content.trim().slice(0, 1000);
    const available = 2000 - characters;
    if (available <= 0) break;
    selected.push({ role: message.role, content: content.slice(0, available) });
    characters += Math.min(content.length, available);
  }
  return selected.reverse();
}

function assistantError(caught: unknown) {
  if (caught instanceof ApiError) {
    if (caught.status === 503) return "The report assistant is not configured or is temporarily unavailable. The rest of the platform is still available.";
    if (caught.status === 429) return `The assistant request limit was reached. Try again${caught.retryAfterSeconds ? ` in ${caught.retryAfterSeconds} seconds` : " shortly"}.`;
    if (caught.status === 502) return "The provider returned an unusable answer. No unverified response was shown; please try again.";
  }
  return caught instanceof Error ? caught.message : "The assistant request could not be completed.";
}

function endOfWeek(monday: string) { const date = new Date(`${monday}T12:00:00Z`); date.setUTCDate(date.getUTCDate() + 6); return date.toISOString().slice(0, 10); }
function SparkIcon() { return <svg viewBox="0 0 24 24" className="size-6" fill="none"><path d="m12 3 1.3 4.2L17.5 8.5l-4.2 1.3L12 14l-1.3-4.2-4.2-1.3 4.2-1.3L12 3ZM18.5 14l.7 2.3 2.3.7-2.3.7-.7 2.3-.7-2.3-2.3-.7 2.3-.7.7-2.3Z" fill="currentColor" /></svg>; }
function AccessDenied() { return <div className="mx-auto max-w-xl rounded-2xl border border-amber-200 bg-amber-50 p-6"><h1 className="font-semibold text-amber-950">Manager access required</h1><p className="mt-2 text-sm text-amber-800">The report assistant is available to managers and administrators.</p><Link href="/dashboard" className="mt-4 inline-block font-semibold text-brand">Return to dashboard</Link></div>; }
