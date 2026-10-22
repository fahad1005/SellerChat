package com.fahad1005.sellerchat.data

/** Seed templates. The v1 twelve are verbatim from the product spec §5
 *  (v2 only adds their purpose tags). They are ordinary rows once
 *  seeded: editable and deletable like any user template. To change
 *  them, edit here (see README).
 *
 *  v2.2: five bodies were corrected (see [seedCorrections]): the two
 *  order-confirmations dropped "{item}" — "aap ka {item} ka order"
 *  rendered "aap ka ka order" whenever the item was blank — and the
 *  three payment templates write "{payment method}" instead of a
 *  bracketed "[...]" hint, so the fill function's token rule (not raw
 *  brackets) is what the customer-facing cleanup keys on. */
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
            body = "Hi {name}, please send Rs {price} to {payment method}. Share the screenshot here and I'll confirm your order.",
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
            body = "السلام علیکم {name}، آپ کا آرڈر کنفرم ہو گیا ہے۔ کل رقم {price} روپے ہے۔",
            language = TemplateLanguage.UR,
            sortOrder = 7,
            purpose = TemplatePurpose.ORDER
        ),
        Template(
            title = "ادائیگی کی تفصیل",
            body = "السلام علیکم {name}، براہِ کرم {price} روپے {payment method} پر بھیجیں اور اسکرین شاٹ یہاں شیئر کریں۔",
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
            body = "Assalam o Alaikum {name}, aap ka order confirm ho gaya hai. Total Rs {price} hai.",
            language = TemplateLanguage.ROMAN_UR,
            sortOrder = 10,
            purpose = TemplatePurpose.ORDER
        ),
        Template(
            title = "Payment details (Roman)",
            body = "Assalam o Alaikum {name}, please Rs {price} {payment method} par bhejein aur screenshot yahan share karein.",
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

    /**
     * The v2.3 seed pass: 12 global templates, inserted ONCE per
     * install (guarded by SettingsStore.seedsV4Done) after the v2 pass
     * and the v2.2 correction pass. Three each for French, Spanish,
     * Arabic and Chinese (Price / Payment / Delivery), mirroring the
     * existing seeds' structure and placeholders.
     *
     * ALL 12 bodies below are AI drafts written for this build and are
     * PENDING NATIVE REVIEW — the same flag the Tamil / Telugu /
     * Marathi drafts carry. Arabic uses generic amount wording rather
     * than a single market's currency noun; Chinese writes the amount
     * before 元, as a native seller would.
     */
    fun v4Templates(startOrder: Int): List<Template> {
        var order = startOrder
        fun next() = order++
        return listOf(
            // ---- French (AI draft — pending native review) ----
            Template(
                title = "Réponse au prix",
                body = "Bonjour {name} ! Merci pour votre message. Le prix de {item} est de {price} €. Souhaitez-vous passer commande ?",
                language = TemplateLanguage.FR,
                sortOrder = next(),
                purpose = TemplatePurpose.PRICE
            ),
            Template(
                title = "Détails de paiement",
                body = "Bonjour {name}, veuillez envoyer {price} € via {payment method}, puis partager la capture d'écran ici. Je confirmerai votre commande.",
                language = TemplateLanguage.FR,
                sortOrder = next(),
                purpose = TemplatePurpose.PAYMENT
            ),
            Template(
                title = "Suivi de livraison",
                body = "Bonjour {name}, votre commande a été expédiée. Livraison prévue sous 2 à 4 jours ouvrables.",
                language = TemplateLanguage.FR,
                sortOrder = next(),
                purpose = TemplatePurpose.DELIVERY
            ),
            // ---- Spanish (AI draft — pending native review) ----
            Template(
                title = "Respuesta de precio",
                body = "Hola {name}, gracias por tu mensaje. El precio de {item} es {price} €. ¿Quieres hacer un pedido?",
                language = TemplateLanguage.ES,
                sortOrder = next(),
                purpose = TemplatePurpose.PRICE
            ),
            Template(
                title = "Datos de pago",
                body = "Hola {name}, envía {price} € por {payment method} y comparte aquí la captura de pantalla. Confirmaré tu pedido.",
                language = TemplateLanguage.ES,
                sortOrder = next(),
                purpose = TemplatePurpose.PAYMENT
            ),
            Template(
                title = "Actualización de entrega",
                body = "Hola {name}, tu pedido ya fue enviado. La entrega tardará entre 2 y 4 días laborables.",
                language = TemplateLanguage.ES,
                sortOrder = next(),
                purpose = TemplatePurpose.DELIVERY
            ),
            // ---- Arabic (AI draft — pending native review) ----
            Template(
                title = "الرد على السعر",
                body = "مرحبًا {name}! شكرًا لرسالتك. سعر {item} هو: {price}. هل تريد تأكيد الطلب؟",
                language = TemplateLanguage.AR,
                sortOrder = next(),
                purpose = TemplatePurpose.PRICE
            ),
            Template(
                title = "تفاصيل الدفع",
                body = "مرحبًا {name}، يرجى إرسال المبلغ {price} عبر {payment method} ومشاركة لقطة الشاشة هنا. سأؤكد طلبك.",
                language = TemplateLanguage.AR,
                sortOrder = next(),
                purpose = TemplatePurpose.PAYMENT
            ),
            Template(
                title = "تحديث التسليم",
                body = "مرحبًا {name}، تم إرسال طلبك. يصل خلال 2 إلى 4 أيام عمل.",
                language = TemplateLanguage.AR,
                sortOrder = next(),
                purpose = TemplatePurpose.DELIVERY
            ),
            // ---- Chinese (AI draft — pending native review) ----
            Template(
                title = "价格回复",
                body = "您好 {name}！感谢您的咨询。{item}的价格是 {price} 元。您要下单吗？",
                language = TemplateLanguage.ZH,
                sortOrder = next(),
                purpose = TemplatePurpose.PRICE
            ),
            Template(
                title = "付款详情",
                body = "您好 {name}，请通过 {payment method} 支付 {price} 元，并在这里发送付款截图。我会确认您的订单。",
                language = TemplateLanguage.ZH,
                sortOrder = next(),
                purpose = TemplatePurpose.PAYMENT
            ),
            Template(
                title = "配送更新",
                body = "您好 {name}，您的订单已发货，预计 2–4 个工作日送达。",
                language = TemplateLanguage.ZH,
                sortOrder = next(),
                purpose = TemplatePurpose.DELIVERY
            )
        )
    }

    /**
     * The v2.2 seed corrections: old stored body → corrected body, for
     * the five defaults changed in v2.2 (order confirmations #8 Urdu /
     * #11 Roman Urdu; payment templates #3 English / #9 Urdu /
     * #12 Roman Urdu). A stored row is rewritten ONLY when its body
     * still equals the old default byte-for-byte — a template the user
     * has edited is never touched. Fresh installs never see the old
     * texts; they seed the corrected bodies above directly.
     */
    fun seedCorrections(): List<Pair<String, String>> = listOf(
        // #11 — Roman Urdu "Order confirm": drop {item} ("ka ka" fix).
        "Assalam o Alaikum {name}, aap ka {item} ka order confirm ho gaya hai. Total Rs {price} hai." to
            "Assalam o Alaikum {name}, aap ka order confirm ho gaya hai. Total Rs {price} hai.",
        // #8 — Urdu order confirmation: the same {item} doubling, fixed the same way.
        "السلام علیکم {name}، آپ کا {item} کا آرڈر کنفرم ہو گیا ہے۔ کل رقم {price} روپے ہے۔" to
            "السلام علیکم {name}، آپ کا آرڈر کنفرم ہو گیا ہے۔ کل رقم {price} روپے ہے۔",
        // #3 — English payment: [your payment method] → {payment method}.
        "Hi {name}, please send Rs {price} to [your payment method]. Share the screenshot here and I'll confirm your order." to
            "Hi {name}, please send Rs {price} to {payment method}. Share the screenshot here and I'll confirm your order.",
        // #9 — Urdu payment: [payment method] → {payment method}.
        "السلام علیکم {name}، براہِ کرم {price} روپے [payment method] پر بھیجیں اور اسکرین شاٹ یہاں شیئر کریں۔" to
            "السلام علیکم {name}، براہِ کرم {price} روپے {payment method} پر بھیجیں اور اسکرین شاٹ یہاں شیئر کریں۔",
        // #12 — Roman Urdu payment: [payment method] → {payment method}.
        "Assalam o Alaikum {name}, please Rs {price} [payment method] par bhejein aur screenshot yahan share karein." to
            "Assalam o Alaikum {name}, please Rs {price} {payment method} par bhejein aur screenshot yahan share karein."
    )
}
