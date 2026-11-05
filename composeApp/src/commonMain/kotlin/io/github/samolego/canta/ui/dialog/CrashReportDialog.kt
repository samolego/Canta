package io.github.samolego.canta.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.samolego.canta.core.CantaPlatform
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.crash_report_copy
import io.github.samolego.canta.generated.resources.crash_report_description
import io.github.samolego.canta.generated.resources.crash_report_dismiss
import io.github.samolego.canta.generated.resources.crash_report_open_issue
import io.github.samolego.canta.generated.resources.crash_report_title
import io.github.samolego.canta.ui.component.CantaDialog
import io.github.samolego.canta.ui.component.WIDE_DIALOG_WIDTH
import io.github.samolego.canta.util.CrashReport
import org.jetbrains.compose.resources.stringResource

/**
 * Offers the crash report saved by the last run. Opening an issue or copying the
 * report deletes it once the dialog closes; so does dismissing.
 */
@Composable
fun CrashReportDialog(report: String, platform: CantaPlatform, onClose: () -> Unit) {
    CantaDialog(
        onDismissRequest = onClose,
        title = stringResource(Res.string.crash_report_title),
        widthFraction = WIDE_DIALOG_WIDTH,
        buttons = {
            TextButton(onClick = onClose) { Text(stringResource(Res.string.crash_report_dismiss)) }
            TextButton(onClick = {
                platform.copyToClipboard(report)
                onClose()
            }) { Text(stringResource(Res.string.crash_report_copy)) }
            TextButton(onClick = {
                platform.openUrl(CrashReport.issueUrl(report))
                onClose()
            }) { Text(stringResource(Res.string.crash_report_open_issue)) }
        },
    ) {
        Column {
            Text(
                text = stringResource(Res.string.crash_report_description),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = report,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState()),
            )
        }
    }
}
