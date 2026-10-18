import "./style.css";
import { quranDb, splitWords } from "./quran-db";
import { RecitationEngine } from "./recitation-engine";
import type { QuranVerse, VerseCandidate, VerseMatch, CorrectionState, DiagnosticReport } from "./types";

// State
let currentMode: "tracking" | "correction" = "tracking";
let currentVerse: QuranVerse | null = null;
let currentWordIndex = -1;
let matchedWordIndices: number[] = [];
let isListening = false;

// DOM Elements
const $btnMic = document.getElementById("btn-mic") as HTMLButtonElement;
const $btnStop = document.getElementById("btn-stop") as HTMLButtonElement;
const $btnReset = document.getElementById("btn-reset") as HTMLButtonElement;
const $btnExplore = document.getElementById("btn-explore") as HTMLButtonElement;
const $btnReport = document.getElementById("btn-report") as HTMLButtonElement;
const $btnFile = document.getElementById("audio-file-input") as HTMLInputElement;

const $modeTracking = document.getElementById("mode-tracking") as HTMLButtonElement;
const $modeCorrection = document.getElementById("mode-correction") as HTMLButtonElement;

const $statusBadge = document.getElementById("status-badge")!;
const $verseMeta = document.getElementById("verse-meta")!;
const $verseText = document.getElementById("verse-text")!;
const $transcriptPreview = document.getElementById("transcript-preview")!;
const $waveformBars = document.querySelectorAll<HTMLElement>(".waveform-bar");

const $correctionContainer = document.getElementById("correction-container")!;
const $trackingContainer = document.getElementById("tracking-container")!;
const $correctionStats = document.getElementById("correction-stats")!;
const $verdictList = document.getElementById("verdict-list")!;

const $candidatesList = document.getElementById("candidates-list")!;

// Modals
const $explorerModal = document.getElementById("explorer-modal")!;
const $closeExplorer = document.getElementById("close-explorer")!;
const $searchInput = document.getElementById("search-input") as HTMLInputElement;
const $surahList = document.getElementById("surah-list")!;

const $reportModal = document.getElementById("report-modal")!;
const $closeReport = document.getElementById("close-report")!;
const $submitReport = document.getElementById("submit-report")!;
const $reportSurah = document.getElementById("report-surah") as HTMLSelectElement;
const $reportAyah = document.getElementById("report-ayah") as HTMLInputElement;
const $reportNotes = document.getElementById("report-notes") as HTMLTextAreaElement;

// Engine Setup
const engine = new RecitationEngine({
  onStateChange: (listening) => {
    isListening = listening;
    updateMicButtons();
  },
  onTranscript: (text) => {
    if (text) {
      $transcriptPreview.textContent = text;
      $transcriptPreview.style.display = "inline-flex";
    } else {
      $transcriptPreview.style.display = "none";
    }
  },
  onWaveform: (levels) => {
    levels.forEach((lvl, idx) => {
      if ($waveformBars[idx]) {
        $waveformBars[idx].style.height = `${Math.max(6, Math.min(48, (lvl / 100) * 48))}px`;
      }
    });
  },
  onCandidates: (candidates) => {
    renderCandidates(candidates);
  },
  onMatch: (match) => {
    renderMatch(match);
  },
  onWordProgress: (curIdx, _total, matched) => {
    currentWordIndex = curIdx;
    matchedWordIndices = matched;
    updateWordHighlighting();
  },
  onCorrectionUpdate: (state) => {
    renderCorrectionState(state);
  },
  onError: (msg) => {
    $statusBadge.textContent = "Error: " + msg;
    $statusBadge.className = "status-badge";
  },
});

function updateMicButtons(): void {
  if (isListening) {
    $btnMic.style.display = "none";
    $btnStop.style.display = "inline-flex";
    $statusBadge.textContent = "Listening to Recitation...";
    $statusBadge.className = "status-badge live";
  } else {
    $btnMic.style.display = "inline-flex";
    $btnStop.style.display = "none";
    $statusBadge.textContent = "Offline Ready";
    $statusBadge.className = "status-badge";
    $waveformBars.forEach((bar) => (bar.style.height = "12px"));
  }
}

function renderCandidates(candidates: VerseCandidate[]): void {
  if (candidates.length === 0) {
    $candidatesList.innerHTML = `<span style="color: var(--md-sys-color-outline); font-size: 0.9rem;">No candidates detected yet. Recite or play a sample.</span>`;
    return;
  }
  $candidatesList.innerHTML = candidates
    .map(
      (c) => `
    <button class="chip-btn" data-surah="${c.surah}" data-ayah="${c.ayah}">
      <span>${c.surah}:${c.ayah}</span>
      <span style="font-family: var(--font-arabic);">${c.surah_name}</span>
      <span style="font-size: 0.75rem; opacity: 0.8;">(${Math.round(c.confidence * 100)}%)</span>
    </button>
  `
    )
    .join("");

  $candidatesList.querySelectorAll(".chip-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      const s = Number(btn.getAttribute("data-surah"));
      const a = Number(btn.getAttribute("data-ayah"));
      selectVerse(s, a);
    });
  });
}

