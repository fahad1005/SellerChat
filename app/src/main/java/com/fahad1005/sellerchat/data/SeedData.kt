package com.fahad1005.sellerchat.data

/** The 12 first-launch seed templates, verbatim from the product spec §5.
 *  They are ordinary rows once seeded: editable and deletable like any
 *  user template. To change them, edit here (see README). */
object SeedData {
    fun templates(): List<Template> = listOf(
        // ---- English ----
        Template(
            title = "Price inquiry reply",
            body = "Hi {name}! Thanks for reaching out. The {item} is Rs {price}. Would you like to place an order?",
            language = TemplateLanguage.EN,
            sortOrder = 0
        ),
        Template(
            title = "Order confirmation",
            body = "Hi {name}, your order for {item} is confirmed. Total: Rs {price}. I'll share the delivery details shortly.",
            language = TemplateLanguage.EN,
            sortOrder = 1
        ),
        Template(
            title = "Payment details",
            body = "Hi {name}, please send Rs {price} to [your payment method]. Share the screenshot here and I'll confirm your order.",
            language = TemplateLanguage.EN,
            sortOrder = 2
        ),
        Template(
            title = "Delivery update",
            body = "Hi {name}, your order has been dispatched. Expected delivery in 2–4 working days. Tracking: [insert].",
            language = TemplateLanguage.EN,
            sortOrder = 3
        ),
        Template(
            title = "Out of stock",
            body = "Hi {name}, sorry — {item} is currently out of stock. It will restock in [X] days. Want me to reserve one for you?",
            language = TemplateLanguage.EN,
            sortOrder = 4
        ),
        Template(
            title = "Thanks after order",
            body = "Thank you for your order, {name}! If you're happy with it, a review would mean a lot. Message me anytime.",
            language = TemplateLanguage.EN,
            sortOrder = 5
        ),
        // ---- Urdu (script) ----
        Template(
            title = "قیمت کا جواب",
            body = "السلام علیکم {name}! آپ کے پیغام کا شکریہ۔ {item} کی قیمت {price} روپے ہے۔ آرڈر کرنا چاہیں گے؟",
            language = TemplateLanguage.UR,
            sortOrder = 6
        ),
        Template(
            title = "آرڈر کنفرم",
            body = "السلام علیکم {name}، آپ کا {item} کا آرڈر کنفرم ہو گیا ہے۔ کل رقم {price} روپے ہے۔",
            language = TemplateLanguage.UR,
            sortOrder = 7
        ),
        Template(
            title = "ادائیگی کی تفصیل",
            body = "السلام علیکم {name}، براہِ کرم {price} روپے [payment method] پر بھیجیں اور اسکرین شاٹ یہاں شیئر کریں۔",
            language = TemplateLanguage.UR,
            sortOrder = 8
        ),
        // ---- Roman Urdu ----
        Template(
            title = "Price reply (Roman)",
            body = "Assalam o Alaikum {name}! Message karne ka shukriya. {item} ki price Rs {price} hai. Order karna chahenge?",
            language = TemplateLanguage.ROMAN_UR,
            sortOrder = 9
        ),
        Template(
            title = "Order confirm (Roman)",
            body = "Assalam o Alaikum {name}, aap ka {item} ka order confirm ho gaya hai. Total Rs {price} hai.",
            language = TemplateLanguage.ROMAN_UR,
            sortOrder = 10
        ),
        Template(
            title = "Payment details (Roman)",
            body = "Assalam o Alaikum {name}, please Rs {price} [payment method] par bhejein aur screenshot yahan share karein.",
            language = TemplateLanguage.ROMAN_UR,
            sortOrder = 11
        )
    )
}
