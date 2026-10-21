package com.fahad1005.sellerchat.data

/** Seed templates. The v1 twelve are verbatim from the product spec §5
 *  (v2 only adds their purpose tags — bodies untouched). They are
 *  ordinary rows once seeded: editable and deletable like any user
 *  template. To change them, edit here (see README). */
object SeedData {
    fun templates(): List<Template> = listOf(
        // ---- English ----
        Template(
            title = "Price inquiry reply",
            body = "Hi {name}! Thanks for reaching out. The {item} is Rs {price}. Would you like to place an order?",
            language = TemplateLanguage.EN,
            sortOrder = 0,
            purpose = TemplatePurpose.PRICE
        ),
        Template(
            title = "Order confirmation",
            body = "Hi {name}, your order for {item} is confirmed. Total: Rs {price}. I'll share the delivery details shortly.",
            language = TemplateLanguage.EN,
            sortOrder = 1,
            purpose = TemplatePurpose.ORDER
        ),
        Template(
            title = "Payment details",
            body = "Hi {name}, please send Rs {price} to [your payment method]. Share the screenshot here and I'll confirm your order.",
            language = TemplateLanguage.EN,
            sortOrder = 2,
            purpose = TemplatePurpose.PAYMENT
        ),
        Template(
            title = "Delivery update",
            body = "Hi {name}, your order has been dispatched. Expected delivery in 2–4 working days. Tracking: [insert].",
            language = TemplateLanguage.EN,
            sortOrder = 3,
            purpose = TemplatePurpose.DELIVERY
        ),
        Template(
            title = "Out of stock",
            body = "Hi {name}, sorry — {item} is currently out of stock. It will restock in [X] days. Want me to reserve one for you?",
            language = TemplateLanguage.EN,
            sortOrder = 4,
            purpose = TemplatePurpose.OTHER
        ),
        Template(
            title = "Thanks after order",
            body = "Thank you for your order, {name}! If you're happy with it, a review would mean a lot. Message me anytime.",
            language = TemplateLanguage.EN,
            sortOrder = 5,
            purpose = TemplatePurpose.OTHER
        ),
        // ---- Urdu (script) ----
        Template(
            title = "قیمت کا جواب",
            body = "السلام علیکم {name}! آپ کے پیغام کا شکریہ۔ {item} کی قیمت {price} روپے ہے۔ آرڈر کرنا چاہیں گے؟",
            language = TemplateLanguage.UR,
            sortOrder = 6,
            purpose = TemplatePurpose.PRICE
        ),
        Template(
            title = "آرڈر کنفرم",
            body = "السلام علیکم {name}، آپ کا {item} کا آرڈر کنفرم ہو گیا ہے۔ کل رقم {price} روپے ہے۔",
            language = TemplateLanguage.UR,
            sortOrder = 7,
            purpose = TemplatePurpose.ORDER
        ),
        Template(
            title = "ادائیگی کی تفصیل",
            body = "السلام علیکم {name}، براہِ کرم {price} روپے [payment method] پر بھیجیں اور اسکرین شاٹ یہاں شیئر کریں۔",
            language = TemplateLanguage.UR,
            sortOrder = 8,
            purpose = TemplatePurpose.PAYMENT
        ),
        // ---- Roman Urdu ----
        Template(
            title = "Price reply (Roman)",
            body = "Assalam o Alaikum {name}! Message karne ka shukriya. {item} ki price Rs {price} hai. Order karna chahenge?",
            language = TemplateLanguage.ROMAN_UR,
            sortOrder = 9,
            purpose = TemplatePurpose.PRICE
        ),
        Template(
            title = "Order confirm (Roman)",
            body = "Assalam o Alaikum {name}, aap ka {item} ka order confirm ho gaya hai. Total Rs {price} hai.",
            language = TemplateLanguage.ROMAN_UR,
            sortOrder = 10,
            purpose = TemplatePurpose.ORDER
        ),
        Template(
            title = "Payment details (Roman)",
            body = "Assalam o Alaikum {name}, please Rs {price} [payment method] par bhejein aur screenshot yahan share karein.",
            language = TemplateLanguage.ROMAN_UR,
            sortOrder = 11,
            purpose = TemplatePurpose.PAYMENT
        )
    )

