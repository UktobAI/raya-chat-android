package ai.teammates.rayachat.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import ai.teammates.rayachat.core.Constants
import ai.teammates.rayachat.core.models.BotConfigProps
import ai.teammates.rayachat.core.models.SessionCloseInfo
import ai.teammates.rayachat.core.util.isDarkColor
import ai.teammates.rayachat.ui.components.common.RayaIcons
import ai.teammates.rayachat.ui.components.common.Strings
import ai.teammates.rayachat.ui.theme.LocalRayaTheme
import ai.teammates.rayachat.ui.theme.RayaTypography

@Composable
internal fun IntroScreen(
    botConfig: BotConfigProps,
    sessionCloseInfo: SessionCloseInfo?,
    onStartChat: () -> Unit,
) {
    val theme = LocalRayaTheme.current
    val context = LocalContext.current
    val locale = theme.locale

    val heading = botConfig.chatboxSystemHeading?.ifBlank { null } ?: "Hi there Raya is ready to help ✨"
    val paragraph = botConfig.chatboxSystemParagraph?.ifBlank { null } ?: "Ask any question — Raya is fast and friendly."
    val avatarUrl = botConfig.chatboxChatIcon?.ifBlank { null }

    val isGradientDark = isDarkColor(botConfig.chatboxGradientColor?.ifBlank { null } ?: "#0047AF")
    val headingColor = if (isGradientDark) Color.White else Color(0xFF14161A)
    val paragraphColor = if (isGradientDark) Color.White.copy(alpha = 0.7f) else Color(0xFF585864)

    Column(
        modifier = Modifier.fillMaxSize().background(theme.background)
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
        ) {
            // ── Blue gradient header area ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.gradientColor)
                    .statusBarsPadding()
                    .padding(top = 20.dp, bottom = 60.dp)
                    .padding(horizontal = 24.dp)
            ) {
                // Top row: avatar left, online badge right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Avatar — only shown if chatbox_chat_icon is configured
                    if (avatarUrl != null) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .shadow(4.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(avatarUrl),
                                contentDescription = "Bot avatar",
                                modifier = Modifier.size(36.dp),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    }
                    // Online badge
                    Row(
                        modifier = Modifier
                            .background(theme.onlineBadgeBg, RoundedCornerShape(16.dp))
                            .border(1.dp, theme.onlineBadgeBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(theme.onlineDot))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            Strings.get("online_now", locale),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = theme.onlineText,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Heading
                Text(
                    text = heading,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = headingColor,
                    lineHeight = 32.sp,
                )
                Spacer(Modifier.height(8.dp))
                // Paragraph
                Text(
                    text = paragraph,
                    style = RayaTypography.body,
                    color = paragraphColor,
                    lineHeight = 22.sp,
                )
            }

            // ── White card overlapping the blue area ──
            Column(
                modifier = Modifier
                    .offset(y = (-36).dp)
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .shadow(5.dp, RoundedCornerShape(16.dp))
                    .background(theme.introCard, RoundedCornerShape(16.dp))
                    .border(1.dp, theme.introCardBorder, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                // Session close warning
                if (sessionCloseInfo != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (theme.isDark) Color(0x1AF59E0B) else Color(0xFFFFFBEB),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (theme.isDark) Color(0x4DF59E0B) else Color(0xFFFDE68A),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            RayaIcons.alertTriangle(if (theme.isDark) Color(0xFFFBBF24) else Color(0xFFD97706)),
                            contentDescription = null,
                            tint = if (theme.isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            sessionCloseInfo.message,
                            style = RayaTypography.caption,
                            color = if (theme.isDark) Color(0xFFFBBF24) else Color(0xFF92400E),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Description
                Text(
                    Strings.get("start_conversation", locale),
                    style = RayaTypography.body,
                    color = theme.mutedForeground,
                    textAlign = TextAlign.Start,
                )
                Spacer(Modifier.height(16.dp))

                // Start a chat button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(theme.gradientColor)
                        .clickable(onClick = onStartChat)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            Strings.get("start_chat", locale),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.gradientForeground,
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            RayaIcons.send(theme.gradientForeground),
                            contentDescription = null,
                            tint = theme.gradientForeground,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Privacy note
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        RayaIcons.shieldCheck(theme.mutedForeground),
                        contentDescription = null,
                        tint = theme.mutedForeground,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        Strings.get("privacy_note", locale),
                        style = RayaTypography.small,
                        color = theme.mutedForeground,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // ── Footer ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.footerBg)
                .clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://teammates.ai")))
                }
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = Strings.get("powered_by", locale) + Strings.get("teammates", locale),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = theme.footerText,
            )
        }
    }
}
