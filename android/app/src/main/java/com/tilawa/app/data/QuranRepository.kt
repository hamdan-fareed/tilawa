package com.tilawa.app.data

import com.tilawa.app.domain.model.QuranVerse
import com.tilawa.app.domain.model.VerseCandidate

class QuranRepository {

    // Pre-indexed key verses for immediate offline functionality
    private val standardVerses = listOf(
        QuranVerse(
            surah = 1,
            ayah = 1,
            textUthmani = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
            textClean = "بسم الله الرحمن الرحيم",
            surahName = "سُورَةُ ٱلْفَاتِحَةِ",
            surahNameEn = "Al-Faatiha"
        ),
        QuranVerse(
            surah = 1,
            ayah = 2,
            textUthmani = "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ",
            textClean = "الحمد لله رب العلمين",
            surahName = "سُورَةُ ٱلْفَاتِحَةِ",
            surahNameEn = "Al-Faatiha"
        ),
        QuranVerse(
            surah = 1,
            ayah = 3,
            textUthmani = "ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
            textClean = "الرحمن الرحيم",
            surahName = "سُورَةُ ٱلْفَاتِحَةِ",
            surahNameEn = "Al-Faatiha"
        ),
        QuranVerse(
            surah = 1,
            ayah = 4,
            textUthmani = "مَٰلِكِ يَوْمِ ٱلدِّينِ",
            textClean = "ملك يوم الدين",
            surahName = "سُورَةُ ٱلْفَاتِحَةِ",
            surahNameEn = "Al-Faatiha"
        ),
        QuranVerse(
            surah = 1,
            ayah = 5,
            textUthmani = "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ",
            textClean = "إياك نعبد وإياك نستعين",
            surahName = "سُورَةُ ٱلْفَاتِحَةِ",
            surahNameEn = "Al-Faatiha"
        ),
        QuranVerse(
            surah = 1,
            ayah = 6,
            textUthmani = "ٱهْدِنَا ٱلصِّرَٰطَ ٱلْمُسْتَقِيمَ",
            textClean = "اهدنا الصرط المستقيم",
            surahName = "سُورَةُ ٱلْفَاتِحَةِ",
            surahNameEn = "Al-Faatiha"
        ),
        QuranVerse(
            surah = 1,
            ayah = 7,
            textUthmani = "صِرَٰطَ ٱلَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ ٱلْمَغْضُوبِ عَلَيْهِمْ وَلَا ٱلضَّآلِّينَ",
            textClean = "صرط الذين أنعمت عليهم غير المغضوب عليهم ولا الضالين",
            surahName = "سُورَةُ ٱلْفَاتِحَةِ",
            surahNameEn = "Al-Faatiha"
        ),
        QuranVerse(
            surah = 2,
            ayah = 255,
            textUthmani = "ٱللَّهُ لَآ إِلَٰهَ إِلَّا هُوَ ٱلْحَىُّ ٱلْقَيُّومُ ۚ لَا تَأْخُذُهُۥ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُۥ مَا فِى ٱلسَّمَٰوَٰتِ وَمَا فِى ٱلْأَرْضِ ۗ مَن ذَا ٱلَّذِى يَشْفَعُ عِندَهُۥٓ إِلَّا بِإِذْنِهِۦ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَىْءٍ مِّنْ عِلْمِهِۦٓ إِلَّا بِمَا شَآءَ ۚ وَسِعَ كُرْسِيُّهُ ٱلسَّمَٰوَٰتِ وَٱلْأَرْضَ ۖ وَلَا يَـُٔودُهُۥ حِفْظُهُمَا ۚ وَهُوَ ٱلْعَلِىُّ ٱلْعَظِيمُ",
            textClean = "الله لا إله إلا هو الحى القيوم لا تأخذه سنة ولا نوم له ما فى السموت وما فى الأرض من ذا الذى يشفع عنده إلا بإذنه يعلم ما بين أيديهم وما خلفهم ولا يحيطون بشىء من علمه إلا بما شاء وسع كرسيه السموت والأرض ولا يوده حفظهما وهو العلى العظيم",
            surahName = "سُورَةُ البَقَرَةِ",
            surahNameEn = "Al-Baqara"
        ),
        QuranVerse(
            surah = 36,
            ayah = 1,
            textUthmani = "يسٓ",
            textClean = "يس",
            surahName = "سُورَةُ يٰسٓ",
            surahNameEn = "Yaseen"
        ),
        QuranVerse(
            surah = 112,
            ayah = 1,
            textUthmani = "قُلْ هُوَ ٱللَّهُ أَحَدٌ",
            textClean = "قل هو الله أحد",
            surahName = "سُورَةُ الإِخْلَاصِ",
            surahNameEn = "Al-Ikhlaas"
        ),
        QuranVerse(
            surah = 112,
            ayah = 2,
            textUthmani = "ٱللَّهُ ٱلصَّمَدُ",
            textClean = "الله الصمد",
            surahName = "سُورَةُ الإِخْلَاصِ",
            surahNameEn = "Al-Ikhlaas"
        ),
        QuranVerse(
            surah = 112,
            ayah = 3,
            textUthmani = "لَمْ يَلِدْ وَلَمْ يُولَدْ",
            textClean = "لم يلد ولم يولد",
            surahName = "سُورَةُ الإِخْلَاصِ",
            surahNameEn = "Al-Ikhlaas"
        ),
        QuranVerse(
            surah = 112,
            ayah = 4,
            textUthmani = "وَلَمْ يَكُن لَّهُۥ كُفُوًا أَحَدٌ",
            textClean = "ولم يكن له كفوا أحد",
            surahName = "سُورَةُ الإِخْلَاصِ",
            surahNameEn = "Al-Ikhlaas"
        )
    )

    fun getVerse(surah: Int, ayah: Int): QuranVerse? {
        return standardVerses.find { it.surah == surah && it.ayah == ayah }
    }

    fun getAllVerses(): List<QuranVerse> = standardVerses

    fun normalizeArabic(text: String): String {
        return text.replace(Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]"), "")
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ٱ", "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
            .trim()
    }

    fun matchSpokenText(spoken: String): List<VerseCandidate> {
        val norm = normalizeArabic(spoken)
        if (norm.length < 2) return emptyList()

        return standardVerses.mapNotNull { v ->
            val vClean = normalizeArabic(v.textClean)
            val score = when {
                vClean.contains(norm) -> 0.95f
                norm.contains(vClean) -> 0.90f
                else -> {
                    val sWords = norm.split(" ").filter { it.isNotBlank() }
                    val vWords = vClean.split(" ").filter { it.isNotBlank() }
                    val matches = sWords.count { sw -> vWords.any { vw -> vw == sw || vw.contains(sw) } }
                    if (matches > 0) matches.toFloat() / sWords.size.toFloat() else 0f
                }
            }
            if (score > 0.4f) {
                VerseCandidate(
                    surah = v.surah,
                    ayah = v.ayah,
                    confidence = score,
                    surahName = v.surahName,
                    surahNameEn = v.surahNameEn
                )
            } else null
        }.sortedByDescending { it.confidence }
    }
}
