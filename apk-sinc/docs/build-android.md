# Build do Android e geração do APK

## Status deste repositório

O projeto Android (`android/`) está **completo e pronto para abrir no
Android Studio**: Gradle Kotlin DSL, wrapper (`gradlew`) já gerado,
Jetpack Compose + Material 3, MVVM, Retrofit, Room, Firebase Messaging.

**A geração real do `.apk` não pôde ser executada dentro do sandbox usado
para construir este projeto**, porque:

1. Não há Android SDK instalado nesse ambiente.
2. O download do SDK (`dl.google.com`, usado pelo `sdkmanager`) é bloqueado
   pela política de egress do sandbox (`403 Forbidden` ao testar).
3. Pela mesma razão, os repositórios Maven do Google
   (`dl.google.com/dl/android/maven2` via `google()` no Gradle) também não
   são alcançáveis dali, então nem o `gradle build` consegue baixar o
   Android Gradle Plugin.

Isso é uma limitação do ambiente de desenvolvimento remoto, não do projeto.
Em qualquer máquina com Android Studio instalado normalmente (que já traz o
SDK e acesso aos repositórios do Google), o build funciona com os passos
abaixo.

## Pré-requisitos (máquina do desenvolvedor)

- Android Studio Koala (2024.1) ou mais recente
- JDK 17 (o Android Studio já traz um embutido)
- Android SDK Platform 34 + Build-Tools (instalados via SDK Manager do
  Android Studio na primeira abertura do projeto)

## Passo a passo

1. Abra a pasta `android/` no Android Studio (`File → Open`).
2. Deixe o Android Studio baixar/sincronizar o Gradle e as dependências
   (primeira sincronização pode demorar alguns minutos).
3. Copie `android/local.properties.example` para `android/local.properties`
   e ajuste `sdk.dir` para o caminho do seu SDK (o Android Studio geralmente
   já cria esse arquivo automaticamente).
4. Configure o Firebase: siga `docs/setup-firebase.md` e coloque
   `android/app/google-services.json`.
5. Ajuste, se necessário, `BuildConfig.API_BASE_URL` em
   `android/app/build.gradle.kts` (por padrão aponta para
   `http://10.0.2.2:8000/`, que é o endereço do `localhost` da máquina host
   quando acessado a partir do emulador Android).

### Build via linha de comando

```bash
cd android
./gradlew assembleDebug
```

O APK debug é gerado em:
```
android/app/build/outputs/apk/debug/app-debug.apk
```

### Build release (assinado, opcional)

```bash
./gradlew assembleRelease
```

Requer configurar um keystore (não incluído neste repositório por
segurança — `*.keystore` e `*.jks` estão no `.gitignore`). Veja a
documentação oficial de assinatura de apps Android para gerar um keystore
próprio e referenciá-lo em `android/app/build.gradle.kts`
(`signingConfigs`).

### Build via Android Studio

`Build → Build Bundle(s) / APK(s) → Build APK(s)`.

## Rodando localmente

1. Suba o backend (`docs/setup-backend.md`), por padrão em
   `http://localhost:8000`.
2. Rode o app no emulador Android — ele já está configurado para acessar
   `http://10.0.2.2:8000/` (túnel padrão do emulador para o host).
3. Para testar em um dispositivo físico na mesma rede, troque
   `API_BASE_URL` para o IP da máquina rodando o backend
   (ex.: `http://192.168.0.10:8000/`).

## Testes unitários do módulo Android

```bash
cd android
./gradlew testDebugUnitTest
```

(Sujeito à mesma limitação de rede/SDK descrita acima quando executado
neste sandbox — validado apenas por revisão de código e consistência de
referências de recursos, não por execução real do Gradle.)
