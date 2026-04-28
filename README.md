# Loja Manager

App Android (Kotlin/Compose, Material 3, offline-first) para gerenciar a operação de uma pequena loja de roupas: **Estoque**, **Pedidos** (compras a fornecedores), **Vendas**, **Fornecedores** (com matriz de pesquisa de preços) e **Painel** com KPIs em tempo real.

---

## Como compilar

Pré-requisitos:

- **Android SDK** instalado (Android 35 já basta — qualquer versão >= 34 serve).
- **JDK 17+** instalado. O JBR 21 que vem com o Android Studio funciona.
- A pasta `C:\gradle-tmp` precisa existir no Windows (ver “Workaround Windows” abaixo).

Edite `local.properties` (gerado automaticamente; ajuste se precisar) e aponte `sdk.dir` para o seu Android SDK, p.ex.:

```properties
sdk.dir=C:\\Users\\<seu_usuario>\\AppData\\Local\\Android\\Sdk
```

Compile o APK debug:

```bash
./gradlew assembleDebug      # macOS/Linux
gradlew.bat assembleDebug    # Windows (CMD/PowerShell)
```

O APK final fica em:

```
app/build/outputs/apk/debug/app-debug.apk
```

Para rodar os testes unitários (Robolectric + Truth):

```bash
./gradlew testDebugUnitTest
```

---

## Como instalar no celular Samsung

### Caminho A — via cabo USB (ADB)

1. No celular, ative **Modo desenvolvedor** (Configurações → Sobre o telefone → toque 7× em "Número da versão").
2. Em **Opções do desenvolvedor**, ative **Depuração USB**.
3. Conecte o cabo, autorize a chave RSA no celular.
4. No PC:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### Caminho B — sem cabo (recomendado)

1. Copie `app-debug.apk` para o celular (Google Drive, e-mail, USB, ou `adb push`).
2. Abra o **Meus Arquivos** no Samsung, toque no APK.
3. O Samsung vai pedir para **habilitar “Instalar apps desconhecidos”** para o app de origem (Drive / Chrome / Meus Arquivos). Aceite.
4. Confirme a instalação.

> O APK é assinado com o `debug.keystore` automático do Android. Não exige Play Store nem conta Google.

---

## Workaround Windows (importante)

JDK 17+ no Windows usa AF_UNIX para os pipes internos do NIO selector. Quando o caminho do perfil do usuário contém **espaço** (ex.: `C:\Users\Jamesson Ferreira\…`), o `connect` em AF_UNIX falha com `Invalid argument`, derrubando o Gradle com:

```
java.io.IOException: Unable to establish loopback connection
```

A correção é redirecionar o diretório AF_UNIX para um caminho sem espaços. O projeto já injeta isso automaticamente em `gradlew`/`gradlew.bat` (procure pelo bloco "Loja Manager workaround"). Você só precisa garantir que `C:\gradle-tmp` exista (criado automaticamente pelos scripts).

Se preferir resolver no shell:

```powershell
$env:JAVA_TOOL_OPTIONS = "-Djdk.net.unixdomain.tmpdir=C:/gradle-tmp"
```

Também é necessário ter `127.0.0.1 localhost` em `C:\Windows\System32\drivers\etc\hosts` (alguns Windows novos não trazem essa linha).

---

## Funcionalidades

- **Painel**: 6 KPIs em tempo real (peças em estoque, valor estoque a venda, total investido, total vendido, lucro realizado, peças vendidas), top 5 produtos com baixo estoque (≤ 2 unidades) e gráfico de barras Canvas dos últimos 7 dias.
- **Estoque**: lista com badge colorido (verde > 5, amarelo 1–5, vermelho 0), busca por texto, FAB para cadastrar produto. Detalhe com histórico de movimentações (entradas via pedidos Recebidos, saídas via vendas) e edição/exclusão (bloqueia exclusão quando há vínculos).
- **Pedidos**: lista agrupada por status (Pendente/Recebido/Cancelado, headers colapsáveis). Edição com cabeçalho (nº, data, fornecedor, status, observação) e itens via bottom sheet. Mudança para Recebido reflete no estoque automaticamente.
- **Vendas**: lista cronológica com filtros (período + forma de pagamento), card de resumo com receita+lucro+peças. Nova venda valida estoque (qtd ≤ disponível), exige cliente+telefone se Fiado, oferece "Desfazer" pós-salvamento.
- **Fornecedores**: 2 abas — Lista (CRUD com URL clicável) e Pesquisa de Preços (matriz produto×fornecedor com menor preço da linha em verde negrito; toque na célula edita). Botão "Exportar CSV" salva em `Downloads/loja_manager_precos_AAAAMMDD.csv` via MediaStore.
- **Banco offline**: Room v1, seed automático com os dados da planilha original na primeira execução. Sem permissões perigosas, sem internet obrigatória.

---

## Discrepâncias documentadas vs. spec

A Seção 2.5 do `CLAUDE.md` lista valores de referência R$ 1.579,90 (investido) e R$ 60,60 (lucro), mas com o seed da Seção 2 esses valores resultam em **R$ 1.500,00** e **R$ 43,60** respectivamente — diferença que parece erro de transcrição da planilha original. Os testes em `PainelRepositoryTest.kt` validam o valor calculado, não o de referência. Demais valores (41 peças, R$ 1.947,40 estoque, R$ 152,60 vendido, 4 peças vendidas) batem.
