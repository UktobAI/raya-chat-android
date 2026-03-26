package ai.teammates.rayachat.ui.components.common

/** Centralized EN/AR string map. No Android resources — keeps the SDK lightweight. */
object Strings {
    fun get(key: String, locale: String): String {
        val map = if (locale.startsWith("ar")) AR else EN
        return map[key] ?: EN[key] ?: key
    }

    private val EN = mapOf(
        // Intro
        "online_now" to "Online now",
        "start_chat" to "Start a chat",
        "start_conversation" to "Start a new conversation and ask me anything",
        "privacy_note" to "We respect your privacy. Your conversations are encrypted and never shared.",
        "powered_by" to "Powered by ",
        "teammates" to "Teammates.ai",
        // Form
        "welcome_form" to "Welcome to our live chat! Please fill in the form below before starting the chat.",
        "full_name" to "Full Name",
        "email" to "Email",
        "phone_number" to "Phone Number",
        "start_the_chat" to "Start the chat",
        "starting_chat" to "Starting the chat",
        "error_name" to "Please enter your name!",
        "error_email" to "Please enter a valid email address!",
        "error_phone" to "Please enter a valid phone number!",
        // Chat
        "type_message" to "Type your message...",
        "select_option" to "Please select an option above",
        "connect_human" to "Connect with human representative",
        // Commands
        "end_chat_title" to "End Chat Session",
        "end_chat_subtitle" to "Do you want to end this chat session?",
        "cancel" to "Cancel",
        "end_session" to "End Session",
        "skip" to "Skip",
        "submit" to "Submit",
        "characters" to "characters",
        "ending_session" to "Ending session...",
        // Misc
        "voice_message" to "Voice message",
    )

    private val AR = mapOf(
        "online_now" to "متصل الآن",
        "start_chat" to "ابدأ محادثة",
        "start_conversation" to "ابدأ محادثة جديدة واسأل أي شيء",
        "privacy_note" to "نحن نحترم خصوصيتك. محادثاتك مشفرة ولم تتم مشاركتها أبدًا.",
        "powered_by" to "مدعوم من ",
        "teammates" to "Teammates.ai",
        "welcome_form" to "مرحبًا بك في الدردشة المباشرة! يرجى تعبئة النموذج أدناه قبل بدء الدردشة.",
        "full_name" to "الاسم الكامل",
        "email" to "البريد الإلكتروني",
        "phone_number" to "رقم الهاتف",
        "start_the_chat" to "ابدأ الدردشة",
        "starting_chat" to "جاري بدء الدردشة",
        "error_name" to "الرجاء إدخال اسمك!",
        "error_email" to "الرجاء إدخال بريد إلكتروني صحيح!",
        "error_phone" to "الرجاء إدخال رقم هاتف صحيح!",
        "type_message" to "اكتب رسالتك...",
        "select_option" to "يرجى اختيار خيار أعلاه",
        "connect_human" to "تواصل مع ممثل بشري",
        "end_chat_title" to "إنهاء جلسة الدردشة",
        "end_chat_subtitle" to "هل تريد إنهاء جلسة الدردشة هذه؟",
        "cancel" to "إلغاء",
        "end_session" to "إنهاء الجلسة",
        "skip" to "تخطي",
        "submit" to "إرسال",
        "characters" to "حرف",
        "ending_session" to "...جاري إنهاء الجلسة",
        "voice_message" to "رسالة صوتية",
    )
}