function selectVerse(surah: number, ayah: number): void {
  const verse = quranDb.getVerse(surah, ayah);
  if (!verse) return;
  currentVerse = verse;

  renderVerseText(verse);

  if (currentMode === "correction") {
    engine.setMode("correction", verse);
  }
}

function renderVerseText(verse: QuranVerse): void {
  $verseMeta.innerHTML = `
    <span>${verse.surah_name}</span>
    <span class="surah-en">${verse.surah_name_en} (${verse.surah}:${verse.ayah})</span>
  `;

  const words = splitWords(verse.text_uthmani);
  $verseText.innerHTML = words
    .map((w, idx) => `<span class="quran-word" id="word-${idx}">${w}</span>`)
    .join(" ") + `<span class="ayah-end-symbol">۝${toArabicIndic(verse.ayah)}</span>`;

  updateWordHighlighting();
}

function toArabicIndic(num: number): string {
  const digits = ["٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩"];
  return String(num)
    .split("")
    .map((d) => digits[Number(d)] ?? d)
    .join("");
}

function updateWordHighlighting(): void {
  const words = $verseText.querySelectorAll(".quran-word");
  words.forEach((el, idx) => {
    el.className = "quran-word";
    if (matchedWordIndices.includes(idx)) {
      el.classList.add("spoken");
    }
    if (idx === currentWordIndex) {
      el.classList.add("current");
    }
  });
}

function renderMatch(match: VerseMatch): void {
  const verse = quranDb.getVerse(match.surah, match.ayah);
  if (verse) {
    currentVerse = verse;
    renderVerseText(verse);
  }
}

function renderCorrectionState(state: CorrectionState): void {
  $correctionStats.innerHTML = `
    <div style="display: flex; gap: 16px; justify-content: center; margin-bottom: 12px; font-weight: 600;">
      <span style="color: var(--md-sys-color-primary);">Accuracy: ${state.accuracy}%</span>
      <span style="color: #B26A00;">Slips: ${state.slipsCount}</span>
      <span>${state.currentWordIndex} of ${state.verdicts.length} words</span>
    </div>
  `;

  $verdictList.innerHTML = state.verdicts
    .map((v) => {
      let badge = "PENDING";
      let cls = "pending";
      if (v.status === "correct") {
        badge = "✓ CORRECT";
        cls = "correct";
      } else if (v.status === "slip") {
        badge = "⚠ SLIP";
        cls = "slip";
      } else if (v.status === "skipped") {
        badge = "✕ SKIPPED";
        cls = "skipped";
      }

      return `
      <div class="verdict-card ${cls}">
        <span class="verdict-word">${v.expected}</span>
        ${v.heard ? `<span style="font-size: 0.8rem; color: #555;">Heard: ${v.heard}</span>` : ""}
        <span class="verdict-status-label">${badge}</span>
        ${
          v.status === "slip"
            ? `
          <div style="display: flex; gap: 4px; margin-top: 4px;">
            <button class="btn btn-tonal" style="min-height: 28px; padding: 2px 8px; font-size: 0.75rem;" data-action="accept" data-idx="${v.index}">Accept</button>
            <button class="btn btn-secondary" style="min-height: 28px; padding: 2px 8px; font-size: 0.75rem;" data-action="retry" data-idx="${v.index}">Retry</button>
          </div>
        `
            : ""
        }
      </div>
    `;
    })
    .join("");

  $verdictList.querySelectorAll("button[data-action]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const idx = Number(btn.getAttribute("data-idx"));
      const action = btn.getAttribute("data-action") as "accept" | "retry" | "skip";
      engine.resolveWordVerdict(idx, action);
    });
  });
}

// Mode Buttons
$modeTracking.addEventListener("click", () => {
  currentMode = "tracking";
  $modeTracking.classList.add("active");
  $modeCorrection.classList.remove("active");
  $trackingContainer.style.display = "block";
  $correctionContainer.style.display = "none";
  engine.setMode("tracking");
  if (currentVerse) renderVerseText(currentVerse);
});

$modeCorrection.addEventListener("click", () => {
  currentMode = "correction";
  $modeCorrection.classList.add("active");
  $modeTracking.classList.remove("active");
  $trackingContainer.style.display = "none";
  $correctionContainer.style.display = "block";
  if (!currentVerse) {
    // Default to Al-Ikhlas (112:1)
    currentVerse = quranDb.getVerse(112, 1) || quranDb.getVerse(1, 1)!;
  }
  engine.setMode("correction", currentVerse);
  renderVerseText(currentVerse);
});

