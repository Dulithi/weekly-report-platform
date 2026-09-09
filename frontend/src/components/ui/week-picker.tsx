"use client";

import { useEffect, useId, useMemo, useRef, useState } from "react";

type WeekPickerProps = {
  label: string;
  value: string;
  onChange(value: string): void;
  disabled?: boolean;
  allowClear?: boolean;
  id?: string;
};

const dayLabels = ["Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"];

export function WeekPicker({ label, value, onChange, disabled = false, allowClear = false, id }: WeekPickerProps) {
  const generatedId = useId();
  const controlId = id ?? `week-${generatedId.replaceAll(":", "")}`;
  const root = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
  const [visibleMonth, setVisibleMonth] = useState(() => monthFor(value));
  const dates = useMemo(() => calendarDates(visibleMonth), [visibleMonth]);

  useEffect(() => {
    function closeOnOutsideClick(event: MouseEvent) {
      if (root.current && !root.current.contains(event.target as Node)) setOpen(false);
    }
    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === "Escape") setOpen(false);
    }
    document.addEventListener("mousedown", closeOnOutsideClick);
    document.addEventListener("keydown", closeOnEscape);
    return () => {
      document.removeEventListener("mousedown", closeOnOutsideClick);
      document.removeEventListener("keydown", closeOnEscape);
    };
  }, []);

  function toggle() {
    if (disabled) return;
    if (!open) setVisibleMonth(monthFor(value));
    setOpen(current => !current);
  }

  function select(date: Date) {
    onChange(toIsoDate(date));
    setOpen(false);
  }

  return (
    <div ref={root} className="relative min-w-0 flex-1 sm:w-52">
      <label id={`${controlId}-label`} htmlFor={controlId} className="block text-sm font-medium text-slate-700">{label}</label>
      <button
        id={controlId}
        type="button"
        className="report-input mt-1 flex min-h-12 items-center justify-between gap-3 text-left"
        aria-labelledby={`${controlId}-label ${controlId}`}
        aria-haspopup="dialog"
        aria-expanded={open}
        disabled={disabled}
        onClick={toggle}
      >
        <span className={value ? "text-slate-900" : "text-slate-400"}>{value ? formatDate(value) : "Choose a Monday"}</span>
        <CalendarIcon />
      </button>

      {open && (
        <div role="dialog" aria-label="Choose a reporting week" className="absolute left-0 z-50 mt-2 w-[min(20rem,calc(100vw-2rem))] rounded-2xl border border-slate-200 bg-white p-4 shadow-[0_18px_50px_rgba(15,23,42,0.18)]">
          <div className="flex items-center justify-between gap-3">
            <button type="button" className="week-calendar-nav" onClick={() => setVisibleMonth(addMonths(visibleMonth, -1))} aria-label="Previous month">←</button>
            <p className="font-semibold text-slate-900" aria-live="polite">{formatMonth(visibleMonth)}</p>
            <button type="button" className="week-calendar-nav" onClick={() => setVisibleMonth(addMonths(visibleMonth, 1))} aria-label="Next month">→</button>
          </div>

          <div className="mt-4 grid grid-cols-7 text-center" aria-hidden="true">
            {dayLabels.map(day => <span key={day} className="pb-2 text-[11px] font-semibold uppercase tracking-wide text-slate-400">{day}</span>)}
          </div>
          <div className="grid grid-cols-7 gap-y-1" role="grid" aria-label={formatMonth(visibleMonth)}>
            {dates.map(date => {
              const iso = toIsoDate(date);
              const monday = date.getUTCDay() === 1;
              const selected = iso === value;
              const selectedWeek = value && iso >= value && iso <= addDaysIso(value, 6);
              const currentMonth = date.getUTCMonth() === visibleMonth.getUTCMonth();
              return monday ? (
                <button
                  key={iso}
                  type="button"
                  role="gridcell"
                  aria-selected={selected}
                  aria-label={`Week starting ${formatDate(iso)}`}
                  onClick={() => select(date)}
                  className={`grid size-9 place-items-center rounded-l-lg text-sm font-semibold outline-none transition focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-1 ${selected ? "bg-brand text-white" : "text-brand-strong hover:bg-brand-soft"} ${currentMonth ? "" : "opacity-45"}`}
                >{date.getUTCDate()}</button>
              ) : (
                <span key={iso} role="gridcell" className={`grid size-9 place-items-center text-sm ${selectedWeek ? "bg-brand-soft/60" : ""} ${currentMonth ? "text-slate-600" : "text-slate-300"}`}>{date.getUTCDate()}</span>
              );
            })}
          </div>

          <div className="mt-4 flex items-center justify-between border-t border-slate-100 pt-3">
            {allowClear ? <button type="button" className="text-sm font-semibold text-slate-500 hover:text-slate-800" onClick={() => { onChange(""); setOpen(false); }}>Clear</button> : <span />}
            <button type="button" className="text-sm font-semibold text-brand hover:text-brand-strong" onClick={() => select(currentMonday())}>Current week</button>
          </div>
          <p className="mt-3 text-xs leading-5 text-slate-400">Only Mondays can start a reporting week.</p>
        </div>
      )}
    </div>
  );
}

function monthFor(value: string) {
  const source = /^\d{4}-\d{2}-\d{2}$/.test(value) ? new Date(`${value}T12:00:00Z`) : new Date();
  return new Date(Date.UTC(source.getUTCFullYear(), source.getUTCMonth(), 1, 12));
}

function calendarDates(month: Date) {
  const mondayOffset = (month.getUTCDay() + 6) % 7;
  const first = new Date(month);
  first.setUTCDate(1 - mondayOffset);
  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(first);
    date.setUTCDate(first.getUTCDate() + index);
    return date;
  });
}

function addMonths(month: Date, amount: number) {
  return new Date(Date.UTC(month.getUTCFullYear(), month.getUTCMonth() + amount, 1, 12));
}

function currentMonday() {
  const today = new Date();
  const date = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate(), 12));
  date.setUTCDate(date.getUTCDate() - ((date.getUTCDay() + 6) % 7));
  return date;
}

function addDaysIso(value: string, days: number) {
  const date = new Date(`${value}T12:00:00Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return toIsoDate(date);
}

function toIsoDate(date: Date) {
  return date.toISOString().slice(0, 10);
}

function formatMonth(date: Date) {
  return new Intl.DateTimeFormat(undefined, { month: "long", year: "numeric", timeZone: "UTC" }).format(date);
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, { weekday: "short", month: "short", day: "numeric", year: "numeric", timeZone: "UTC" })
    .format(new Date(`${value}T12:00:00Z`));
}

function CalendarIcon() {
  return <svg aria-hidden="true" viewBox="0 0 24 24" className="size-5 shrink-0 text-brand" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="M7 3v3m10-3v3M4.5 9h15M6 5h12a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z" /><path d="M8 13h3v3H8z" /></svg>;
}