    /**
     * The v2 seed pass: 12 more templates, inserted ONCE per install
     * (guarded by SettingsStore.seedsV2Done) — for fresh installs and
     * v1 upgraders alike, appended after the existing library order.
     *
     * The 3 Hindi (Hinglish, Roman script) bodies are verbatim from the
     * v2 spec §3C. The 9 Tamil / Telugu / Marathi bodies are AI drafts
     * written for this build — natural and colloquial, same
     * placeholders — and are flagged in BUILD_NOTES as PENDING NATIVE
     * REVIEW before being treated as final copy.
     */
    fun v2Templates(startOrder: Int): List<Template> {
        var order = startOrder
        fun next() = order++
        return listOf(
            // ---- Hindi (Hinglish — verbatim, v2 spec §3C) ----
            Template(
                title = "Price reply (Hindi)",
                body = "Hi {name}! {item} ka price Rs {price} hai. Order karna chahenge?",
                language = TemplateLanguage.HI,
                sortOrder = next(),
                purpose = TemplatePurpose.PRICE
            ),
            Template(
                title = "Payment details (Hindi)",
                body = "Hi {name}, please Rs {price} [payment method] par bhejein aur screenshot share karein.",
                language = TemplateLanguage.HI,
                sortOrder = next(),
                purpose = TemplatePurpose.PAYMENT
            ),
            Template(
                title = "Delivery update (Hindi)",
                body = "Hi {name}, aapka order dispatch ho gaya hai. 2-4 din mein delivery hogi.",
                language = TemplateLanguage.HI,
                sortOrder = next(),
                purpose = TemplatePurpose.DELIVERY
            ),
            // ---- Tamil (AI draft — pending native review) ----
            Template(
                title = "Price reply (Tamil)",
                body = "வணக்கம் {name}! {item} இன் விலை Rs {price}. ஆர்டர் செய்ய விரும்புகிறீர்களா?",
                language = TemplateLanguage.TA,
                sortOrder = next(),
                purpose = TemplatePurpose.PRICE
            ),
            Template(
                title = "Payment details (Tamil)",
                body = "வணக்கம் {name}, தயவுசெய்து Rs {price} [payment method] க்கு அனுப்பி, ஸ்கிரீன்ஷாட்டை இங்கே பகிரவும்.",
                language = TemplateLanguage.TA,
                sortOrder = next(),
                purpose = TemplatePurpose.PAYMENT
            ),
            Template(
                title = "Delivery update (Tamil)",
                body = "வணக்கம் {name}, உங்கள் ஆர்டர் அனுப்பப்பட்டுவிட்டது. 2-4 வேலை நாட்களில் டெலிவரி ஆகும்.",
                language = TemplateLanguage.TA,
                sortOrder = next(),
                purpose = TemplatePurpose.DELIVERY
            ),
            // ---- Telugu (AI draft — pending native review) ----
            Template(
                title = "Price reply (Telugu)",
                body = "హలో {name}! {item} ధర Rs {price}. ఆర్డర్ చేయాలనుకుంటున్నారా?",
                language = TemplateLanguage.TE,
                sortOrder = next(),
                purpose = TemplatePurpose.PRICE
            ),
            Template(
                title = "Payment details (Telugu)",
                body = "హలో {name}, దయచేసి Rs {price} [payment method] కి పంపి, స్క్రీన్‌షాట్ ఇక్కడ షేర్ చేయండి.",
                language = TemplateLanguage.TE,
                sortOrder = next(),
                purpose = TemplatePurpose.PAYMENT
            ),
            Template(
                title = "Delivery update (Telugu)",
                body = "హలో {name}, మీ ఆర్డర్ పంపించబడింది. 2-4 పని దినాల్లో డెలివరీ అవుతుంది.",
                language = TemplateLanguage.TE,
                sortOrder = next(),
                purpose = TemplatePurpose.DELIVERY
            ),
            // ---- Marathi (AI draft — pending native review) ----
            Template(
                title = "Price reply (Marathi)",
                body = "नमस्कार {name}! {item} ची किंमत Rs {price} आहे. ऑर्डर करायची आहे का?",
                language = TemplateLanguage.MR,
                sortOrder = next(),
                purpose = TemplatePurpose.PRICE
            ),
            Template(
                title = "Payment details (Marathi)",
                body = "नमस्कार {name}, कृपया Rs {price} [payment method] वर पाठवा आणि स्क्रीनशॉट इथे शेअर करा.",
                language = TemplateLanguage.MR,
                sortOrder = next(),
                purpose = TemplatePurpose.PAYMENT
            ),
            Template(
                title = "Delivery update (Marathi)",
                body = "नमस्कार {name}, तुमची ऑर्डर पाठवली आहे. 2-4 कामकाजाच्या दिवसांत डिलिव्हरी होईल.",
                language = TemplateLanguage.MR,
                sortOrder = next(),
                purpose = TemplatePurpose.DELIVERY
            )
        )
    }
}
