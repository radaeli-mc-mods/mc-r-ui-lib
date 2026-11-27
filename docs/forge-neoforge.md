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

## Testes automatizados

~~~powershell
.\scripts\dev.ps1 -Task buildAll
.\scripts\dev.ps1 -Task :forge:runClient -Validate -WithJei
.\scripts\dev.ps1 -Task :neoforge:runClient -Validate -WithJei
~~~

Execute os clientes sequencialmente. O modo opt-in usa os widgets e as fontes reais do jogo, executa asserções de comportamento e encerra o cliente automaticamente. O diretório isolado de cada loader é runs/validation; a execução normal continua em runs/client.

Os resultados ficam em results.json dentro de runs/validation/ui-validation. Uma asserção que falha produz failure.txt e falha a execução. A suíte não gera capturas de tela nem compara imagens.

Os testes JVM em src/test/java verificam geometria, curvas, cores e o contrato do tema Workshop. O exemplo de API da documentação é compilado nos dois loaders. O build também verifica os artefatos Java 17/21 e os recursos compartilhados.

A suíte do cliente verifica callbacks, foco, Unicode, seleção e edição de texto, limites, controles desabilitados, sliders/range, toggle/chip, Select, scroll, inventário, modal e toast. O grupo Workshop cobre seleção sem callback em setters, inspeção de controles desabilitados, ellipsis com Unicode, abas laterais, disponibilidade de Stepper, gutter, ordem de Escape e retorno do foco. Os exemplos embutidos são carregados e validados antes de qualquer Save.

Com -WithJei, a suíte usa JEI 15 no Forge e JEI 19 no NeoForge para verificar ghost targets e entrega de itens ao filtro.

Para avaliar aparência e uso no consumidor, abra o playground com teclado e mouse reais. IME, atalhos físicos, resource packs de fonte, escalas de GUI e integrações com outros mods precisam de validação prática.

Referências oficiais: [Forge 1.20.1](https://docs.minecraftforge.net/en/1.20.1/gettingstarted/) e [ModDevGradle](https://docs.neoforged.net/toolchain/docs/plugins/mdg/).
