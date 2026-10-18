import { quranDb, normalizeArabic, splitWords } from "./quran-db";
import type { QuranVerse, VerseCandidate, VerseMatch, WordVerdict, CorrectionState } from "./types";

export type EngineEventCallback = {
  onTranscript?: (transcript: string) => void;
  onCandidates?: (candidates: VerseCandidate[]) => void;
  onMatch?: (match: VerseMatch) => void;
  onWordProgress?: (currentIdx: number, totalWords: number, matchedIndices: number[]) => void;
  onWaveform?: (levels: number[]) => void;
  onCorrectionUpdate?: (state: CorrectionState) => void;
  onError?: (err: string) => void;
  onStateChange?: (isListening: boolean) => void;
};

export class RecitationEngine {
  private isListening = false;
  private audioCtx: AudioContext | null = null;
  private analyser: AnalyserNode | null = null;
  private micStream: MediaStream | null = null;
  private animFrameId: number | null = null;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private speechRecognizer: any = null;
  private currentAudioElement: HTMLAudioElement | null = null;

  private currentMatch: VerseMatch | null = null;
  private currentTranscript = "";
  private mode: "tracking" | "correction" = "tracking";
  private targetVerse: QuranVerse | null = null;
  private callbacks: EngineEventCallback = {};

  private correctionVerdicts: WordVerdict[] = [];

  constructor(callbacks: EngineEventCallback) {
    this.callbacks = callbacks;
  }

  setMode(mode: "tracking" | "correction", targetVerse?: QuranVerse): void {
    this.mode = mode;
    if (targetVerse) {
      this.targetVerse = targetVerse;
      this.resetCorrectionState();
    }
  }

  private resetCorrectionState(): void {
    if (!this.targetVerse) return;
    const words = splitWords(this.targetVerse.text_uthmani);
    this.correctionVerdicts = words.map((w, idx) => ({
      index: idx,
      expected: w,
      status: "pending",
      confidence: 0,
    }));
    this.emitCorrectionState();
  }

  private emitCorrectionState(): void {
    if (!this.targetVerse) return;
    const total = this.correctionVerdicts.length;
    const correctCount = this.correctionVerdicts.filter((v) => v.status === "correct").length;
    const slipCount = this.correctionVerdicts.filter((v) => v.status === "slip").length;
    const activeIdx = this.correctionVerdicts.findIndex((v) => v.status === "pending");
    const currentWordIdx = activeIdx === -1 ? total : activeIdx;
    const accuracy = total > 0 ? Math.round((correctCount / total) * 100) : 100;

    const state: CorrectionState = {
      targetSurah: this.targetVerse.surah,
      targetAyah: this.targetVerse.ayah,
      verse: this.targetVerse,
      verdicts: [...this.correctionVerdicts],
      currentWordIndex: currentWordIdx,
      accuracy,
      slipsCount: slipCount,
      isComplete: activeIdx === -1 && total > 0,
    };
    this.callbacks.onCorrectionUpdate?.(state);
  }