// Controls
$btnMic.addEventListener("click", async () => {
  try {
    await engine.startMicrophone();
  } catch (err) {
    console.error(err);
  }
});

$btnStop.addEventListener("click", () => {
  engine.stop();
});

$btnReset.addEventListener("click", () => {
  engine.reset();
  matchedWordIndices = [];
  currentWordIndex = -1;
  updateWordHighlighting();
});

// Sample Chips Listeners
document.querySelectorAll(".chip-sample").forEach((chip) => {
  chip.addEventListener("click", () => {
    const file = chip.getAttribute("data-file");
    const surah = Number(chip.getAttribute("data-surah"));
    const ayah = Number(chip.getAttribute("data-ayah"));
    if (file && surah && ayah) {
      engine.playSample(file, surah, ayah);
    }
  });
});

// Audio File Input
$btnFile.addEventListener("change", (e) => {
  const target = e.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return;

  const url = URL.createObjectURL(file);
  // Default to verse match attempt
  engine.playSample(url, currentVerse?.surah || 1, currentVerse?.ayah || 1);
});

// Explorer Modal
$btnExplore.addEventListener("click", () => {
  $explorerModal.style.display = "grid";
  renderSurahList();
});

$closeExplorer.addEventListener("click", () => {
  $explorerModal.style.display = "none";
});

function renderSurahList(filter = ""): void {
  const list = quranDb.getSurahList();
  const filtered = filter
    ? list.filter((s) => s.nameEn.toLowerCase().includes(filter.toLowerCase()) || s.name.includes(filter) || String(s.surah) === filter)
    : list;

  $surahList.innerHTML = filtered
    .map(
      (s) => `
    <div class="surah-item" data-surah="${s.surah}">
      <div style="display: flex; align-items: center; gap: 10px;">
        <div class="surah-item-number">${s.surah}</div>
        <div>
          <div style="font-weight: 600;">${s.nameEn}</div>
          <div style="font-size: 0.8rem; color: var(--md-sys-color-outline);">${s.count} Ayahs</div>
        </div>
      </div>
      <div class="surah-item-ar">${s.name}</div>
    </div>
  `
    )
    .join("");

  $surahList.querySelectorAll(".surah-item").forEach((item) => {
    item.addEventListener("click", () => {
      const s = Number(item.getAttribute("data-surah"));
      selectVerse(s, 1);
      $explorerModal.style.display = "none";
    });
  });
}

$searchInput.addEventListener("input", (e) => {
  const query = (e.target as HTMLInputElement).value;
  renderSurahList(query);
});

// Report Modal
$btnReport.addEventListener("click", () => {
  populateSurahDropdown();
  if (currentVerse) {
    $reportSurah.value = String(currentVerse.surah);
    $reportAyah.value = String(currentVerse.ayah);
  }
  $reportModal.style.display = "grid";
});

$closeReport.addEventListener("click", () => {
  $reportModal.style.display = "none";
});

function populateSurahDropdown(): void {
  const surahs = quranDb.getSurahList();
  $reportSurah.innerHTML = surahs.map((s) => `<option value="${s.surah}">${s.surah}. ${s.nameEn} (${s.name})</option>`).join("");
}

$submitReport.addEventListener("click", () => {
  const rep: DiagnosticReport = {
    timestamp: new Date().toISOString(),
    intendedSurah: Number($reportSurah.value),
    intendedAyah: Number($reportAyah.value),
    predictedSurah: currentVerse?.surah,
    predictedAyah: currentVerse?.ayah,
    transcript: $transcriptPreview.textContent || "",
    notes: $reportNotes.value,
    durationMs: 5000,
  };

  const stored = JSON.parse(localStorage.getItem("tilawa-reports") || "[]");
  stored.push(rep);
  localStorage.setItem("tilawa-reports", JSON.stringify(stored));

  alert("Report saved successfully. Thank you for contributing to offline recitation accuracy!");
  $reportModal.style.display = "none";
  $reportNotes.value = "";
});

// Initialization
async function init() {
  $statusBadge.textContent = "Loading Quran Database...";
  try {
    await quranDb.load();
    $statusBadge.textContent = "100% Offline Ready";
    $statusBadge.className = "status-badge";

    // Initialize with Surah Al-Fatihah (1:1)
    const firstVerse = quranDb.getVerse(1, 1);
    if (firstVerse) {
      currentVerse = firstVerse;
      renderVerseText(firstVerse);
    }
  } catch (err) {
    console.error("Initialization error:", err);
    $statusBadge.textContent = "Error loading database";
  }
}

init();
