package org.samo_lego.canta.util

import android.os.Parcelable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import java.io.File
import java.net.URL

const val BLOAT_URL =
    "https://raw.githubusercontent.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/main/resources/assets/uad_lists.json"
private const val BLOAT_COMMITS =
    "https://api.github.com/repos/Universal-Debloater-Alliance/universal-android-debloater-next-generation/commits?path=resources%2Fassets%2Fuad_lists.json"

/**
 * Parse commits to get latest commit hash
 */
fun parseLatestHash(commits: String): String {
    val c = commits.substringAfter("\"sha\":\"")
    return c.substringBefore("\"")
}


const val TAG = "BloatUtils"

class BloatUtils {
    fun fetchBloatList(uadList: File): Pair<JSONObject, String> {
        try {
            // Fetch json from BLOAT_URL and parse it
            val response = URL(BLOAT_URL).readText()
            // Parse response to json
            val json = JSONObject(response)

            val commits = URL(BLOAT_COMMITS).readText()

            val hash = parseLatestHash(commits)

            // Write json to file
            uadList.writeText(json.toString())

            LogUtils.i(TAG, "Successfully fetched latest bloat list.")

            return Pair(json, hash)
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to fetch bloat list", e)
            return Pair(JSONObject(), "")
        }
    }

    fun checkForUpdates(latestBloatHash: String): Boolean {
        return try {
            val commits = URL(BLOAT_COMMITS).readText()
            val hash = parseLatestHash(commits)

            hash != latestBloatHash
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to check for updates", e)
            false
        }
    }
}

/**
 * App bloat information, parsed from the UAD json.
 */
@Parcelize
data class BloatData(
    internal val installData: InstallData?,
    internal val description: String?,
    internal val removal: RemovalRecommendation?,
) : Parcelable {
    companion object {
        fun fromJson(json: JSONObject): BloatData {
            val installData = InstallData.byNameIgnoreCaseOrNull(json.getString("list"))
            val description = json.getString("description")
            val removal = RemovalRecommendation.byNameIgnoreCaseOrNull(json.getString("removal"))

            return BloatData(installData, description, removal)
        }
    }
}

/**
 * Enum class to represent the removal recommendation, from the UAD list.
 */
enum class RemovalRecommendation(
    val icon: ImageVector,
    val badgeColor: Color,
    val description: String
) {
    RECOMMENDED(
        Icons.Default.Check,
        Color.Green,
        "Pointless or outright negative packages, and/or apps available through Google Play."
    ),
    ADVANCED(
        Icons.Default.Settings,
        Color.Yellow,
        "Breaks obscure or minor parts of functionality, or apps that aren't easily enabled/installed through Settings/Google Play. This category is also used for apps that are useful (default keyboard/gallery/launcher/music app.) but that can easily be replaced by a better alternative."
    ),
    EXPERT(
        Icons.Default.Warning,
        Color.Red,
        "Breaks widespread and/or important functionality, but nothing important to the basic operation of the operating system. Removing an 'Expert' package should not bootloop the device (unless mentioned in the description) but we can't guarantee it 100%."
    ),
    UNSAFE(
        Icons.Default.Close,
        Color.Magenta,
        "Can break vital parts of the operating system. Removing an 'Unsafe' package have an extremely high risk of bootlooping your device."
    ),
    SYSTEM(
        Icons.Default.Android,
        Color.DarkGray,
        "System apps are apps that come pre-installed with your device."
    );

    companion object {
        fun byNameIgnoreCaseOrNull(input: String): RemovalRecommendation? {
            return entries.firstOrNull { it.name.equals(input, true) }
        }
    }
}

/**
 * Represents the install data from the UAD list.
 */
enum class InstallData {
    OEM,
    CARRIER;

    companion object {
        fun byNameIgnoreCaseOrNull(input: String): InstallData? {
            return entries.firstOrNull { it.name.equals(input, true) }
        }
    }
}
