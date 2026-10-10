package com.moneyout.shared.domain

/** Violação de regra ou invariante de domínio. Nunca carrega PII na mensagem. */
open class DomainException(
    message: String,
) : RuntimeException(message)
