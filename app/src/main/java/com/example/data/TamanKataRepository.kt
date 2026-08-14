package com.example.data

import kotlinx.coroutines.flow.Flow

class TamanKataRepository(
    private val dao: TamanKataDao,
    private val consentPreferences: ConsentPreferences? = null
) {
    val allStages: Flow<List<Stage>> = dao.getAllStages()
    val sessionHistory: Flow<List<SessionHistory>> = dao.getSessionHistory()
    val masteredItems: Flow<List<LearningItem>> = dao.getMasteredItems()
    val allStories: Flow<List<Story>> = dao.getAllStories()

    val hasConsented: Flow<Boolean> = consentPreferences?.hasConsented ?: kotlinx.coroutines.flow.flowOf(false)
    val consentTimestamp: Flow<Long> = consentPreferences?.consentTimestamp ?: kotlinx.coroutines.flow.flowOf(0L)

    suspend fun saveConsent(consented: Boolean, timestamp: Long = System.currentTimeMillis()) {
        consentPreferences?.saveConsent(consented, timestamp)
    }

    suspend fun revokeConsent() {
        consentPreferences?.revokeConsent()
    }

    fun getItemsForStage(stageId: Int): Flow<List<LearningItem>> {
        return dao.getItemsForStage(stageId)
    }

    suspend fun updateStage(stage: Stage) = dao.updateStage(stage)
    suspend fun updateItem(item: LearningItem) = dao.updateItem(item)
    suspend fun saveSession(history: SessionHistory) = dao.insertSessionHistory(history)
    suspend fun getWeakItems(limit: Int) = dao.getWeakItems(limit)
    
    suspend fun initializeDummyData() {
        if (dao.getStageCount() == 0) {
            val stages = listOf(
                Stage(id = 0, title = "Tahap 1: Vokal", isUnlocked = true),
                Stage(id = 1, title = "Tahap 2: KV", isUnlocked = false),
                Stage(id = 2, title = "Tahap 3: KVK", isUnlocked = false),
                Stage(id = 3, title = "Tahap 4: Kluster", isUnlocked = false),
                Stage(id = 4, title = "Tahap 5: 3+ Suku Kata", isUnlocked = false),
                Stage(id = 5, title = "Tahap 6: Kalimat", isUnlocked = false),
                Stage(id = 6, title = "Tahap 7: Paragraf Mini", isUnlocked = false),
                Stage(id = 7, title = "Tahap 8: Kelancaran", isUnlocked = false),
                Stage(id = 8, title = "Tahap 9", isUnlocked = false)
            )
            dao.insertStages(stages)
            
            val items = mutableListOf<LearningItem>()
            // Stage 0 (Tahap 1: Vokal - a, i, u, e, o)
            val stage0Vowels = listOf("a", "i", "u", "e", "o")
            stage0Vowels.forEach { vowel ->
                items.add(LearningItem(stageId = 0, text = vowel, syllables = vowel))
            }
            
            // Stage 1 (Tahap 2: Suku Kata KV - 21 keluarga huruf x 5 vokal = 105 item)
            // Urutan keluarga huruf berdasarkan frekuensi umum: b, m, p, t, d, n, l, k, s, r, g, h, c, j, w, y, f, v, z, ng, ny
            // Catatan: "ng" dan "ny" adalah konsonan ganda (digraf). Tolong diuji manual apakah TTS Android melafalkan "nga", "nyi", dll dengan wajar.
            val stage1Consonants = listOf("b", "m", "p", "t", "d", "n", "l", "k", "s", "r", "g", "h", "c", "j", "w", "y", "f", "v", "z", "ng", "ny")
            val vowels = listOf("a", "i", "u", "e", "o")
            stage1Consonants.forEach { consonant ->
                vowels.forEach { vowel ->
                    val syllable = "$consonant$vowel"
                    items.add(LearningItem(stageId = 1, text = syllable, syllables = syllable))
                }
            }
            
            // Stage 2 (Tahap 3 - Suku Kata Tertutup)
            val stage2Items = listOf(
                "ma-kan", "mi-num", "du-duk", "ku-cing", "pin-tar", "kan-cil", "ban-tal", "can-tik",
                "ti-dur", "man-di", "ban-tu", "hu-jan", "a-ngin", "ja-lan", "lom-pat", "ta-ngan",
                "ka-ki", "gun-ting", "dom-pet", "jen-de-la", "si-sir", "em-ber", "lam-pu", "kur-si",
                "me-ja", "pen-sil", "se-ko-lah", "gam-bar", "war-na", "bu-lan", "bin-tang", "po-hon"
            )
            stage2Items.forEach { 
                items.add(LearningItem(stageId = 2, text = it.replace("-", ""), syllables = it))
            }
            
            // Stage 3 (Tahap 4 - Kluster)
            val stage3Items = listOf("per-gi", "ko-tak", "sen-dok", "tem-pat", "si-kat", "ba-pak", "ker-tas", "ce-pat")
            stage3Items.forEach {
                items.add(LearningItem(stageId = 3, text = it.replace("-", ""), syllables = it))
            }
            
            // Stage 4 (Tahap 5)
            val stage4Items = listOf(
                "ke-lin-ci", "se-pa-tu", "ma-ta-ha-ri", "ke-la-pa", "ce-la-na",
                "ben-de-ra", "ku-pu-ku-pu", "ka-ca-ma-ta", "pe-sa-wat", "ke-le-reng",
                "ge-lem-bung", "ke-lom-pok", "te-rom-pet", "ke-pom-pong", "ke-me-ja",
                "le-ma-ri", "bo-ne-ka", "se-pe-da", "se-mang-ka", "pe-ra-hu",
                "ka-me-ra", "bu-a-ya"
            )
            stage4Items.forEach {
                items.add(LearningItem(stageId = 4, text = it.replace("-", ""), syllables = it))
            }

            // Stage 5 (Tahap 6)
            val stage5Items = listOf(
                "Ini bola saya.", "Kucing itu lucu.", "Adik makan nasi.", 
                "Ibu memasak di dapur.", "Burung terbang tinggi.", "Aku suka buah pisang.",
                "Siapa namamu?", "Kamu mau ke mana?", "Apa warna bajumu?", 
                "Di mana buku itu?", "Kapan kita pulang?",
                "Ayo bermain!", "Hati-hati di jalan!", "Hore, kita menang!", "Wah, bunga itu indah!"
            )
            stage5Items.forEach {
                items.add(LearningItem(stageId = 5, text = it, syllables = it))
            }

            // Stage 6 (Tahap 7) Paragraf
            val p1Extra = "{\"q1\": \"Hewan apa peliharaan Budi?\", \"o1\": [\"Anjing 🐶\", \"Kucing 🐱\"], \"a1\": 1, \"q2\": \"Warna apa kucingnya?\", \"o2\": [\"Hitam ⚫\", \"Putih ⚪\"], \"a2\": 0}"
            items.add(LearningItem(stageId = 6, text = "Ini kucing Budi. Kucing Budi warna hitam. Budi suka main bola sama kucing.", extraData = p1Extra))
            
            val p2Extra = "{\"q1\": \"Siapa yang lari?\", \"o1\": [\"Kelinci 🐇\", \"Kura-kura 🐢\"], \"a1\": 0, \"q2\": \"Di mana kelinci lari?\", \"o2\": [\"Di taman 🌳\", \"Di rumah 🏠\"], \"a2\": 0}"
            items.add(LearningItem(stageId = 6, text = "Kelinci suka lari. Kelinci lari di taman. Taman itu sangat luas.", extraData = p2Extra))
            
            val p3Extra = "{\"q1\": \"Hari apa ini?\", \"o1\": [\"Minggu 📅\", \"Senin 📅\"], \"a1\": 0, \"q2\": \"Siti dan ibu buat apa?\", \"o2\": [\"Kue 🍰\", \"Roti 🍞\"], \"a2\": 0}"
            items.add(LearningItem(stageId = 6, text = "Hari ini hari Minggu. Siti membantu ibu di dapur. Mereka membuat kue bolu.", extraData = p3Extra))

            val p4Extra = "{\"q1\": \"Di mana burung berada?\", \"o1\": [\"Pohon 🌳\", \"Sangkar 🏡\"], \"a1\": 0, \"q2\": \"Apa warna bulu burung?\", \"o2\": [\"Merah 🔴\", \"Biru 🔵\"], \"a2\": 1}"
            items.add(LearningItem(stageId = 6, text = "Ada burung di atas pohon. Bulunya warna biru yang cantik. Burung itu suka bernyanyi.", extraData = p4Extra))

            val p5Extra = "{\"q1\": \"Ke mana keluarga Rina pergi?\", \"o1\": [\"Gunung ⛰️\", \"Pantai 🏖️\"], \"a1\": 1, \"q2\": \"Apa yang ayah lakukan?\", \"o2\": [\"Baca buku 📖\", \"Tidur 😴\"], \"a2\": 0}"
            items.add(LearningItem(stageId = 6, text = "Keluarga Rina pergi ke pantai. Rina main pasir di pinggir laut. Ayah membaca buku cerita.", extraData = p5Extra))
            
            // Rest
            for (i in 7..8) {
                items.add(LearningItem(stageId = i, text = "ta-mat", syllables = "ta-mat"))
            }
            
            dao.insertItems(items)
        }
        
        if (dao.getStoryCount() == 0) {
            val stories = listOf(
                Story(
                    title = "Timun Mas",
                    body = "Dahulu kala, ada seorang janda bernama Mbok Srini yang sangat ingin memiliki anak.\n\nSuatu hari, raksasa memberinya biji timun. Dari dalam timun emas yang besar, lahirlah bayi perempuan cantik bernama Timun Mas.\n\nNamun, raksasa itu berjanji akan kembali mengambil Timun Mas saat ia dewasa. Mbok Srini membekali Timun Mas kantong ajaib.\n\nTimun Mas melempar biji timun yang menjadi hutan, jarum yang menjadi bambu berduri, dan terasi yang menjadi lumpur hidup.\n\nRaksasa pun tenggelam di lumpur, dan Timun Mas hidup bahagia bersama ibunya.",
                    category = "Dongeng Rakyat",
                    sourceAttribution = "Dongeng rakyat Indonesia, domain publik"
                ),
                Story(
                    title = "Si Kancil dan Buaya",
                    body = "Kancil sedang berjalan di hutan. Ia merasa lapar dan ingin makan buah rambutan di seberang sungai.\n\nNamun, sungai itu dipenuhi buaya kelaparan. Kancil pun mendapat ide cerdik.\n\nIa berteriak memanggil buaya dan berkata bahwa raja hutan memintanya menghitung jumlah buaya untuk diberi hadiah daging.\n\nPara buaya segera berbaris di sungai. Kancil melompat dari satu punggung buaya ke punggung buaya lainnya sambil berhitung.\n\nSesampainya di seberang, Kancil tertawa dan berterima kasih lalu pergi memakan rambutan.",
                    category = "Dongeng Rakyat",
                    sourceAttribution = "Dongeng rakyat Indonesia, domain publik"
                ),
                Story(
                    title = "Bawang Merah Bawang Putih",
                    body = "Bawang Putih adalah gadis yang baik hati dan rajin. Bawang Merah adalah saudara tirinya yang pemalas dan sombong.\n\nSuatu hari, selendang Bawang Merah hanyut di sungai. Bawang Putih mencarinya dan bertemu nenek tua.\n\nNenek tua itu meminta Bawang Putih membantunya. Bawang Putih bekerja dengan rajin dan diberi hadiah labu kecil.\n\nSaat dibuka, labu itu berisi emas. Bawang Merah ikut menemui nenek itu tapi ia malas. Ia memilih labu besar yang ternyata berisi ular.\n\nBawang Merah dan ibunya pun menyesal atas perbuatan buruk mereka.",
                    category = "Dongeng Rakyat",
                    sourceAttribution = "Dongeng rakyat Indonesia, domain publik"
                ),
                Story(
                    title = "Keong Emas",
                    body = "Putri Candra Kirana disihir menjadi keong emas oleh penyihir jahat dan dibuang ke sungai.\n\nSeorang nenek baik hati menemukan keong emas itu dan membawanya pulang. Keesokan harinya, rumah nenek itu penuh dengan makanan lezat.\n\nNenek itu penasaran dan mengintip. Ia melihat keong emas berubah menjadi putri cantik.\n\nNenek segera memecahkan cangkang keong itu agar putri tidak bisa kembali menjadi keong.\n\nKutukan pun patah, dan putri kembali ke istana hidup bahagia.",
                    category = "Dongeng Rakyat",
                    sourceAttribution = "Dongeng rakyat Indonesia, domain publik"
                ),
                Story(
                    title = "Kancil Mencuri Mentimun",
                    body = "Pak Tani sedang kesal karena kebun mentimunnya selalu rusak. Ia lalu membuat orang-orangan sawah yang diolesi getah lengket.\n\nKancil yang lapar datang ke kebun. Ia melihat orang-orangan itu dan mencoba menendangnya.\n\nNamun, kaki Kancil malah menempel! Kancil menangis dan meminta tolong saat Pak Tani datang menangkapnya.\n\nPak Tani memaafkan Kancil dan menasihatinya agar tidak mencuri lagi. Kancil berjanji akan menjadi anak yang baik.",
                    category = "Dongeng Rakyat",
                    sourceAttribution = "Dongeng rakyat Indonesia, domain publik"
                )
            )
            dao.insertStories(stories)
        }
    }
}
