"""Envio de notificacoes push via Firebase Cloud Messaging.

Nunca colocar credenciais administrativas no APK ou no codigo fonte. O
service account (`firebase-adminsdk.json`) e lido de um caminho configurado
via `.env` (FIREBASE_CREDENTIALS_PATH) e deve ficar fora do controle de
versao (ver .gitignore).

Se FCM_DRY_RUN=true (ou o arquivo de credenciais nao existir), o envio real
e substituido por um log estruturado - isso permite rodar o backend inteiro
sem depender de um projeto Firebase configurado.

Uso no OMG SINC: lembretes de habito (ex.: "hora de beber agua"). O envio e
por token individual (dispositivos registrados via POST /api/devices) e
tambem publicado no topico de fallback (FCM_TOPIC).
"""

import logging
import os

from app.config import get_settings

logger = logging.getLogger("omgsinc.fcm")

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

    def send_habit_reminder(
        self, habit_title: str, message: str, device_tokens: list[str] | None = None
    ) -> bool:
        title = f"✨ {habit_title}"
        data = {"habitTitle": habit_title}
        tokens = device_tokens or []

        if self._dry_run_active():
            logger.info(
                "[FCM dry-run] topic=%s tokens=%d title=%r body=%r data=%s",
                self.settings.fcm_topic,
                len(tokens),
                title,
                message,
                data,
            )
            return True

        from firebase_admin import messaging

        _get_firebase_app()
        notification = messaging.Notification(title=title, body=message)
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
