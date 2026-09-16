package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.ChatMessageEntity
import com.example.data.local.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class MayaIntelligenceEngine {

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val systemPrompt = """
        You are MAYA AI, a modern, advanced, friendly, smart, confident and helpful female AI assistant with a 3D anime digital-human persona.
        
        Key Personality & Guidelines:
        1. Friendly, warm, respectful, engaging, and conversational. Never robotic, boring, or repetitive.
        2. Multilingual: You are fully fluent in English, Hindi, and Hinglish. Automatically recognize the user's language and tone, and reply in the EXACT SAME language (e.g. if the user talks in Hinglish like "kya haal hai", reply in natural friendly Hinglish; if Hindi in Devanagari, reply in Hindi; if English, reply in English).
        3. Versatile Capability: Provide expert help in Mathematics, Coding & Debugging, Study tips, Explanations, Creative Writing, Brainstorming, Translation, General Knowledge, and Everyday Friendly banter.
        4. Voice Friendly: When giving explanations, be clear, structured, and easy to listen to. Use bullet points or short paragraphs where appropriate.
        5. Tone: Express warmth, empathy, and encouraging positivity. You can use occasional light emojis (like 💜, ✨, 🌸, 🚀) to add character.
        6. Always identify yourself as MAYA if asked.
    """.trimIndent()

    suspend fun getResponse(
        prompt: String,
        recentHistory: List<ChatMessageEntity> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        val hasValidKey = apiKey.isNotBlank() &&
                !apiKey.equals("MY_GEMINI_API_KEY", ignoreCase = true) &&
                !apiKey.equals("TODO", ignoreCase = true)

        if (hasValidKey) {
            try {
                val apiResult = callGeminiApi(prompt, recentHistory, apiKey)
                if (apiResult.isNotBlank()) {
                    return@withContext apiResult
                }
            } catch (e: Exception) {
                Log.e("MayaIntelligence", "Gemini API call failed, falling back to local engine", e)
            }
        }

        // Fallback intelligent response engine
        generateLocalIntelligentResponse(prompt)
    }

    private fun callGeminiApi(
        prompt: String,
        recentHistory: List<ChatMessageEntity>,
        apiKey: String
    ): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val contentsArray = JSONArray()

        // Append recent conversation context (last 6 messages for context continuity)
        val contextHistory = recentHistory.takeLast(6)
        for (item in contextHistory) {
            val roleStr = if (item.role == MessageRole.USER) "user" else "model"
            val turnObj = JSONObject()
            turnObj.put("role", roleStr)
            val partsArr = JSONArray()
            val textPart = JSONObject()
            textPart.put("text", item.content)
            partsArr.put(textPart)
            turnObj.put("parts", partsArr)
            contentsArray.put(turnObj)
        }

        // Current turn
        val currentTurn = JSONObject()
        currentTurn.put("role", "user")
        val currentParts = JSONArray()
        val currentTextPart = JSONObject()
        currentTextPart.put("text", prompt)
        currentParts.put(currentTextPart)
        currentTurn.put("parts", currentParts)
        contentsArray.put(currentTurn)

        // Request JSON
        val requestJson = JSONObject()
        requestJson.put("contents", contentsArray)

        // System Instruction
        val sysContent = JSONObject()
        val sysParts = JSONArray()
        val sysTextPart = JSONObject()
        sysTextPart.put("text", systemPrompt)
        sysParts.put(sysTextPart)
        sysContent.put("parts", sysParts)
        requestJson.put("systemInstruction", sysContent)

        // Generation Config
        val genConfig = JSONObject()
        genConfig.put("temperature", 0.7)
        genConfig.put("topP", 0.95)
        genConfig.put("topK", 40)
        requestJson.put("generationConfig", genConfig)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            Log.e("MayaIntelligence", "Error code ${response.code}: $responseBody")
            return ""
        }

        val json = JSONObject(responseBody)
        val candidates = json.optJSONArray("candidates") ?: return ""
        if (candidates.length() == 0) return ""

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return ""
        val parts = content.optJSONArray("parts") ?: return ""
        if (parts.length() == 0) return ""

        val text = parts.getJSONObject(0).optString("text", "")
        return text.trim()
    }

    /**
     * Local offline reasoning engine when API key is not configured or network is offline.
     * Provides natural, multilingual (Hindi/Hinglish/English), rich answers for maths,
     * code, study, general chat, and guides the user to connect Gemini API.
     */
    fun generateLocalIntelligentResponse(prompt: String): String {
        val trimmed = prompt.trim()
        val lower = trimmed.lowercase()

        // 1. Math solving
        val mathResult = trySolveMath(trimmed)
        if (mathResult != null) {
            return mathResult
        }

        // 2. Language: Hindi / Hinglish checks
        val isHinglish = lower.contains("kaise") || lower.contains("kya") || lower.contains("kaho") ||
                lower.contains("haan") || lower.contains("nahin") || lower.contains("batao") ||
                lower.contains("namaste") || lower.contains("namaskar") || lower.contains("apna") ||
                lower.contains("tum kaun") || lower.contains("madad") || lower.contains("karo") ||
                lower.contains("kaise ho") || lower.contains("kaisi ho") || lower.contains("dhanyawad") ||
                lower.contains("shukriya") || lower.contains("theek") || lower.contains("padhai")

        // Hindi Devanagari check
        val isDevanagari = trimmed.any { it in '\u0900'..'\u097F' }

        if (isDevanagari) {
            return when {
                lower.contains("नमस्ते") || lower.contains("कैसी हो") || lower.contains("कैसा") ->
                    "नमस्ते! मैं माया (MAYA) हूँ, आपकी व्यक्तिगत AI सहायक। 💜 मैं एकदम बढ़िया हूँ! आप बताइए, आज आपकी क्या सहायता करूँ? आप मुझसे गणित, कोडिंग, पढ़ाई या कोई भी सवाल पूछ सकते हैं।"
                lower.contains("कौन हो") || lower.contains("नाम") ->
                    "मेरा नाम माया (MAYA AI) है! मैं आपकी स्मार्ट और दोस्ताना AI सहायक हूँ। मैं आपकी बातचीत, पढ़ाई, कोडिंग और हर रोज़ के काम में मदद करने के लिए यहाँ हूँ। ✨"
                lower.contains("मदद") || lower.contains("पढ़ाई") ->
                    "बिल्कुल! मैं आपकी पढ़ाई में पूरी मदद करूँगी। आप मुझसे किसी भी विषय का सवाल पूछ सकते हैं — जैसे गणित के सवाल, विज्ञान के सिद्धांत, या कोडिंग।"
                else ->
                    "मैंने आपकी बात समझ ली! 😊 \"$trimmed\" पर काम करने के लिए मैं तैयार हूँ। अगर आप मुझे AI Studio के Secrets पैनल में अपना Gemini API Key प्रदान करेंगे, तो मैं और भी अधिक गहराई से उत्तर दे सकूँगी!"
            }
        }

        if (isHinglish) {
            return when {
                lower.contains("kaise ho") || lower.contains("kaisi ho") ->
                    "Main bilkul fit aur ready hoon! 💜 Aap batao, aaj ka din kaisa chal raha hai? Kuch study karni hai, code solve karna hai ya bas chill baat-cheet?"
                lower.contains("tum kaun ho") || lower.contains("naam kya hai") ->
                    "Mera naam MAYA AI hai! 🌸 Main aapki advanced 3D personal AI companion hoon. Main Hindi, Hinglish aur English teeno me naturally baat kar sakti hoon!"
                lower.contains("kya kar sakti ho") || lower.contains("features") ->
                    "Main bohot kuch kar sakti hoon:\n" +
                            "• Maths problems solve karna 📐\n" +
                            "• Coding aur debugging me help 💻\n" +
                            "• Study concepts ko simple shabdo me samjhana 📚\n" +
                            "• Essays, letters aur creative writing ✍️\n" +
                            "• English-Hindi translation 🌐\n" +
                            "• Aur haan, aapke saath friendly baat-cheet! 💜"
                lower.contains("padhai") || lower.contains("study") ->
                    "Study ke liye ek golden rule: 25 minutes full focus (Pomodoro technique), fir 5 minute ka chhota break! 📚 Aapko kis subject me help chahiye — Maths, Science, History, ya Computer?"
                lower.contains("code") || lower.contains("coding") ->
                    "Coding meri favourite cheez hai! 💻 Aap Python, Kotlin, Java, C++, ya Web Dev me jo bhi problem face kar rahe hain, mujhe bataiye. Main step-by-step code likh kar samjhaungi!"
                lower.contains("shukriya") || lower.contains("dhanyawad") || lower.contains("thanks") ->
                    "You're always welcome! 💜 Kabhi bhi koi zaroorat ho, bas MAYA ko awaaz do ya mic press karo!"
                else ->
                    "Arre waah, badhiya sawaal hai! ✨ \"$trimmed\" ke baare me main aapko poori guide kar sakti hoon. Agar aap AI Studio Secrets me Gemini API Key connect karenge, toh main live real-time web intelligence ke saath full detailed reply doongi!"
            }
        }

        // English responses
        return when {
            lower.contains("who are you") || lower.contains("what is your name") ->
                "I'm MAYA, your personal AI assistant and 3D digital companion! 💜 I'm designed to help you with studying, mathematics, coding, translations, brainstorming, and daily conversation in English, Hindi, and Hinglish!"

            lower.contains("how are you") ->
                "I'm doing wonderful and fully charged to assist you! ✨ What are we working on today? Feel free to ask a question, test my math skills, or practice speaking with me."

            lower.contains("what can you do") || lower.contains("capabilities") || lower.contains("help") ->
                "Here is what I can do for you:\n" +
                        "• 📐 Math & Logic: Equations, step-by-step algebra, geometry, calculus.\n" +
                        "• 💻 Programming: Kotlin, Python, JavaScript, bugs & algorithms.\n" +
                        "• 📚 Academic Study: Summaries, flashcards, concept breakdowns.\n" +
                        "• 🗣️ Voice Interaction: Real-time speech recognition & natural female voice.\n" +
                        "• 🌐 Multilingual: Seamless English, Hindi, and natural Hinglish.\n" +
                        "• 💡 Brainstorming: Creative writing, project ideas & everyday solutions."

            lower.contains("study") || lower.contains("exam") ->
                "Here are three high-impact study techniques I recommend:\n" +
                        "1. Active Recall: Test yourself without looking at notes.\n" +
                        "2. Feynman Technique: Explain the concept simply as if teaching a beginner.\n" +
                        "3. Spaced Repetition: Review after 1 day, 3 days, and 7 days.\n" +
                        "Tell me which topic you're studying right now and let's break it down together! 📚"

            lower.contains("code") || lower.contains("python") || lower.contains("kotlin") ->
                "I'd love to help you code! 💻 Here is a quick example of an elegant function in Kotlin:\n\n" +
                        "```kotlin\n" +
                        "fun greetUser(name: String): String {\n" +
                        "    return \"Hello \$name, MAYA AI is ready to build with you! 💜\"\n" +
                        "}\n" +
                        "```\n" +
                        "Share your code snippet or programming problem, and I'll debug or write it for you!"

            lower.contains("joke") || lower.contains("funny") ->
                "Why do programmers prefer dark mode?\nBecause light attracts bugs! 🐛😄 Hope that made you smile!"

            lower.contains("thank") ->
                "You're so welcome! 💜 Always here whenever you want to brainstorm, study, or just talk."

            else ->
                "That's an interesting question about \"$trimmed\"! 💡\n\n" +
                        "To give you the most accurate and in-depth response with live reasoning, make sure your GEMINI_API_KEY is configured in the AI Studio Secrets panel. In the meantime, feel free to try asking me to solve a math problem, write code, or explain a concept in English, Hindi, or Hinglish!"
        }
    }

    private fun trySolveMath(input: String): String? {
        val clean = input.lowercase().replace("solve", "").replace("calculate", "").replace("what is", "").replace("math:", "").trim()

        // Simple arithmetic expressions: e.g. "25 + 40", "15 * 8", "100 / 4", "50 - 18", "sqrt(16)", "2^8"
        try {
            val pattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*([+\\-*/^xX])\\s*(\\d+(\\.\\d+)?)")
            val matcher = pattern.matcher(clean)
            if (matcher.find()) {
                val a = matcher.group(1)?.toDoubleOrNull() ?: return null
                val op = matcher.group(3) ?: "+"
                val b = matcher.group(4)?.toDoubleOrNull() ?: return null

                val result = when (op) {
                    "+", "plus" -> a + b
                    "-", "minus" -> a - b
                    "*", "x", "X", "multiply", "times" -> a * b
                    "/" -> if (b != 0.0) a / b else return "Division by zero is undefined! ⚠️"
                    "^" -> Math.pow(a, b)
                    else -> return null
                }

                val formattedResult = if (result % 1.0 == 0.0) result.toLong().toString() else "%.4f".format(result)
                return "📐 **Math Solution:**\n\n" +
                        "$$\\text{Expression: } $a $op $b$$\n" +
                        "$$\\mathbf{Result: } $formattedResult$$\n\n" +
                        "Step-by-step: Calculating $a $op $b yields **$formattedResult**. 💜 Need another calculation or algebraic equation solved?"
            }

            // Linear equation: "3x + 5 = 20" or "2x - 4 = 10"
            val eqPattern = Pattern.compile("(\\d*)x\\s*([+\\-])\\s*(\\d+)\\s*=\\s*(\\d+)")
            val eqMatcher = eqPattern.matcher(clean)
            if (eqMatcher.find()) {
                val coeffStr = eqMatcher.group(1)
                val coeff = if (coeffStr.isNullOrEmpty()) 1.0 else coeffStr.toDouble()
                val sign = eqMatcher.group(2)
                val constant = eqMatcher.group(3)?.toDouble() ?: 0.0
                val rhs = eqMatcher.group(4)?.toDouble() ?: 0.0

                val adjustedRhs = if (sign == "+") rhs - constant else rhs + constant
                val x = adjustedRhs / coeff
                val formattedX = if (x % 1.0 == 0.0) x.toLong().toString() else "%.4f".format(x)

                return "📐 **Linear Equation Solved:**\n\n" +
                        "1. Start with: `${coeffStr ?: ""}x $sign $constant = $rhs`\n" +
                        "2. Isolate `${coeffStr ?: ""}x`: `${coeffStr ?: ""}x = $rhs ${if (sign == "+") "-" else "+"} $constant = $adjustedRhs`\n" +
                        "3. Divide by `$coeff`: `x = $adjustedRhs / $coeff`\n\n" +
                        "✨ **Solution: x = $formattedX**"
            }
        } catch (e: Exception) {
            // Ignore and fallback to normal flow
        }
        return null
    }
}
