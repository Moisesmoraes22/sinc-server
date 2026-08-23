package com.apksinc.monitor.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta oficial OMG SINC - identidade fixa, nao decorativa.
// Escuro e a experiencia primaria (Premium Digital Wellness); as cores
// funcionais (accent/success/warning/danger) tem um unico significado em
// todo o app e nao devem ser reaproveitadas como decoracao.

// ---- Superficies (escala de elevacao por luminosidade, sem bordas) ----
val Background = Color(0xFF0A0C10)
val Elevated = Color(0xFF131519)
val Card = Color(0xFF17191E)
val Interactive = Color(0xFF1C1F25)

// ---- Texto ----
val TextPrimary = Color(0xFFF2F3F6)
val TextSecondary = Color(0xFF969BA5)
val TextMuted = Color(0xFF6B7078)

// ---- Linhas discretas de separacao (uso raro - preferir luminosidade) ----
val Hairline = Color(0x12FFFFFF)
val HairlineStrong = Color(0x1FFFFFFF)

// ---- Cores funcionais - significado unico, nunca decorativo ----
val Accent = Color(0xFF2264F8) // CTA, selecao, navegacao ativa, foco, identidade
val AccentSoft = Color(0x262264F8)

val Success = Color(0xFF33D391) // conclusao, progresso positivo
val SuccessSoft = Color(0x2133D391)

val Warning = Color(0xFFEEB443) // atencao, precisa de observacao
val WarningSoft = Color(0x21EEB443)

val Danger = Color(0xFFEF5B60) // erro, acao destrutiva, critico
val DangerSoft = Color(0x24EF5B60)

// ---- Variante clara (mesma identidade, superficies invertidas) ----
val BackgroundLight = Color(0xFFF7F8FA)
val ElevatedLight = Color(0xFFFFFFFF)
val CardLight = Color(0xFFFFFFFF)
val TextPrimaryLight = Color(0xFF11131A)
val TextSecondaryLight = Color(0xFF5B6270)
val TextMutedLight = Color(0xFF8A909C)
val HairlineLight = Color(0x14000000)