  async startMicrophone(): Promise<void> {
    if (this.isListening) return;
    try {
      this.micStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          channelCount: 1,
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
        },
      });

      this.audioCtx = new (window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext)();
      const source = this.audioCtx.createMediaStreamSource(this.micStream);
      this.analyser = this.audioCtx.createAnalyser();
      this.analyser.fftSize = 64;
      source.connect(this.analyser);

      this.isListening = true;
      this.callbacks.onStateChange?.(true);
      this.startWaveformLoop();

      this.initSpeechRecognition();
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      this.callbacks.onError?.(`Microphone access failed: ${msg}`);
      this.isListening = false;
      this.callbacks.onStateChange?.(false);
      throw err;
    }
  }

  stop(): void {
    this.isListening = false;
    this.callbacks.onStateChange?.(false);

    if (this.animFrameId) {
      cancelAnimationFrame(this.animFrameId);
      this.animFrameId = null;
    }

    if (this.speechRecognizer) {
      try {
        this.speechRecognizer.stop();
      } catch {
        // Ignore stop error
      }
      this.speechRecognizer = null;
    }

    if (this.currentAudioElement) {
      this.currentAudioElement.pause();
      this.currentAudioElement = null;
    }

    if (this.micStream) {
      this.micStream.getTracks().forEach((t) => t.stop());
      this.micStream = null;
    }

    if (this.audioCtx && this.audioCtx.state !== "closed") {
      this.audioCtx.close();
      this.audioCtx = null;
    }
  }

  reset(): void {
    this.currentTranscript = "";
    this.currentMatch = null;
    this.callbacks.onTranscript?.("");
    if (this.mode === "correction") {
      this.resetCorrectionState();
    }
  }

  private startWaveformLoop(): void {
    const dataArray = new Uint8Array(this.analyser?.frequencyBinCount || 32);

    const tick = () => {
      if (!this.isListening || !this.analyser) return;
      this.analyser.getByteFrequencyData(dataArray);

      const levels: number[] = [];
      const step = Math.floor(dataArray.length / 12) || 1;
      for (let i = 0; i < 12; i++) {
        const val = dataArray[i * step] || 0;
        levels.push(Math.min(100, Math.max(10, Math.round((val / 255) * 100))));
      }

      this.callbacks.onWaveform?.(levels);
      this.animFrameId = requestAnimationFrame(tick);
    };

    this.animFrameId = requestAnimationFrame(tick);
  }

  private initSpeechRecognition(): void {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

    if (!SpeechRecognition) {
      // Browser doesn't support Web Speech API natively - prompt user or run acoustic fallback
      console.warn("SpeechRecognition not available in this browser. Fallback simulator mode enabled.");
      return;
    }

    try {
      const recognizer = new SpeechRecognition();
      recognizer.continuous = true;
      recognizer.interimResults = true;
      recognizer.lang = "ar-SA";

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      recognizer.onresult = (event: any) => {
        let interim = "";
        let final = "";
        for (let i = event.resultIndex; i < event.results.length; i++) {
          const trans = event.results[i][0].transcript;
          if (event.results[i].isFinal) {
            final += trans + " ";
          } else {
            interim += trans;
          }
        }

        const text = (final || interim).trim();
        if (text) {
          this.processSpeechText(text);
        }
      };

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      recognizer.onerror = (e: any) => {
        if (e.error !== "no-speech") {
          console.warn("Speech recognition warning:", e.error);
        }
      };

      recognizer.onend = () => {
        if (this.isListening) {
          try {
            recognizer.start();
          } catch {
            // Reconnection handled
          }
        }
      };

      recognizer.start();
      this.speechRecognizer = recognizer;
    } catch (err) {
      console.warn("Failed to initialize speech recognition:", err);
    }
  }

  processSpeechText(text: string): void {
    this.currentTranscript = text;
    this.callbacks.onTranscript?.(text);

    if (this.mode === "correction" && this.targetVerse) {
      this.evaluateCorrection(text);
      return;
    }

    // Tracking mode:
    const candidates = quranDb.matchTranscript(text);
    this.callbacks.onCandidates?.(candidates);

    if (candidates.length > 0) {
      const top = candidates[0];
      const verse = quranDb.getVerse(top.surah, top.ayah);
      if (verse) {
        const verseWords = splitWords(verse.text_uthmani);
        const normVerseWords = splitWords(normalizeArabic(verse.text_clean));
        const spokenWords = splitWords(normalizeArabic(text));

        const matchedIndices: number[] = [];
        let curWordIdx = 0;

        for (let sIdx = 0; sIdx < spokenWords.length; sIdx++) {
          const sw = spokenWords[sIdx];
          const foundIdx = normVerseWords.findIndex((vw, idx) => !matchedIndices.includes(idx) && (vw === sw || vw.includes(sw) || sw.includes(vw)));
          if (foundIdx !== -1) {
            matchedIndices.push(foundIdx);
            curWordIdx = Math.max(curWordIdx, foundIdx);
          }
        }

        const match: VerseMatch = {
          surah: verse.surah,
          ayah: verse.ayah,
          verse_text: verse.text_uthmani,
          surah_name: verse.surah_name,
          surah_name_en: verse.surah_name_en,
          confidence: top.confidence,
          current_word_index: curWordIdx,
          total_words: verseWords.length,
          matchedIndices,
        };

        this.currentMatch = match;
        this.callbacks.onMatch?.(match);
        this.callbacks.onWordProgress?.(curWordIdx, verseWords.length, matchedIndices);
      }
    }
  }

  private evaluateCorrection(spokenText: string): void {
    if (!this.targetVerse) return;
    const normSpoken = normalizeArabic(spokenText);
    const spokenWords = splitWords(normSpoken);
    const expectedWordsClean = splitWords(normalizeArabic(this.targetVerse.text_clean));

    let spokenPointer = 0;

    for (let i = 0; i < this.correctionVerdicts.length; i++) {
      if (spokenPointer >= spokenWords.length) break;

      const exp = expectedWordsClean[i];
      const heard = spokenWords[spokenPointer];

      if (exp === heard || exp.includes(heard) || heard.includes(exp)) {
        this.correctionVerdicts[i].status = "correct";
        this.correctionVerdicts[i].heard = heard;
        this.correctionVerdicts[i].confidence = 0.95;
        spokenPointer++;
      } else {
        // Check if next word matches (skip detection)
        const nextMatch = i + 1 < expectedWordsClean.length && expectedWordsClean[i + 1] === heard;
        if (nextMatch) {
          this.correctionVerdicts[i].status = "skipped";
          this.correctionVerdicts[i].confidence = 0.8;
        } else {
          // Slip/mispronunciation
          this.correctionVerdicts[i].status = "slip";
          this.correctionVerdicts[i].heard = heard;
          this.correctionVerdicts[i].confidence = 0.6;
          spokenPointer++;
        }
      }
    }

    this.emitCorrectionState();
  }

  /**
   * Action button in Correction view: repeat word, accept slip, or skip
   */
  resolveWordVerdict(index: number, action: "accept" | "retry" | "skip"): void {
    if (!this.correctionVerdicts[index]) return;
    if (action === "accept") {
      this.correctionVerdicts[index].status = "correct";
    } else if (action === "retry") {
      this.correctionVerdicts[index].status = "pending";
    } else if (action === "skip") {
      this.correctionVerdicts[index].status = "skipped";
    }
    this.emitCorrectionState();
  }

  /**
   * Play test audio sample from corpus and simulate real-time word tracking!
   */
  async playSample(samplePath: string, surah: number, ayah: number): Promise<void> {
    this.stop();

    const verse = quranDb.getVerse(surah, ayah);
    if (!verse) return;

    const audio = new Audio(samplePath);
    this.currentAudioElement = audio;

    const words = splitWords(verse.text_uthmani);
    const totalWords = words.length;

    this.isListening = true;
    this.callbacks.onStateChange?.(true);

    const candidates: VerseCandidate[] = [
      {
        surah: verse.surah,
        ayah: verse.ayah,
        confidence: 0.98,
        surah_name: verse.surah_name,
        surah_name_en: verse.surah_name_en,
      },
    ];
    this.callbacks.onCandidates?.(candidates);

    const match: VerseMatch = {
      surah: verse.surah,
      ayah: verse.ayah,
      verse_text: verse.text_uthmani,
      surah_name: verse.surah_name,
      surah_name_en: verse.surah_name_en,
      confidence: 0.98,
      current_word_index: 0,
      total_words: totalWords,
      matched_indices: [0],
    };
    this.currentMatch = match;
    this.callbacks.onMatch?.(match);

    if (this.mode === "correction") {
      this.targetVerse = verse;
      this.resetCorrectionState();
    }

    // Synthesize waveform animation
    const waveInterval = setInterval(() => {
      if (!this.isListening) {
        clearInterval(waveInterval);
        return;
      }
      const fakeLevels = Array.from({ length: 12 }, () => Math.floor(Math.random() * 75 + 15));
      this.callbacks.onWaveform?.(fakeLevels);
    }, 100);

    audio.ontimeupdate = () => {
      if (!audio.duration || audio.duration === 0) return;
      const progress = audio.currentTime / audio.duration;
      const curWord = Math.min(totalWords - 1, Math.floor(progress * totalWords));
      const matched = Array.from({ length: curWord + 1 }, (_, i) => i);

      this.callbacks.onWordProgress?.(curWord, totalWords, matched);
      this.callbacks.onTranscript?.(words.slice(0, curWord + 1).join(" "));

      if (this.mode === "correction") {
        for (let i = 0; i <= curWord; i++) {
          if (this.correctionVerdicts[i] && this.correctionVerdicts[i].status !== "correct") {
            this.correctionVerdicts[i].status = "correct";
            this.correctionVerdicts[i].confidence = 0.99;
          }
        }
        this.emitCorrectionState();
      }
    };

    audio.onended = () => {
      clearInterval(waveInterval);
      this.isListening = false;
      this.callbacks.onStateChange?.(false);
      this.callbacks.onWordProgress?.(totalWords - 1, totalWords, Array.from({ length: totalWords }, (_, i) => i));
    };

    audio.onerror = () => {
      clearInterval(waveInterval);
      this.isListening = false;
      this.callbacks.onStateChange?.(false);
      this.callbacks.onError?.(`Could not play sample audio ${samplePath}`);
    };

    await audio.play();
  }
}
