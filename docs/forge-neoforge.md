# Forge 1.20.1 e NeoForge 1.21.1

Os dois artefatos compilam o mesmo código em `src/main/java`: componentes,
layout, temas, desenho, transições, foco, captura de mouse e playground. Cada
módulo contém somente o bootstrap, os eventos do loader, metadados e as pontes
para APIs diferentes do Minecraft/JEI.

| Ambiente | Minecraft | Loader | JDK da compilação e execução |
| --- | --- | --- | --- |
| `forge` | 1.20.1 | Forge 47.4.20 | 17 |
| `neoforge` | 1.21.1 | NeoForge 21.1.236 | 21 |

O Gradle 8.11.1 roda com JDK 21 e usa toolchains para selecionar o compilador
e o runtime de cada ambiente. No Windows, `scripts/dev.ps1` procura os JDKs
instalados em `.jdks`, `.gradle/jdks`, Java e Eclipse Adoptium. Use
`R_UI_JAVA17` e `R_UI_JAVA21` ou os parâmetros `-Java17` e `-Java21` para
indicar instalações diferentes. Os kits de modding são resolvidos pelos plugins
ForgeGradle e ModDevGradle; não é necessário copiar um MDK manualmente.

## Build e execução

```powershell
.\scripts\dev.ps1 -Task buildAll
.\scripts\dev.ps1 -Task :forge:build
.\scripts\dev.ps1 -Task :neoforge:build
.\scripts\dev.ps1 -Task :forge:runClient
.\scripts\dev.ps1 -Task :neoforge:runClient
```

Também é possível chamar o wrapper diretamente com JDK 21 em `JAVA_HOME` e
JDKs 17/21 detectáveis pelo Gradle. `build` e `buildAll` constroem ambos.
`runForgeClient` e `runNeoForgeClient` são atalhos; `runClient` mantém o atalho
NeoForge antigo. Os servidores usam `:forge:runServer` e `:neoforge:runServer`.
As tarefas equivalentes estão em `.vscode/tasks.json`.

Cada loader usa seus próprios `build/` e `runs/`, incluindo opções, mundos e
layouts JSON. Os dados antigos de `runs/client` permanecem no local original;
copie apenas os layouts desejados para o ambiente correspondente. Nunca abra
um mundo 1.21.1 no Minecraft 1.20.1.

Artefatos:

- `forge/build/libs/r-ui-lib-forge-<version>.jar`: reobfuscado para Forge.
- `neoforge/build/libs/r-ui-lib-neoforge-<version>.jar`: NeoForge.
- Os arquivos `*-sources.jar` correspondentes incluem fontes compartilhadas e
  as pontes da plataforma selecionada.

## Integração de um consumidor Forge

No projeto ForgeGradle do consumidor:

```groovy
dependencies {
    implementation fg.deobf(files('libs/r-ui-lib-forge-0.5.0.jar'))
}
```

Adicione a dependência em `META-INF/mods.toml`, substituindo `your_mod`:

```toml
[[dependencies.your_mod]]
modId="r_ui_lib"
mandatory=true
versionRange="[0.5.0,)"
ordering="AFTER"
side="CLIENT"
```

Para Maven, publique com `:forge:publishToMavenLocal` e consuma
`fg.deobf('com.radaeli.ruilib:r-ui-lib-forge:0.5.0')`. NeoForge mantém
`com.radaeli.ruilib:r-ui-lib-neoforge:0.5.0`.

O código que compõe `UiScreen`, `Select`, `Slider`, campos de texto e os demais
componentes usa a mesma API `com.radaeli.ruilib.ui`. Recompile esse código para
cada Minecraft. Um JAR de tela compilado para 1.21.1/Java 21 não é binariamente
compatível com 1.20.1/Java 17. Itens, recursos ou APIs específicos de 1.21.1
também precisam existir ou receber equivalente no consumidor 1.20.1.

