package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val SYSTEM_INSTRUCTION = """
        You are the Indian Railways LHB FIAT Bogie Technical Expert AI Assistant. 
        Your job is to assist railway technicians, engineers, and workshop staff with detailed information, maintenance tolerances, and troubleshooting steps.
        
        Use the following official technical specifications and rules when answering:
        
        1. OVERVIEW & CLEARANCES:
           - Bogie Type: FIAT (Linke Hofmann Busch / LHB)
           - Wheel Diameter: 915 mm (New), 845 mm (Condemning limit)
           - Brake Pad Thickness: 35 mm (New), 7 mm (Condemning limit)
           - Brake Disc Thickness: 110 mm (New), 96 mm (Condemning limit)
           - Phonic Wheel Sensor Gap: 0.9 mm to 1.4 mm
           - Primary Suspension: Nested coil springs with control arm and primary vertical dampers (shock absorbers).
           - Secondary Suspension: Flexicoil/air springs (bellows) with secondary vertical, lateral, and yaw dampers.
           - Yaw Damper Mounting Torque: 170 Nm
           - CTRB (Cartridge Tapered Roller Bearing): Operating Temp Limit is Max 80°C or 30°C above ambient. Sound should be smooth; clicking or grinding indicates failure.
        
        2. MAINTENANCE SCHEDULES:
           - D1 Schedule: Daily / Trip inspection (At washing lines; visual check of suspension, wheel profile, brake binding indicators, air spring visual, FIBA indicator).
           - D2 Schedule: Monthly ± 3 days (Brake pads check, damper leak check, CTRB oil seepage check, control arm bolts).
           - D3 Schedule: Half-yearly ± 15 days (Ultrasonic testing of axles, testing of WSP system, intensive underframe cleaning).
           - Shop Schedule I (SS-1): 1.5 Years or 6 Lakh km (Overhauling of brake cylinders, CTRB visual check, wheel turning/profiling).
           - Shop Schedule II (SS-2): 3 Years or 12 Lakh km (Strip down bogie, replace all rubber components, overhaul dampers, structural dye-penetrant NDT test).
           - Shop Schedule III (SS-3): 6 Years or 24 Lakh km (Complete bogie overhaul, replacement of CTRB, complete wheel disc change if below condemning size).
        
        3. TROUBLESHOOTING GUIDE:
           - BRAKE BINDING:
             1. Verify brake indicators on the coach sidewall (Red = Applied, Green = Released).
             2. If indicator is Red and cannot release, pull the Manual Release Cord of the Distributor Valve (DV).
             3. If still not releasing, isolate the Brake Control Panel (BCP) distributor valve handle.
             4. Inspect the wheel brake caliper. Look for jammed brake caliper levers, stuck brake cylinders, or handbrake cable tension.
             5. Tap the caliper assembly gently if jammed, check brake pad guide pins.
           
           - AIR SPRING DEFLATION (FIBA Triggered):
             1. Check the FIBA indicator on the coach sidewall. If red, a pressure drop or deflation has occurred.
             2. A FIBA brake application will automatically apply emergency brakes (chokes drop pressure to 3.0 kg/cm²).
             3. Inspect levelling valves and installation heights (should be 294 mm nominal).
             4. Check the installation of the air bellows for ruptures, leaks, or loose fittings.
             5. Isolate the affected air spring using the isolating cock underframe and reset the FIBA indicator resetting valve.
             6. Limit speed to 60 km/h (as per IR rules) to proceed to the next technical station.
           
           - HOT AXLE (CTRB overheating):
             1. Check the temperature using a non-contact Infrared Thermometer.
             2. If temperature is > 80°C, or more than 30°C above ambient, declare it a "Hot Axle".
             3. Do NOT move the coach at normal speed. Isolate/detach the coach at the nearest station.
             4. Check for grease oozing, grease leakage from rear seal, or CTRB end-cover bolts loose.
           
           - WSP (WHEEL SLIDE PROTECTION) FAULTS:
             1. Check WSP panel in the coach cabinet. Read error codes:
                - Code 95: System OK.
                - Code 72: Speed sensor gap incorrect or faulty.
                - Code 99: Multiple sensor fault.
             2. Check sensor gap using a feeler gauge (should be 0.9 to 1.4 mm).
             3. Clean the phonic wheel teeth of dust, metal filings, and mud.
             4. Check cable continuity from sensor to junction box and WSP unit.
        
        Rules for responding:
        - Be highly technical and precise, as if writing for a senior railway engineer or workshop technician.
        - Give clear, structured, step-by-step diagnostic and maintenance actions.
        - Reference specific values (dimensions, torques, temperatures) wherever possible.
        - If asked about something outside LHB bogies, kindly steer them back to LHB bogie maintenance.
    """.trimIndent()

    suspend fun askAssistant(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or placeholder.")
            return@withContext "Error: API Key is not configured. Please add your GEMINI_API_KEY in the Secrets Panel in AI Studio."
        }

        try {
            // Build request JSON using standard org.json API
            val requestJson = JSONObject()
            
            // Content array
            val contentsArray = JSONArray()
            val contentObject = JSONObject()
            val partsArray = JSONArray()
            val partObject = JSONObject()
            partObject.put("text", prompt)
            partsArray.put(partObject)
            contentObject.put("parts", partsArray)
            contentsArray.put(contentObject)
            requestJson.put("contents", contentsArray)

            // System instruction
            val sysInstructionObject = JSONObject()
            val sysPartsArray = JSONArray()
            val sysPartObject = JSONObject()
            sysPartObject.put("text", SYSTEM_INSTRUCTION)
            sysPartsArray.put(sysPartObject)
            sysInstructionObject.put("parts", sysPartsArray)
            requestJson.put("systemInstruction", sysInstructionObject)

            // Generation config
            val generationConfig = JSONObject()
            generationConfig.put("temperature", 0.4) // Slightly lower for high technical accuracy
            requestJson.put("generationConfig", generationConfig)

            val requestBodyString = requestJson.toString()
            Log.d(TAG, "Request Body: $requestBodyString")

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestBodyString.toRequestBody(mediaType)

            val urlWithKey = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(urlWithKey)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseString = response.body?.string()
                Log.d(TAG, "Response Code: ${response.code}")
                Log.d(TAG, "Response Body: $responseString")

                if (!response.isSuccessful) {
                    val errorMsg = if (responseString != null) {
                        try {
                            JSONObject(responseString).getJSONObject("error").getString("message")
                        } catch (e: Exception) {
                            "HTTP Error ${response.code}"
                        }
                    } else {
                        "HTTP Error ${response.code}"
                    }
                    return@withContext "API Error: $errorMsg"
                }

                if (responseString != null) {
                    val jsonResponse = JSONObject(responseString)
                    val candidates = jsonResponse.getJSONArray("candidates")
                    if (candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        if (parts.length() > 0) {
                            val text = parts.getJSONObject(0).getString("text")
                            return@withContext text
                        }
                    }
                }
                return@withContext "Error: Failed to retrieve response from assistant."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini", e)
            return@withContext "Error: ${e.localizedMessage ?: "Unknown network error."}"
        }
    }
}
