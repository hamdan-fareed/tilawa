import type { QuranVerse, VerseCandidate } from "./types";

const DIACRITICS_RE = /[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06DE\u06DF-\u06ED\u0640]/g;

const NORM_MAP: Record<string, string> = {
  "\u0623": "\u0627", // أ -> ا
  "\u0625": "\u0627", // إ -> ا
  "\u0622": "\u0627", // آ -> ا
  "\u0671": "\u0627", // ٱ -> ا
  "\u0629": "\u0647", // ة -> ه
  "\u0649": "\u064A", // ى -> ي
};

export function normalizeArabic(text: string): string {
  if (!text) return "";
  let clean = text.replace(/\ufeff/g, "");
  clean = clean.replace(DIACRITICS_RE, "");
  clean = clean.replace(/./g, (ch) => NORM_MAP[ch] ?? ch);
  clean = clean.split(/\s+/).filter(Boolean).join(" ");
  return clean.trim();
}

export function splitWords(text: string): string[] {
  return text.split(/\s+/).filter((w) => w.length > 0);
}

export class QuranDB {
  private verses: QuranVerse[] = [];
  private versesMap = new Map<string, QuranVerse>();
  private normalizedIndex: { surah: number; ayah: number; clean: string; words: string[] }[] = [];
  private isLoaded = false;

  async load(): Promise<void> {
    if (this.isLoaded) return;
    try {
      const res = await fetch("/quran.json");
      if (!res.ok) throw new Error(`HTTP error ${res.status}`);
      this.verses = await res.json();
      for (const v of this.verses) {
        const key = `${v.surah}:${v.ayah}`;
        this.versesMap.set(key, v);
        const norm = normalizeArabic(v.text_clean || v.text_uthmani);
        this.normalizedIndex.push({
          surah: v.surah,
          ayah: v.ayah,
          clean: norm,
          words: splitWords(norm),
        });
      }
      this.isLoaded = true;
    } catch (err) {
      console.error("Failed to load Quran database:", err);
      throw err;
    }
  }

  getVerse(surah: number, ayah: number): QuranVerse | undefined {
    return this.versesMap.get(`${surah}:${ayah}`);
  }

  getAllVerses(): QuranVerse[] {
    return this.verses;
  }

  getSurahVerses(surah: number): QuranVerse[] {
    return this.verses.filter((v) => v.surah === surah);
  }

  getSurahList(): { surah: number; name: string; nameEn: string; count: number }[] {
    const map = new Map<number, { name: string; nameEn: string; count: number }>();
    for (const v of this.verses) {
      if (!map.has(v.surah)) {
        map.set(v.surah, { name: v.surah_name, nameEn: v.surah_name_en, count: 0 });
      }
      map.get(v.surah)!.count++;
    }
    return Array.from(map.entries()).map(([surah, info]) => ({
      surah,
      name: info.name,
      nameEn: info.nameEn,
      count: info.count,
    }));
  }

  /**
   * Match speech or transcript words against Quran verses using fuzzy n-gram matching
   */
  matchTranscript(spokenText: string): VerseCandidate[] {
    const normSpoken = normalizeArabic(spokenText);
    if (!normSpoken || normSpoken.length < 3) return [];

    const spokenWords = splitWords(normSpoken);
    const candidates: { surah: number; ayah: number; score: number }[] = [];

    for (const entry of this.normalizedIndex) {
      let score = 0;

      // Exact substring match
      if (entry.clean.includes(normSpoken)) {
        score = 0.9 + Math.min(0.09, normSpoken.length / (entry.clean.length + 1));
      } else {
        // Word overlap match
        let matchedWords = 0;
        let lastIdx = -1;
        let inOrder = true;

        for (const sw of spokenWords) {
          const foundIdx = entry.words.findIndex((w, idx) => idx > lastIdx && (w === sw || w.includes(sw) || sw.includes(w)));
          if (foundIdx !== -1) {
            matchedWords++;
            lastIdx = foundIdx;
          } else {
            inOrder = false;
          }
        }

        if (matchedWords >= 2 || (matchedWords === 1 && spokenWords.length === 1 && entry.words.length <= 5)) {
          const overlapRatio = matchedWords / Math.max(spokenWords.length, 1);
          score = overlapRatio * 0.75 + (inOrder ? 0.15 : 0.0);
        }
      }

      if (score >= 0.45) {
        candidates.push({ surah: entry.surah, ayah: entry.ayah, score });
      }
    }

    candidates.sort((a, b) => b.score - a.score);

    return candidates.slice(0, 5).map((c) => {
      const v = this.getVerse(c.surah, c.ayah)!;
      return {
        surah: c.surah,
        ayah: c.ayah,
        confidence: Math.min(0.99, Number(c.score.toFixed(2))),
        surah_name: v.surah_name,
        surah_name_en: v.surah_name_en,
      };
    });
  }

  search(query: string): QuranVerse[] {
    const qNorm = normalizeArabic(query).toLowerCase();
    if (!qNorm) return [];

    return this.verses.filter((v) => {
      const clean = normalizeArabic(v.text_clean);
      const en = v.surah_name_en.toLowerCase();
      const num = `${v.surah}:${v.ayah}`;
      return clean.includes(qNorm) || en.includes(qNorm) || num.startsWith(qNorm);
    }).slice(0, 20);
  }
}

export const quranDb = new QuranDB();
