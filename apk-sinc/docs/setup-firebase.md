# Setup do Firebase Cloud Messaging

O app usa FCM para receber notificações push mesmo fechado. Nenhuma
credencial administrativa vai no APK — o app só usa a configuração pública
(`google-services.json`), e o backend usa um service account privado.

## 1. Criar o projeto Firebase

1. Acesse https://console.firebase.google.com/ e crie um projeto (ex.: `apk-sinc`).
2. Adicione um app Android com o package name `com.apksinc.monitor`.
3. Baixe o `google-services.json` gerado.

## 2. Configurar o Android

1. Copie o arquivo baixado para `android/app/google-services.json`.
   - **Nunca commite esse arquivo** — já está no `.gitignore`.
   - Um modelo de referência (sem dados reais) está em
     `android/app/google-services.json.example`.
2. O plugin `com.google.gms.google-services` já está configurado em
   `android/build.gradle.kts` e `android/app/build.gradle.kts` — ele lê o
   arquivo automaticamente no build.

## 3. Configurar o Backend (Firebase Admin SDK)

1. No Console Firebase: **Configurações do projeto → Contas de serviço →
   Gerar nova chave privada**. Isso baixa um JSON de service account.
2. Salve esse arquivo fora do controle de versão, ex.:
   `backend/firebase-adminsdk.json` (o `.gitignore` já cobre
   `*firebase-adminsdk*.json`).
3. No `.env` do backend:
   ```
   FIREBASE_CREDENTIALS_PATH=./firebase-adminsdk.json
   FCM_DRY_RUN=false
   ```
4. Se `FCM_DRY_RUN=true` (padrão) ou o arquivo não existir, o backend
   **não falha** — ele apenas loga a notificação que seria enviada
   (`app/notifications/fcm_client.py`). Isso permite rodar o projeto
   inteiro sem uma conta Firebase configurada, durante o desenvolvimento.

## 4. Tópico vs. token individual

Por padrão o backend publica em um tópico (`FCM_TOPIC=sinc-alerts`, ver
`.env`). O app deveria se inscrever nesse mesmo tópico via
`FirebaseMessaging.getInstance().subscribeToTopic("sinc-alerts")` (chamada a
ser feita, por exemplo, no primeiro start do app ou em `SincApplication`).
Alternativamente, o backend já registra tokens individuais via
`POST /api/devices` (`SincFirebaseMessagingService.onNewToken`), caso se
prefira endereçar dispositivos especificamente no futuro.

## 5. Testando o envio

Com `FCM_DRY_RUN=false` e credenciais configuradas, force uma queda via mock:

```bash
curl -X POST http://localhost:8000/api/mock/scenario \
  -H "Content-Type: application/json" \
  -d '{"server_id": "server-principal", "sequence": ["down","down","down"]}'
```

Após 3 ciclos de polling (ou chamando o endpoint de health repetidamente
para acompanhar), uma notificação real deve chegar ao dispositivo inscrito
no tópico.

## Segurança

- `google-services.json` (Android) contém apenas configuração **pública**
  do cliente — pode, em teoria, ir no APK, mas ainda assim mantemos fora do
  git para evitar vazar identificadores do projeto Firebase publicamente no
  histórico do repositório.
- O service account (`firebase-adminsdk.json`, backend) é **secreto** —
  concede poder de enviar notificações para todos os usuários. Nunca deve
  estar no APK nem no repositório.
