package org.samo_lego.canta

import org.json.JSONObject
import java.net.URL

class BloatFetcher {
    private val BLOAT_URL =
        "https://raw.githubusercontent.com/0x192/universal-android-debloater/main/resources/assets/uad_lists.json"
    private val BLOAT_COMMITS =
        "https://api.github.com/repos/0x192/universal-android-debloater/commits?path=resources%2Fassets%2Fuad_lists.json"

    fun fetchBloatList() {
        // Fetch json from BLOAT_URL and parse it
        val response = URL(BLOAT_URL).readText()
        // Parse response to json
        val json = JSONObject(response)

        val commits = URL(BLOAT_COMMITS).readText()
        // Parse commits to get latest commit hash
        val hash = commits.split("\"sha\":\"")[1].split("\"")[0]
    }
}