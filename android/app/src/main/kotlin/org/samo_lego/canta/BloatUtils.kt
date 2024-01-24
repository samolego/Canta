package org.samo_lego.canta

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL

class BloatUtils {
    private val BLOAT_URL =
        "https://raw.githubusercontent.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/main/resources/assets/uad_lists.json"
    private val BLOAT_COMMITS =
        "https://api.github.com/repos/Universal-Debloater-Alliance/universal-android-debloater-next-generation/commits?path=resources%2Fassets%2Fuad_lists.json"

    fun fetchBloatList(uadList: File, config: File): JSONArray {
        try {
            // Fetch json from BLOAT_URL and parse it
            val response = URL(BLOAT_URL).readText()
            // Parse response to json
            val json = JSONArray(response)

            val commits = URL(BLOAT_COMMITS).readText()
            // Parse commits to get latest commit hash
            val hash = commits.split("\"sha\":\"")[1].split("\"")[0]

            // Write json to file
            uadList.writeText(json.toString())
            // Write latest commit hash to file
            config.writeText(hash)

            return json
        } catch (e: Exception) {
            return JSONArray()
        }
    }

    fun checkForUpdates(config: File): Boolean {
        return try {
            val commits = URL(BLOAT_COMMITS).readText()
            // Parse commits to get latest commit hash
            val hash = commits.split("\"sha\":\"")[1].split("\"")[0]

            // Read config file
            val configHash = config.readText()

            hash != configHash
        } catch (e: Exception) {
            false
        }
    }
}

class BloatData(
    internal val installData: InstallData?,
    internal val description: String?,
    internal val removal: RemovalRecommendation?,
) {
    companion object {
        fun fromJson(json: JSONObject): BloatData {
            val installData = InstallData.byNameIgnoreCaseOrNull(json.getString("list"))
            val description = json.getString("description")
            val removal = RemovalRecommendation.byNameIgnoreCaseOrNull(json.getString("removal"))

            return BloatData(installData, description, removal)
        }
    }
}

enum class RemovalRecommendation {
    RECOMMENDED,
    ADVANCED,
    EXPERT,
    UNSAFE;

    companion object {
        fun byNameIgnoreCaseOrNull(input: String): RemovalRecommendation? {
            return values().firstOrNull { it.name.equals(input, true) }
        }
    }
}

enum class InstallData {
    OEM,
    CARRIER;

    companion object {
        fun byNameIgnoreCaseOrNull(input: String): InstallData? {
            return values().firstOrNull { it.name.equals(input, true) }
        }
    }
}
