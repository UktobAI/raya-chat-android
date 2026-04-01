package ai.teammates.rayachat.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.teammates.rayachat.core.models.BotConfigProps
import ai.teammates.rayachat.core.models.UserInfo
import ai.teammates.rayachat.core.util.validateEmail
import ai.teammates.rayachat.ui.components.common.Header
import ai.teammates.rayachat.ui.components.common.RayaIcons
import ai.teammates.rayachat.ui.components.common.Strings
import ai.teammates.rayachat.ui.theme.LocalRayaTheme

@Composable
internal fun FormScreen(
    botConfig: BotConfigProps,
    statusBarHeight: Int = 0,
    onSubmit: (UserInfo) -> Unit,
    onBack: () -> Unit,
) {
    val theme = LocalRayaTheme.current
    val locale = theme.locale
    val focusManager = LocalFocusManager.current

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf(setOf<String>()) }
    var loading by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        val newErrors = mutableSetOf<String>()
        if (fullName.isBlank()) newErrors.add("fullName")
        if (botConfig.enableUserEmail && email.isNotBlank() && !validateEmail(email.trim())) newErrors.add("email")
        if (botConfig.enableUserEmail && email.isBlank()) newErrors.add("email")
        if (botConfig.enableUserPhone && phone.isBlank()) newErrors.add("phone")
        errors = newErrors
        return newErrors.isEmpty()
    }

    fun handleSubmit() {
        if (!validate()) return
        loading = true
        focusManager.clearFocus()
        onSubmit(UserInfo(fullName.trim(), email.trim(), phone.trim()))
    }

    Column(modifier = Modifier.fillMaxSize().background(theme.background)) {
        // Header with back button
        Header(
            botIcon = botConfig.chatboxChatIcon,
            statusBarHeight = statusBarHeight,
            showBackButton = true,
            showCloseButton = false,
            showBotIcon = false,
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Avatar circle — overlaps into card below, zIndex keeps it above the card
            Box(
                modifier = Modifier
                    .zIndex(1f)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(theme.gradientColor),
                contentAlignment = Alignment.Center,
            ) {
                val userIcon = remember(theme.gradientForeground) { RayaIcons.user(theme.gradientForeground) }
                Icon(userIcon, contentDescription = null, tint = theme.gradientForeground, modifier = Modifier.size(24.dp))
            }

            // Card — overlaps avatar by 22dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-22).dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.formCardBg)
                    .border(1.dp, theme.border, RoundedCornerShape(12.dp))
                    .padding(top = 36.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Welcome text
                Text(
                    Strings.get("welcome_form", locale),
                    fontSize = 14.sp,
                    color = theme.mutedForeground,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                )

                // Full Name
                SimpleInput(
                    value = fullName,
                    onValueChange = { fullName = it; errors = errors - "fullName" },
                    placeholder = Strings.get("full_name", locale),
                    hasError = "fullName" in errors,
                    errorText = Strings.get("error_name", locale),
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                )
                Spacer(Modifier.height(14.dp))

                // Email
                if (botConfig.enableUserEmail) {
                    SimpleInput(
                        value = email,
                        onValueChange = { email = it; errors = errors - "email" },
                        placeholder = Strings.get("email", locale),
                        hasError = "email" in errors,
                        errorText = Strings.get("error_email", locale),
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        onBlurValidate = { if (it.isNotBlank() && !validateEmail(it.trim())) errors = errors + "email" },
                    )
                    Spacer(Modifier.height(14.dp))
                }

                // Phone
                if (botConfig.enableUserPhone) {
                    SimpleInput(
                        value = phone,
                        onValueChange = { phone = it; errors = errors - "phone" },
                        placeholder = Strings.get("phone_number", locale),
                        hasError = "phone" in errors,
                        errorText = Strings.get("error_phone", locale),
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done,
                    )
                    Spacer(Modifier.height(14.dp))
                }

                // Submit button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (loading) theme.gradientColor.copy(alpha = 0.7f) else theme.gradientColor)
                        .clickable(enabled = !loading, onClick = ::handleSubmit),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (loading) Strings.get("starting_chat", locale) else Strings.get("start_the_chat", locale),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = theme.gradientForeground,
                        )
                        if (loading) {
                            Spacer(Modifier.width(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = theme.gradientForeground,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Clean input field matching web widget — no Material3 bloat. */
@Composable
private fun SimpleInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    hasError: Boolean,
    errorText: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    onBlurValidate: ((String) -> Unit)? = null,
) {
    val theme = LocalRayaTheme.current
    val borderColor = if (hasError) theme.errorRed else theme.inputBorder

    Column {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = theme.foreground,
                fontSize = 14.sp,
            ),
            cursorBrush = SolidColor(theme.gradientColor),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp)
                .onFocusChanged { if (!it.isFocused) onBlurValidate?.invoke(value) },
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(placeholder, fontSize = 14.sp, color = theme.mutedForeground)
                    }
                    innerTextField()
                }
            },
        )

        if (hasError) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("⚠", fontSize = 12.sp, color = theme.errorRed)
                Text(errorText, fontSize = 12.sp, color = theme.errorRed)
            }
        }
    }
}
