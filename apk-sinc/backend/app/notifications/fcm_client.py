"""Envio de notificacoes push via Firebase Cloud Messaging.

Nunca colocar credenciais administrativas no APK ou no codigo fonte. O
service account (`firebase-adminsdk.json`) e lido de um caminho configurado
via `.env` (FIREBASE_CREDENTIALS_PATH) e deve ficar fora do controle de
versao (ver .gitignore).

Se FCM_DRY_RUN=true (ou o arquivo de credenciais nao existir), o envio real
e substituido por um log estruturado - isso permite rodar o backend inteiro
sem depender de um projeto Firebase configurado.

O envio e feito por token individual (dispositivos registrados via
POST /api/devices, armazenados na tabela `devices` do Supabase/SQLite), e
tambem publicado no topico de fallback (FCM_TOPIC) para compatibilidade
com clientes que apenas assinem o topico sem registrar token.
"""

import logging
import os

from app.config import get_settings
from app.models.domain import CRITICO

logger = logging.getLogger("sinc.fcm")

_firebase_app = None


def _get_firebase_app():
    global _firebase_app
    if _firebase_app is not None:
        return _firebase_app

    import firebase_admin
    from firebase_admin import credentials

    settings = get_settings()
    cred = credentials.Certificate(settings.firebase_credentials_path)
    _firebase_app = firebase_admin.initialize_app(cred)
    return _firebase_app


def _build_notification(a7_code: str, business_unit: str, new_status: str) -> tuple[str, str]:
    if new_status == CRITICO:
        title = "🔴 UNIDADE CRÍTICA"
        body = f"Unidade: {business_unit} ({a7_code})\nSincronização crítica - fora do prazo esperado."
    elif new_status == "ATENCAO":
        title = "🟡 UNIDADE EM ATENÇÃO"
        body = f"Unidade: {business_unit} ({a7_code})\nSincronização atrasada - acompanhe."
    else:
        title = "🟢 UNIDADE NORMALIZADA"
        body = f"Unidade: {business_unit} ({a7_code})\nA sincronização voltou ao normal."
    return title, body


class FcmClient:
    def __init__(self):
        self.settings = get_settings()

    def _dry_run_active(self) -> bool:
        return self.settings.fcm_dry_run or not os.path.exists(self.settings.firebase_credentials_path)

    def send_test(self, device_tokens: list[str] | None = None) -> bool:
        """Notificacao de teste disparada manualmente pela pessoa em
        Configuracoes -> Testar notificacao - confirma que o caminho
        completo (backend -> FCM -> celular) esta funcionando, sem precisar
        esperar uma mudanca de status real acontecer."""
        title = "🔔 Notificação de teste"
        body = "Se você recebeu isso, as notificações do OMG SINC estão funcionando."
        data = {"type": "test"}
        tokens = device_tokens or []

        if self._dry_run_active():
            logger.info("[FCM dry-run] teste tokens=%d title=%r body=%r", len(tokens), title, body)
            return True

        from firebase_admin import messaging

        _get_firebase_app()
        notification = messaging.Notification(title=title, body=body)
        success = True
        for token in tokens:
            try:
                messaging.send(messaging.Message(notification=notification, data=data, token=token))
            except Exception:
                logger.exception("Falha ao enviar notificacao de teste para o token %s", token[:12])
                success = False
        return success

    def send_status_change(
        self, a7_code: str, business_unit: str, new_status: str, device_tokens: list[str] | None = None
    ) -> bool:
        title, body = _build_notification(a7_code, business_unit, new_status)
        data = {"a7Code": a7_code, "businessUnit": business_unit, "status": new_status}
        tokens = device_tokens or []

        if self._dry_run_active():
            logger.info(
                "[FCM dry-run] topic=%s tokens=%d title=%r body=%r data=%s",
                self.settings.fcm_topic,
                len(tokens),
                title,
                body,
                data,
            )
            return True

        from firebase_admin import messaging

        _get_firebase_app()
        notification = messaging.Notification(title=title, body=body)
        success = True

        for token in tokens:
            try:
                messaging.send(messaging.Message(notification=notification, data=data, token=token))
            except Exception:
                logger.exception("Falha ao enviar notificacao FCM para o token %s", token[:12])
                success = False

        try:
            messaging.send(messaging.Message(notification=notification, data=data, topic=self.settings.fcm_topic))
        except Exception:
            logger.exception("Falha ao enviar notificacao FCM para o topico %s", self.settings.fcm_topic)
            success = False

        return success