O evento de scroll nativo do Forge possui apenas o delta vertical; a ponte o
entrega ao método compartilhado de quatro argumentos com `scrollX = 0`.
Subclasses podem continuar sobrescrevendo a assinatura compartilhada.
`UiEditBox` preserva o tamanho, seleção por arraste, autoscroll e o relógio de
cursor de 300 ms nas duas versões. A edição e os atalhos continuam delegados
ao editor nativo. O tamanho lógico acompanha a escala tipográfica, evitando
texto fora do campo. JEI 15 e 19 usam pontes de propriedades de GUI
separadas, preservando o mesmo código de ghost targets.

## Validação repetível

```powershell
.\scripts\dev.ps1 -Task :forge:runClient -Validate
.\scripts\dev.ps1 -Task :neoforge:runClient -Validate
.\scripts\dev.ps1 -Task :forge:runClient -Validate -WithJei
.\scripts\dev.ps1 -Task :neoforge:runClient -Validate -WithJei
.\scripts\Compare-UiCaptures.ps1
```

Execute os clientes sequencialmente. O modo opt-in abre uma tela de validação a
partir do menu, usa os widgets/fontes/renderizador reais, grava `results.json`
e PNGs em `<loader>/runs/validation/ui-validation/` e fecha o cliente ao terminar.
Falhas de uma asserção geram `failure.txt` e falham a execução. O comparador
exige a mesma lista de verificações e imagens idênticas pixel a pixel; salva
`build/reports/ui-parity.json`. As capturas de animação usam timestamps fixos.

Use a mesma opção `-WithJei` nos dois loaders para comparar listas equivalentes.
Ela instala JEI 15 no Forge e JEI 19 no NeoForge, e exercita a descoberta dos
ghost targets e a entrega de um item ao filtro.

Os testes JVM em `src/test/java` verificam geometria radial, curvas de animação
e cores. A suíte do cliente cobre callbacks, foco, Unicode, edição, limites,
disabled, sliders/range, toggle/chip, select, scroll nativo, drag de inventário,
modal e toast. Os atlas exercitam os componentes do playground em Dark/Light,
nos estados padrão, interativo e desabilitado. As capturas adicionais cobrem
popup de Select, inventário, ghost slot e transições de modal/toast.
O comparador também exige que o cursor alterne aos 300 ms e retorne ao mesmo
visual aos 600 ms. Há asserções para seleção por arraste, autoscroll em texto
comprido e seleção com escala tipográfica de 150%.

Antes de declarar equivalência completa de um consumidor, valide também com
teclado/mouse reais: Ctrl+A/C/X/V, seleção por Shift e arraste, Home/End,
repetição de teclas, IME, redimensionamento, escalas de GUI, navegação de popups,
JEI com os mods instalados e os itens do próprio mod. Os testes automatizados
não substituem esses cenários de hardware e integração.

Referências oficiais: [Forge 1.20.1](https://docs.minecraftforge.net/en/1.20.1/gettingstarted/)
e [ModDevGradle](https://docs.neoforged.net/toolchain/docs/plugins/mdg/).

## Resultado local em 14/10/2026

- Clientes Forge/Java 17 e NeoForge/Java 21 iniciados e encerrados pela suíte.
- 196 verificações concluídas em cada cliente, com JEI 15/19 instalado.
- 32 pares de PNGs de 1280×960 idênticos pixel a pixel; zero pixels diferentes.
- Período do cursor verificado: visível em 0 ms, oculto em 300 ms, visível em 600 ms.
- Três testes JVM por loader, sem falhas ou testes ignorados.
- JARs inspecionados: 98 classes por loader, 69 fontes compartilhadas idênticas,
  metadados isolados, recursos iguais e bytecode Java 17/21.

Os relatórios locais gerados ficam em `build/reports/ui-parity.json`,
`build/reports/artifacts.json` e nos diretórios `build/test-results/test` dos
loaders. As capturas e as listas de verificações ficam em
`<loader>/runs/validation/ui-validation/`. Esses arquivos de execução são ignorados
pelo Git. A matriz manual acima permanece necessária para declarar cobertura
de IME, atalhos físicos e integrações específicas de um consumidor.
