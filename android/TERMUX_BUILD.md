# JARVIS Android — build no Termux

O projeto Android fica em `android/` e não depende do Android Studio para o build.

## Requisitos

- JDK 17
- Android SDK com platform 36 e Build Tools 36.0.0
- Gradle 9.1.0
- conexão com a internet na primeira resolução das dependências

O AGP 9.0 exige no mínimo Gradle 9.1.0. Consulte a tabela oficial do Android Developers antes de trocar versões.

## Gerar o Gradle Wrapper

Se o repositório ainda não tiver `gradlew`/wrapper JAR, dentro de `android/`:

```bash
gradle wrapper --gradle-version 9.1.0
```

Depois:

```bash
chmod +x gradlew
./gradlew --version
```

## Verificar

```bash
./gradlew :app:testDebugUnitTest
```

## Compilar APK debug

```bash
./gradlew :app:assembleDebug
```

APK esperado:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Instalar no aparelho

Com ADB configurado:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Observação

Não coloque a Gemini API Key no Gradle, Git, código-fonte ou logs. O JARVIS usa armazenamento protegido no Android para a chave.

## Checklist da Fase 7

- [x] contrato da ponte local
- [x] estados de conexão
- [x] ferramentas de status/conexão/envio
- [x] confirmação antes de comunicação externa
- [x] agendamento via WorkManager
- [x] watcher de mensagens
- [x] agente WhatsApp
- [x] comportamento honesto quando a ponte não está configurada
- [ ] implementação concreta da ponte local
