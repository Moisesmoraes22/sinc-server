package com.apksinc.monitor.util

import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Traduz exceções técnicas (timeout, host desconhecido, HTTP 401...) em
 * mensagens que a pessoa usando o app consegue entender e agir - nunca a
 * mensagem crua da exception (ex.: "SocketTimeoutException: timeout").
 */
fun friendlyErrorMessage(t: Throwable): String = when (t) {
    is SocketTimeoutException ->
        "O servidor demorou demais para responder. Verifique sua internet e tente de novo."
    is UnknownHostException, is ConnectException ->
        "Não foi possível conectar ao servidor. Verifique sua internet e tente de novo."
    is HttpException -> when (t.code()) {
        401 -> "Acesso não autorizado ao servidor. Fale com o suporte."
        in 500..599 -> "O servidor está com problemas no momento. Tente novamente em instantes."
        else -> "Não foi possível atualizar os dados. Tente novamente."
    }
    else -> "Não foi possível atualizar os dados. Tente novamente."
}
