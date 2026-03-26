package ai.teammates.rayachat.core.util

private val EMAIL_REGEX = Regex(
    "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$"
)

private val PHONE_REGEX = Regex(
    "^[+]?[0-9\\s\\-()]{7,20}$"
)

/** Validates an email address format. */
fun validateEmail(email: String): Boolean =
    email.isNotBlank() && EMAIL_REGEX.matches(email.trim())

/** Validates a phone number format (basic — allows digits, spaces, dashes, parens, optional +). */
fun validatePhone(phone: String): Boolean =
    phone.isNotBlank() && PHONE_REGEX.matches(phone.trim())
