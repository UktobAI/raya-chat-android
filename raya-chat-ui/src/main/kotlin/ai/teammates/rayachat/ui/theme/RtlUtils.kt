package ai.teammates.rayachat.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection

/** Get text alignment based on RTL state. */
fun textAlign(isRTL: Boolean): TextAlign =
    if (isRTL) TextAlign.Right else TextAlign.Left

/** Get horizontal arrangement for row direction. */
fun rowArrangement(isRTL: Boolean, isUserMessage: Boolean): Arrangement.Horizontal =
    if (isUserMessage) {
        if (isRTL) Arrangement.Start else Arrangement.End
    } else {
        if (isRTL) Arrangement.End else Arrangement.Start
    }

/** Get layout direction for Compose. */
fun layoutDirection(isRTL: Boolean): LayoutDirection =
    if (isRTL) LayoutDirection.Rtl else LayoutDirection.Ltr

/** Get horizontal alignment for message bubbles. */
fun messageAlignment(isUser: Boolean, isRTL: Boolean): Alignment.Horizontal =
    if (isUser) {
        if (isRTL) Alignment.Start else Alignment.End
    } else {
        if (isRTL) Alignment.End else Alignment.Start
    }
