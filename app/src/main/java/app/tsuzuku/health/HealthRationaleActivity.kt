package app.tsuzuku.health

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

/**
 * What Health Connect opens when someone asks why Kinmichi wants health data (required on
 * Android 13 and below before it will even show the permission screen): the privacy page.
 */
class HealthRationaleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        finish()
    }

    private companion object {
        const val PRIVACY_URL = "https://github.com/sametackgz41/kinmichi/blob/main/PRIVACY.md"
    }
}
