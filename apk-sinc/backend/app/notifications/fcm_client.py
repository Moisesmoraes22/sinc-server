"""Envio de notificacoes push via Firebase Cloud Messaging.

Nunca colocar credenciais administrativas no APK ou no codigo fonte. O
service account (`firebase-adminsdk.json`) e lido de um caminho configurado
via `.env` (FIREBASE_CREDENTIALS_PATH) e deve ficar fora do controle de
versao (ver .gitignore).

Se FCM_DRY_RUN=true (ou o arquivo de credenciais nao existir), o envio real
e substituido por um log estruturado - isso permite rodar o backend inteiro
sem depender de um projeto Firebase configurado.
"""

import logging
import os

from app.config import get_settings

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


class FcmClient:
    def __init__(self):
        self.settings = get_settings()

    def _dry_run_active(self) -> bool:
        return self.settings.fcm_dry_run or not os.path.exists(self.settings.firebase_credentials_path)

    def send_status_change(
        self, server_id: str, server_name: str, new_status: str, reason: str | None = None
    ) -> bool:
        if new_status == "OFFLINE":
            title = "🔴 SERVIDOR OFFLINE"
            body = f"Servidor: {server_name}\nO servidor foi detectado como offline."
        else:
            title = "🟢 SERVIDOR ONLINE"
            body = f"Servidor: {server_name}\nO servidor voltou a responder normalmente."

        data = {"serverId": server_id, "status": new_status}

        if self._dry_run_active():
            logger.info(
                "[FCM dry-run] topic=%s title=%r body=%r data=%s",
                self.settings.fcm_topic,
                title,
                body,
                data,
            )
            return True

        from firebase_admin import messaging

        _get_firebase_app()
        message = messaging.Message(
            notification=messaging.Notification(title=title, body=body),
            data=data,
            topic=self.settings.fcm_topic,
        )
        try:
            messaging.send(message)
            return True
        except Exception:
            logger.exception("Falha ao enviar notificacao FCM para servidor %s", server_id)
            return False
