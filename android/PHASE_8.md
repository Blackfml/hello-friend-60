# Fase 8 — memória, configurações, segurança e resiliência

## Entregue

- 14 personalidades configuráveis, incluindo Estrategista.
- Personalidade aplicada ao system instruction sem poder substituir regras de segurança.
- Gemini model, temperatura e limite de saída passam a respeitar a configuração persistida.
- API Key permanece protegida pelo Android Keystore.
- Memória persistente local com DataStore, limitada às últimas 100 entradas.
- Memória pode ser ativada/desativada e apagada pelo usuário.
- Configuração de voz e fala automática de respostas digitadas.
- Interface de configurações dentro do JARVIS.
- Timeout de 95 segundos por etapa.
- Até 3 tentativas apenas para geração do Gemini antes de executar ferramentas.
- Backoff progressivo entre tentativas.
- Ações perigosas continuam atrás do ConfirmationManager.
- Nenhum segredo deve aparecer em logs.
- Compilação planejada para Termux sem depender do Android Studio.

## Comandos de memória

A UI já permite adicionar e apagar memórias. A interpretação natural de frases como "Jarvis, lembre disso" pode ser ligada ao ToolRegistry em uma próxima iteração, mantendo a mesma autorização do usuário.

## Validação no Termux

```bash
cd android
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Se o wrapper ainda não tiver sido gerado:

```bash
gradle wrapper --gradle-version 9.1.0
chmod +x gradlew
```

O APK debug fica em `app/build/outputs/apk/debug/app-debug.apk`.

## Pendências antes da versão 1.0

- gerar e versionar o Gradle Wrapper completo;
- testar em aparelho físico;
- validar Android 26+;
- implementar a ponte WhatsApp concreta dentro das limitações reais do Android/WhatsApp;
- adicionar testes instrumentados para Accessibility e MediaProjection;
- configurar assinatura release fora do repositório e sem colocar keystore/senhas no Git;
- executar auditoria final de permissões.
