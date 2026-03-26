package ai.teammates.rayachat.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
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
import ai.teammates.rayachat.ui.theme.RayaTypography

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
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(theme.gradientColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    RayaIcons.user(theme.gradientForeground),
                    contentDescription = null,
                    tint = theme.gradientForeground,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(Modifier.height((-22).dp)) // Overlap into card

            // Card
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .shadow(2.dp, RoundedCornerShape(10.dp))
                    .background(theme.formCardBg, RoundedCornerShape(10.dp))
                    .border(1.dp, theme.border, RoundedCornerShape(10.dp))
                    .padding(horizontal = 28.dp, vertical = 36.dp)
            ) {
                // Welcome text
                Text(
                    Strings.get("welcome_form", locale),
                    style = RayaTypography.body,
                    color = theme.mutedForeground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(20.dp))

                // Full Name
                FormField(
                    value = fullName,
                    onValueChange = { fullName = it; errors = errors - "fullName" },
                    label = Strings.get("full_name", locale),
                    hasError = "fullName" in errors,
                    errorText = Strings.get("error_name", locale),
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                )
                Spacer(Modifier.height(10.dp))

                // Email
                if (botConfig.enableUserEmail) {
                    FormField(
                        value = email,
                        onValueChange = { email = it; errors = errors - "email" },
                        label = Strings.get("email", locale),
                        hasError = "email" in errors,
                        errorText = Strings.get("error_email", locale),
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        onBlurValidate = { if (it.isNotBlank() && !validateEmail(it.trim())) errors = errors + "email" },
                    )
                    Spacer(Modifier.height(10.dp))
                }

                // Phone
                if (botConfig.enableUserPhone) {
                    FormField(
                        value = phone,
                        onValueChange = { phone = it; errors = errors - "phone" },
                        label = Strings.get("phone_number", locale),
                        hasError = "phone" in errors,
                        errorText = Strings.get("error_phone", locale),
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done,
                    )
                    Spacer(Modifier.height(10.dp))
                }

                // Submit button
                Button(
                    onClick = ::handleSubmit,
                    enabled = !loading,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.gradientColor),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        if (loading) Strings.get("starting_chat", locale) else Strings.get("start_the_chat", locale),
                        style = RayaTypography.bodyBold,
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

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    hasError: Boolean,
    errorText: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    onBlurValidate: ((String) -> Unit)? = null,
) {
    val theme = LocalRayaTheme.current
    val borderColor = if (hasError) theme.errorRed else theme.inputBorder

    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, fontSize = 12.sp) },
            isError = hasError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = theme.inputBorderFocused,
                unfocusedBorderColor = borderColor,
                errorBorderColor = theme.errorRed,
                focusedTextColor = theme.foreground,
                unfocusedTextColor = theme.foreground,
                cursorColor = theme.foreground,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .onFocusChanged { if (!it.isFocused) onBlurValidate?.invoke(value) },
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
