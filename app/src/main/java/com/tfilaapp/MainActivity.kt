package com.tfilaapp

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tfilaapp.TextConstants
import com.tfilaapp.TextConstants.KARAOKE_TEXT
import com.tfilaapp.TextConstants.TARGET_DATE_TIME_ISO
import com.tfilaapp.TextConstants.toLocalDateTime
import com.tfilaapp.ui.theme.TfilaAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TfilaAppTheme {
                TfilaApp()
            }
        }
    }
}

// Global state used by the scheduler / external source logic
object AppSchedulerState {
    // Text from the external source (if any). When present, used instead of TextConstants.KARAOKE_TEXT.
    var externalKaraokeText by mutableStateOf<String?>(null)

    // Target date & time from the external source (if any).
    var targetDateTime: LocalDateTime? by mutableStateOf(null)

    // Flag that, when set to true by the background checker, causes navigation to Page 2.
    var navigateToPage2 by mutableStateOf(false)
}

private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
private var hasStarted = false

/**
 * Called once when the app starts.
 *
 * 1. Calls a function that receives text (and optional date/time) from an external source.
 * 2. If text has arrived, that text is used instead of the hard-coded karaoke text.
 * 3. If date and time have arrived, starts a background checker that:
 *    - checks every day for the correct date,
 *    - and, on that date, checks every minute for the correct time,
 *    - and navigates to Page 2 when both match.
 */
@RequiresApi(Build.VERSION_CODES.O)
fun onStart() {
    Log.e("onStart", "start")
    if (hasStarted) return
    hasStarted = true

    appScope.launch {
        val externalData = fetchExternalDataFromSource()
        if (externalData != null) {

            // If text has arrived, use it instead of the hard-coded text.
            if (externalData.text.isNotBlank()) {
                withContext(Dispatchers.Main) {
                    AppSchedulerState.externalKaraokeText = externalData.text
                }
            }

            // If date and time have arrived, start the background checker.
            val target = externalData.targetDateTime
            Log.e("TargetDateTime", "start")
            Log.e("TargetDateTime", "$target")
            if (target != null) {
                withContext(Dispatchers.Main) {
                    AppSchedulerState.targetDateTime = target
                }

                // Run in background "all the time"
                launch {
                    Log.e("BackgroundCheckerStarted", "$target")
                    runBackgroundScheduleChecker(target)
                }
            }
        }
    }
}

/**
 * Placeholder for the external source call.
 * Replace this implementation to actually fetch data (e.g. from network, database, etc).
 */
@RequiresApi(Build.VERSION_CODES.O)
private suspend fun fetchExternalDataFromSource(): ExternalData? {
    // TODO: Implement real external source.
    // For now, return null to indicate "no external data".
    return ExternalData(KARAOKE_TEXT, toLocalDateTime(TARGET_DATE_TIME_ISO))
}

data class ExternalData(
    val text: String,
    val targetDateTime: LocalDateTime?
)

/**
 * Background loop that checks date once a day and time once a minute on the target date.
 */
@RequiresApi(Build.VERSION_CODES.O)
private suspend fun runBackgroundScheduleChecker(
    targetDateTime: LocalDateTime
) {
    while (true) {
        val now = LocalDateTime.now()
        Log.e("ScheduleCheckerNow", "$now.toLocalTime()")
        Log.e("ScheduleCheckerTarget", "$targetDateTime.toLocalTime()")
        // Check if the correct date has arrived
        if (now.toLocalDate() == targetDateTime.toLocalDate()) {
            // On the correct date: check time every minute
            val nowTime = now.toLocalTime()
            val targetTime = targetDateTime.toLocalTime()

            if (nowTime.hour == targetTime.hour && nowTime.minute == targetTime.minute) {
                withContext(Dispatchers.Main) {
                    AppSchedulerState.navigateToPage2 = true
                }
                break
            }

            // Wait one minute before checking the time again
            delay(60_000)
        } else {
            // Not yet the correct date – wait roughly one day before re-checking
            delay(24L * 60L * 60L * 1_000L)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TfilaApp() {
    var currentPage by remember { mutableStateOf(1) }

    onStart()

    // React to background navigation trigger
    val navigateToPage2Flag = AppSchedulerState.navigateToPage2
    LaunchedEffect(navigateToPage2Flag) {
        if (navigateToPage2Flag) {
            currentPage = 2
        }
    }

    // If external text arrived, use it instead of the hard-coded karaoke text.
    val karaokeTextForPage2 =
        AppSchedulerState.externalKaraokeText ?: TextConstants.KARAOKE_TEXT
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            id = if (currentPage == 1) {
                                R.string.title_welcome
                            } else {
                                R.string.title_countdown
                            }
                        ),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (currentPage) {
                1 -> PageOne(onTestClick = { currentPage = 2 })
                2 -> PageTwo(karaokeText = karaokeTextForPage2)
            }
        }
    }
}


@Composable
fun PageOne(
    onTestClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Temporary hardcoded name
            Text(
                text = TextConstants.USER_NAME,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.size(24.dp))

            Button(onClick = onTestClick) {
                Text(text = stringResource(id = R.string.button_test))
            }
        }
    }
}

@Composable
fun PageTwo(
    karaokeText: String
) {
    val startFrom = 5
    var counter by remember { mutableIntStateOf(startFrom) }
    var showKaraoke by remember { mutableStateOf(false) }

    // Support multiple lines: each line is revealed word by word.
    val lines = karaokeText.split(".")
    var currentLineWords by remember {
        mutableStateOf(lines.first().split(" "))
    }
    var visibleWordsInLine by remember { mutableIntStateOf(0) }

    // Countdown effect
    LaunchedEffect(Unit) {
        for (i in startFrom downTo 0) {
            counter = i
            delay(1_000)
        }
        showKaraoke = true
    }

    // Karaoke effect – start only after countdown is finished
    LaunchedEffect(showKaraoke) {
        if (showKaraoke) {
            for (lineIndex in lines.indices) {
                // Move to a new line: clear previous text
                currentLineWords = lines[lineIndex].split(" ")
                visibleWordsInLine = 0

                for (i in 1..currentLineWords.size) {
                    visibleWordsInLine = i
                    delay(500) // half a second between each word
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!showKaraoke) {
                Text(
                    text = stringResource(id = R.string.countdown_label, counter),
                    style = MaterialTheme.typography.headlineMedium
                )
            } else {
                val visibleText =
                    currentLineWords.take(visibleWordsInLine).joinToString(" ")
                Text(
                    text = visibleText,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun TfilaAppPreview() {
    TfilaAppTheme {
        TfilaApp()
    }
}