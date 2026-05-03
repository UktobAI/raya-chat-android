package ai.teammates.rayachat.sample

/**
 * Bot token for the sample app.
 *
 * Set `rayaChatSampleToken=...` in `local.properties` (gitignored), or export
 * `RAYA_CHAT_SAMPLE_TOKEN` before building. Get a token from the Teammates.ai dashboard.
 */
val SAMPLE_TOKEN: String = BuildConfig.SAMPLE_TOKEN
    .ifEmpty { error("Set rayaChatSampleToken in local.properties or RAYA_CHAT_SAMPLE_TOKEN env var") }
