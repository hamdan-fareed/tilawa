export interface QuranVerse {
  surah: number;
  ayah: number;
  text_uthmani: string;
  text_clean: string;
  surah_name: string;
  surah_name_en: string;
}

export type RecitationMode = "tracking" | "correction";

export interface VerseCandidate {
  surah: number;
  ayah: number;
  confidence: number;
  surah_name: string;
  surah_name_en: string;
}

export interface VerseMatch {
  surah: number;
  ayah: number;
  verse_text: string;
  surah_name: string;
  surah_name_en: string;
  confidence: number;
  current_word_index: number;
  total_words: number;
  matched_indices: number[];
}

export interface WordVerdict {
  index: number;
  expected: string;
  heard?: string;
  status: "correct" | "slip" | "skipped" | "pending";
  confidence: number;
}

export interface CorrectionState {
  targetSurah: number;
  targetAyah: number;
  verse: QuranVerse;
  verdicts: WordVerdict[];
  currentWordIndex: number;
  accuracy: number;
  slipsCount: number;
  isComplete: boolean;
}

export interface DiagnosticReport {
  timestamp: string;
  intendedSurah: number;
  intendedAyah: number;
  predictedSurah?: number;
  predictedAyah?: number;
  transcript: string;
  notes: string;
  durationMs: number;
}
