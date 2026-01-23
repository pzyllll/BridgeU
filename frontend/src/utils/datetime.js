/**
 * Normalize backend time values into a JS Date.
 *
 * Supports:
 * - ISO strings (with Z / offset)
 * - SQL-ish strings: "YYYY-MM-DD HH:mm:ss[.ffffff]"
 * - epoch millis/seconds (number or numeric string)
 * - Jackson Instant object shapes: { epochSecond, nano } or { epochSecond, nanos }
 */
export function parseBackendDate(input) {
  if (!input) return null;

  if (input instanceof Date) {
    return Number.isFinite(input.getTime()) ? input : null;
  }

  if (typeof input === 'number') {
    // Heuristic: detect epoch unit by magnitude
    // - seconds: ~1e9 (2026)
    // - millis: ~1e12
    // - micros: ~1e15
    // - nanos:  ~1e18
    let ms = input;
    const abs = Math.abs(input);
    if (abs < 1e11) {
      ms = input * 1000; // seconds -> ms
    } else if (abs >= 1e14 && abs < 1e17) {
      ms = input / 1000; // microseconds -> ms
    } else if (abs >= 1e17) {
      ms = input / 1e6; // nanoseconds -> ms
    }
    const d = new Date(ms);
    return Number.isFinite(d.getTime()) ? d : null;
  }

  // Handle common Jackson Instant object shapes
  if (typeof input === 'object') {
    const epochSecond = input.epochSecond ?? input.seconds ?? input.epochSeconds;
    const nano = input.nano ?? input.nanos ?? input.nanoSeconds ?? input.nanoseconds;
    const es =
      typeof epochSecond === 'string' && epochSecond.trim() !== '' ? Number(epochSecond) : epochSecond;
    const ns = typeof nano === 'string' && nano.trim() !== '' ? Number(nano) : nano;
    if (typeof es === 'number' && Number.isFinite(es)) {
      const ms = es * 1000 + (typeof ns === 'number' && Number.isFinite(ns) ? ns / 1e6 : 0);
      const d = new Date(ms);
      return Number.isFinite(d.getTime()) ? d : null;
    }
  }

  if (typeof input !== 'string') return null;

  let s = input.trim();
  if (!s) return null;

  // Numeric epoch (seconds or millis)
  if (/^\d+$/.test(s)) {
    const n = Number(s);
    if (!Number.isFinite(n)) return null;
    const ms = s.length <= 10 ? n * 1000 : n;
    const d = new Date(ms);
    return Number.isFinite(d.getTime()) ? d : null;
  }

  // Convert "YYYY-MM-DD HH:mm:ss(.ffffff)" => "YYYY-MM-DDTHH:mm:ss(.fff)Z"
  // (We treat missing timezone as UTC because backend uses Instant.)
  const looksLikeSqlTimestamp =
    /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}(\.\d+)?$/.test(s);
  if (looksLikeSqlTimestamp) {
    s = s.replace(' ', 'T');
    // truncate fractional seconds to millis
    s = s.replace(/\.(\d{3})\d+$/, '.$1');
    s = `${s}Z`;
  } else {
    // If it's ISO but has >3 fractional digits, truncate to millis
    s = s.replace(/\.(\d{3})\d+(Z|[+-]\d{2}:\d{2})$/, '.$1$2');
    // If it's ISO-like without timezone, treat as UTC for consistency
    const hasTz = /Z$|[+-]\d{2}:\d{2}$/.test(s);
    const isoNoTz =
      /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?$/.test(s);
    if (!hasTz && isoNoTz) {
      s = `${s}Z`;
    }
  }

  const d = new Date(s);
  return Number.isFinite(d.getTime()) ? d : null;
}

export function formatBangkokAbsolute(date) {
  const d = date instanceof Date ? date : parseBackendDate(date);
  if (!d) return null;

  const fmt = new Intl.DateTimeFormat('sv-SE', {
    timeZone: 'Asia/Bangkok',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });
  // "sv-SE" yields "YYYY-MM-DD HH:mm"
  return fmt.format(d);
}

export function formatRelativeFromNow(date) {
  const d = date instanceof Date ? date : parseBackendDate(date);
  if (!d) return null;
  const diffMs = Date.now() - d.getTime();
  if (!Number.isFinite(diffMs)) return null;
  return diffMs;
}


