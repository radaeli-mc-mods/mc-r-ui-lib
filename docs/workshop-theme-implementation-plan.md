# Plano de implementação — tema Workshop da R-Ui Lib

Data: 27/11/2026. Estado: planejado; implementação e aceite visual ainda não iniciados.
Base: R-Ui Lib 0.10.0; Forge 1.20.1/Java 17 e NeoForge 1.21.1/Java 21.

## Objetivo e referência vinculante

Implementar **na própria biblioteca** um tema completo chamado **Workshop**, disponível como `UiTheme.WORKSHOP` e no seletor de temas do playground, ao lado de Dark e Light. Identificador estável: `workshop`.

O nome Workshop remete aos painéis de instrumentos de uma oficina: carcaça cinza quente, molduras em relevo, campos rebaixados e controles verde oliva. Essa combinação observada na proposta define a identidade do tema.

A referência visual é o protótipo aprovado de Better Radio em `../better-radio/docs/design/index.html`, seu desenho em `prototype.js` e a captura `console.png` do mesmo diretório. Consultar também `references.md`: a linguagem foi baseada nas interfaces de Create e nas abas de Sophisticated Backpacks. Antes de implementar, criar uma captura de referência no repositório da lib para que a comparação não dependa de alterações futuras no checkout do consumidor. A captura é referência interna, não textura para desenhar a UI.

O tema terá molduras em pixels, superfícies cinza quente em relevo e rebaixo, tipografia Minecraft em escala nativa, controles compactos e seleção verde oliva. O Better Radio não precisará implementar renderizadores próprios de painel, botão, aba, campo ou scrollbar. Layouts e regras musicais continuam pertencendo ao consumidor; o tema é útil para qualquer mod.

Não alterar áudio, persistência ou código do Better Radio nesta implementação. Não substituir Dark/Light, não definir Workshop como padrão global e não copiar sprites do Create ou atlas da Mojang para o JAR. Usar Font e renderer de itens fornecidos pelo jogo; novos ícones da lib devem ser originais.

## Contrato visual comum a todas as fases

### Paleta de referência

Valores opacos em ARGB, salvo backdrop. Centralizar em papéis do tema/skin, sem constantes copiadas por widget.

| Papel | Valor | Uso |
| --- | --- | --- |
| Panel | `0xFFC6C4BC` | Carcaça principal |
| Outline | `0xFF242323` | Contorno externo de 1 px lógico |
| EdgeLight | `0xFFF5F2E7` | Topo/esquerda do relevo |
| EdgeDark | `0xFF77756F` | Base/direita do relevo |
| InsetLight / InsetDark | `0xFFE3E0D4` / `0xFF585750` | Bordas invertidas do rebaixo |
| TitlePlate | `0xFFE4D6B9` | Faixa discreta de título |
| Display | `0xFF97978B` | Visor e áreas informativas rebaixadas |
| Well | `0xFF8B887E` | Fundo de lista e espaços vazios |
| Row / RowAlternate | `0xFFB2AFA3` / `0xFFA8A59A` | Linhas densas de dados |
| Text / TextMuted | `0xFF403D38` / `0xFF686457` | Texto sobre painéis claros |
| Button / ButtonHover | `0xFF999A90` / `0xFFAAA99B` | Controles em relevo |
| ButtonSelected | `0xFF777D62` | Seleção persistente |
| TextOnButton / TextSelected | `0xFFF5F2E8` / `0xFFDCF3A4` | Contraste nos controles |
| Disabled / TextDisabled | `0xFF92918A` / `0xFF64655F` | Controle indisponível |
| Accent | `0xFFB4C478` | Progresso e pequenos indicadores |
| Focus | `0xFFF8DFA0` | Contorno de foco, distinto da seleção |
| Input / TextOnInput | `0xFF6D6A60` / `0xFFF4EEE1` | Campo de busca e edição |
| Tooltip / TooltipBorder | `0xFF100B19` / `0xFF56406F` | Caixa escura inspirada no tooltip do jogo |
| Overlay | `0x99000000` | Escurecimento atrás do modal |
| Success / Warning / Error | `0xFF6D814C` / `0xFFAA823D` / `0xFF965A48` | Símbolos e detalhes de status; sempre acompanhados de texto |

A paleta do estudo contém mais papéis que UiPalette hoje. Preservar o construtor público atual de UiPalette; acrescentar os papéis de acabamento e de texto sobre superfícies num estilo específico da skin, sem exigir dezenas de novos argumentos de todos os consumidores.

### Geometria e estados

- Coordenadas e bordas em unidades lógicas inteiras; escala aplicada pelo Minecraft. Raios zero para superfícies retangulares. Formas funcionais circulares/radiais serão rasterizadas em pixels, sem serem convertidas em quadrados.
- Frame padrão: contorno externo de 1 px, luz/sombra interna de 1 px e reserva total de 3 px para o conteúdo. Raised usa luz em cima/esquerda; Inset inverte; Plain não pinta frame. TitlePlate é variante explícita, não decoração automática em toda Label.
- Fonte nativa `Font`, todos os papéis tipográficos em escala 1.0. Hierarquia por posição, cor e separação. Não reduzir autor/hint com escala fracionária nem aumentar heading por transformação.
- Gaps 2/4/8; padding total de painel 8 e card 6, incluindo os insets de borda. Não somar duas vezes borda e padding. Variantes compactas podem usar padding 4, nunca abaixo do frame.
- Button padrão: mínimo 20 de altura; IconButton 20×20; compacto explícito 18×18; destaque 24×22. Conteúdo centralizado opticamente em pixels. Medir antes de impor tamanho: rótulos maiores crescem ou truncam com política explícita.
- Itens em slots: imagem natural 16×16, célula inicial 20×20. Linhas de texto simples: mínimo 20; duas linhas nativas: mínimo 22, aumentando se Font exigir. Não mudar dimensões explícitas de documentos existentes.
- Normal: relevo. Hover: superfície clareia. Pressed: bordas invertidas e conteúdo deslocado 1 px; hitbox fica imóvel. Selected: superfície oliva rebaixada e marcador persistente. Focus: contorno interno claro, preservando selected. Disabled: superfície/texto próprios, sem hover/ativação; tooltip explicativo continua disponível.
- Se disabled, suprimir press/hover visual; preservar marcador de seleção sem aparência ativa. Focus é desenhado por cima da seleção, sem encobrir ícone. Nenhum estado muda o tamanho externo do componente.
- Motion zero neste tema: nada de deslizamento de alça, fade de modal ou scroll interpolado. Atualizações numéricas e controle de cursor continuam funcionais. Dark/Light conservam motion atual.
- Sem sombras desfocadas, gradientes decorativos, cápsulas arredondadas, banners de marca ou elementos ornamentais de rádio. Cores de itens e cores escolhidas pelo usuário não recebem filtro oliva.

## Fundação transversal — tema, renderização e playground inicial

**Dependência:** nenhuma. É a base das fases por grupo abaixo.

1. Acrescentar `UiSkin` ao tema, mantendo `new UiTheme(palette, tokens)` com aparência padrão. Se UiTheme ganhar componente no record, manter overloads e auditar igualdade, reflexão e consumidores do playground; não presumir compatibilidade binária total.
2. Introduzir `UiVisualState` e variantes de superfície DEFAULT/RAISED/INSET/PLAIN/TITLE_PLATE. Centralizar as métricas de borda, conteúdo, ícone, scrollbar e popup. Todas as peças de um controle consultam a mesma métrica para medida, desenho e hit testing.
3. Manter primitivas de baixo nível em UiPaint; delegar desenho semântico para skin. A implementação padrão conserva a aparência atual. A implementação Workshop desenha frames procedurais em pixels. Não resolver este trabalho com subclasses de controles no consumidor nem tornando todas as classes não finais.
4. Expor `UiTheme.WORKSHOP`; preservar UiThemes e seleção local/global existentes. Troca de tema deve relayoutar usando a infraestrutura atual, sem manter widgets presos às medidas antigas.
5. Acrescentar **Workshop** ao Theme picker do playground desde esta fundação. Preservar seu funcionamento global atual e exibir nome correto no status; não usar o ternário Light/Dark existente para identificar o terceiro tema. Registrar que a escolha global afeta telas sem override; telas com tema explícito permanecem isoladas.
6. Não alterar o significado do arquivo de layout: o seletor continua uma preferência global da sessão, sem adicionar themeId em PlaygroundDocument. Campos novos dos componentes são opcionais e normalizados, com defaults que preservem os documentos v1 antigos.
7. Adicionar uma visualização interna de amostras dos frames/estados para servir de referência durante a implementação. Todo código público novo usa `com.radaeli.ruilib`.

**Saída:** Workshop selecionável, primitives e regras prontas; componentes ainda pendentes claramente listados no checklist interno. Não declarar o tema completo só porque o seletor existe.

**Verificação:** construtores antigos compilam; Dark/Light conservam capturas; frame Raised/Inset tem pixels e insets corretos; troca de tema não modifica valores nem dispara callbacks.

## Fase 1 — superfícies, texto e layouts

**Dependência:** fundação. Exemplo embutido principal: `layout.json`; amostras de texto também em `basic.json`.

| Componente | Mudança visual obrigatória |
| --- | --- |
| UiScreen | Backdrop escuro uniforme; root continua responsável pela carcaça. Nenhuma janela gigante de largura total acrescentada automaticamente. Modal e toast recebem o mesmo tema da tela. |
| Panel | Moldura Raised por padrão no Workshop; variantes Inset, Plain e TitlePlate. Background override altera o preenchimento, sem descartar frame escolhido. |
| Card | Mesmo frame, compacto, sem sombras; título nativo de 1.0 sobre placa opcional. Hover discreto; seleção e foco seguem o contrato comum. |
| Label | Texto escuro nativo; papéis muted/on-button/on-input e estilo por instância. CLIP, ELLIPSIS e WRAP com limite de linhas; elipse preserva Component/estilos/Unicode e pode mostrar texto completo em tooltip. |
| Divider | Sulco horizontal/vertical de 2 px, sombra e luz. Área de espaçamento continua separada da linha; não desenhar caixas em torno de separadores. |
| Row | Sem superfície própria; gaps compactos do tema, alinhamento inteiro e altura coerente dos filhos. Não esticar apenas o texto por escala para preencher a linha. |
| Column | Sem superfície; mesmos gaps e padding só quando explicitamente solicitado pelo container. |
| Grid | Sem fundo por célula automaticamente; espaçamento inteiro uniforme. Células herdam o acabamento dos próprios controles. |
| Stack | Transparente, sem frame extra; manter ordem de desenho/input e alinhamento inteiro dos filhos. |
| AbsoluteLayout | Transparente; conservar coordenadas lógicas e alinhamento exato. Não aplicar escala do protótipo HTML. |
| Spacer | Invisível; conservar fixed/flex. Não desenhar fundo ou borda em espaços vazios. |

**APIs:** variantes de superfície; papéis de texto; helper de overflow compartilhado, reaproveitável por Select/Table/ItemCard. Manter configuração legada como default onde necessário para não mudar Dark/Light.

**Saída/aceite:** composição de frame, placa de título, texto e rebaixo equivalente à referência. Testar nome longo e PT-BR sem sobreposição; comparar pixels e medidas após troca entre três temas. Containers não podem criar molduras duplicadas.

## Fase 2 — ações e seleções básicas

**Dependência:** fase 1. Exemplo principal: `basic.json`.

| Componente | Mudança visual obrigatória |
| --- | --- |
| Button | Botão cinza compacto Raised; pressed Inset com conteúdo +1 px; texto claro. Mesmo desenho para ativação por mouse ou teclado, sem deslocar hitbox. |
| IconButton | Mesmo frame. Ícone gráfico centralizado em pixels; selected persistente oliva e foco independente. Suportar troca de ícone/nome sem remontar. Manter glyph e cadeados atuais. |
| Toggle | Substituir cápsula por trilho retangular Inset e alça retangular Raised. Duas posições estáticas; ON com pequena marca verde e estado acessível, não só cor. Label permanece fora do trilho. |
| Chip | Opção retangular compacta, sem pill. Normal Raised, selecionada Inset oliva; rótulo com contraste do respectivo estado. |
| Radio | Indicador circular pequeno rasterizado em degraus e centro preenchido quando selecionado; sem antialias. Preservar leitura de seleção exclusiva, label e hint alinhados. Hover discreto na linha, não um botão gigante. |
| InfoBox | Pequena placa quadrada com ? em pixels; hover e foco claros. Adicionar foco por teclado e tooltip ancorado; não transformar a ajuda em ação. |

**APIs:** `UiIcon` com GLYPH e TEXTURE_REGION, tamanhos naturais e tint opcional; helper de blit na ponte de plataforma; setters de ícone/nome e selected em IconButton. SetSelected sincroniza apresentação sem disparar ação. Assets originais para close, confirm, plus, minus, setas, play/pause/previous/next, shuffle, save, history, settings, search e cadeados, disponíveis na lib/playground.

**Saída/aceite:** matriz normal/hover/pressed/selected/focused/disabled, incluindo selected+focused e selected+disabled. Verificar clique e teclado sem callbacks duplicados, estados legíveis e nenhum ícone cortado em 18×18. Cores de texto não podem continuar iguais em botão claro/escuro por acidente.

## Fase 3 — entrada, configuração e edição de cor

**Dependência:** fases 1–2. Exemplo principal: `input.json`; Select também exercitado em `data.json`.

| Componente | Mudança visual obrigatória |
| --- | --- |
| TextField | Frame Inset, preenchimento Input, texto claro, placeholder menos intenso. Cursor e seleção do editor nativo; foco na borda. Reservar insets coerentes com baseline e clipping. |
| SearchField | Mesmo campo com ícone de lupa gráfico em pixels. Reserva fixa à esquerda; query não invade ícone. Reutilizar editor nativo, não simular busca por key events como no HTML. |
| Select | Controle fechado Raised com seta gráfica separada e elipse. Aberto fica visualmente pressionado. Popup Inset/Raised externo com linhas de 20 px, hover localizado, seleção oliva com marca e texto contrastante. Popup usa tema mesmo fora do viewport da lista. |
| Slider | Trilho Inset reto e alça Raised retangular; preenchimento oliva fino. Readout nativo alinhado fora da área de arraste. Seleção dupla usa duas alças distintas, com foco e handle ativo claros. |
| Stepper | Valor numa área Inset e botões pequenos Raised nas extremidades. Setas/minus/plus em pixels; limites desabilitam só o lado indisponível. Label e valor com alinhamento e larguras estáveis. |
| SegmentedControl | Bloco retangular unido, sem gaps que pareçam botões soltos; divisórias internas únicas. Segmento ativo Inset oliva, foco com contorno interno e texto apropriado. |
| RadioGroup | Reutilizar exatamente Radio para indicador, label, hint e estados; espaçamento vertical compacto. Nunca criar estilo de radio diferente dentro do grupo. |
| ColorSwatch | Amostra RGB verdadeira em cavidade quadrada; borda externa selecionada/focada com marca adicional. Não tingir, dessaturar ou escurecer a cor escolhida para indicar disabled: indicar indisponibilidade no frame/tooltip. |
| ColorPicker | Composição do swatch, campo HEX, três sliders RGB e ações Apply/Cancel já existentes, todos com o mesmo tema. Rótulos R/G/B e amostra fiel; inválido sinalizado por texto/borda Error e Apply desabilitado. Sem converter para editor HSV ou mudar o contrato transacional. |

**Saída/aceite:** painel de configurações coeso aberto junto ao popup. Editor com Ctrl+A/C/X/V, Unicode, seleção e cursor; slider por mouse/roda/teclado e intervalo duplo; stepper vazio/limites; cores 000000/FFFFFF/valor inválido; callbacks e Apply/Cancel iguais aos atuais. Parametrizar geometria hoje fixa; desenhar skin sobre controle antigo sem ajustar medida não basta.

## Fase 4 — itens, inventário e dados tabulares

**Dependência:** fases 1–3. Exemplo principal: `data.json`; InventoryGrid deve ganhar demonstração carregável nessa categoria.

| Componente | Mudança visual obrigatória |
| --- | --- |
| SelectableCard | Frame Raised neutro, selected Inset oliva com marca lateral discreta. Filhos interativos conservam o próprio acabamento e input sem disparar seleção do card. |
| ItemCard | Mesmo padrão de ação/card selecionável; miniatura vanilla 16×16 e label nativo com elipse. Reservar primeiro área de item e ação; texto não atravessa borda. |
| ItemSlot | Cavidade Inset com aspecto de slot Minecraft. Item sem filtro visual; placeholder de slot vazio em pixels. Estados de hover/ghost target/seleção/foco distintos; override de cor altera só preenchimento. |
| ItemGrid | Grade de ItemCards/slots conforme o contrato atual; gaps compactos e uniformes, sem moldura nova em cada nível de container. Não trocar semântica de cards por inventário real. |
| InventoryGrid | Slots com a mesma cavidade de ItemSlot, sem o card arredondado atual. Hotbar separada por intervalo/sulco discreto, sem faixa verde translúcida. Hover e seleção do picker visíveis; ícone/quantidade vanilla intactos. |
| ItemPreview | Item vanilla com escala inteira quando possível e sem frame automático. Consumidor compõe Panel Inset para criar a cavidade do protótipo. Tooltip segue camada de overlays. |
| Table | Viewport Inset; cabeçalho Raised discreto, fonte nativa. Linhas alternadas Row/RowAlternate, hover claro, seleção oliva com texto apropriado; divisórias finas em vez de grade pesada. Colunas reservam largura e truncam com tooltip. |
| TableColumn | Nenhum desenho próprio; largura, alinhamento e renderer textual continuam alimentando a tabela. Garantir que headers e células usem os mesmos papéis/overflow. |

**Saída/aceite:** inventário com itens vanilla, contagens, slots vazios e ghost targets; nenhuma alteração real de inventário no modo picker. Verificar JEI opcional, drag e hierarquia Z. Tabela com várias colunas e textos longos sem invadir scrollbar. Manter cores/foil/modelos dos itens e empilhamento vanilla.

## Fase 5 — navegação e viewports

**Dependência:** fases 1–4. Exemplos: Tabs em `data.json`; viewports em `scroll.json`.

| Componente | Mudança visual obrigatória |
| --- | --- |
| Tabs | Manter TOP e acrescentar LEFT/RIGHT; ícone opcional, título acessível e tooltip por aba. No tema Workshop, placas Raised com ativo Inset oliva conectado à carcaça. Suprimir borda compartilhada; sem underline de aplicativo web. |
| ScrollView | Viewport Inset por padrão no novo tema, variante Plain para composição. Trilho rebaixado, alça retangular Raised, cores neutras; não usar pill translúcida. Reservar gutter para não cobrir texto/ações. |
| HorizontalScrollView | Mesmo material e métricas da barra vertical, rotacionados no layout. Área clicável maior que a arte quando necessário, sem mudar a largura do conteúdo ao hover. |
| ClipStack | Transparente; manter clipping e hit testing. Não acrescentar painel só por ser viewport. Restaurar scissor corretamente nos containers aninhados. |

**APIs:** colocação e conteúdo de Tabs; aparência de viewport/barra e gutter. Bounds do composto incluem abas aparentemente externas: UiContainer rejeita cliques fora dos próprios bounds. Reservar também o conjunto inteiro em jeiGuiBounds. Abas inativas não recebem foco; setas cima/baixo nas laterais, esquerda/direita em TOP.

**Saída/aceite:** janela genérica com reprodução demonstrativa no topo, abas à direita e lista abaixo, reproduzindo as proporções do protótipo sem lógica musical. Rolar/clique/arraste na barra, foco, Escape e sobreposição com JEI; trocar tema sem resetar página/offset indevidamente. Scroll imediato no Workshop, animado conforme tokens nos temas antigos.

## Fase 6 — progresso, gráfico e menu radial

**Dependência:** fases 1–5. ProgressBar/RadialMenu em `status.json`; CurveChart em `data.json`.

| Componente | Mudança visual obrigatória |
| --- | --- |
| ProgressBar | Trilho fino Inset retangular, preenchimento oliva sólido com pixels inteiros. Altura configurável; texto opcional nativo sem cobrir frame. Zero e 100% respeitam borda. Não converter em slider interativo. |
| CurveChart | Plotagem em rebaixo escuro `0xFF5F6159`, grade discreta `0xFF74766A`, eixos/texto claro `0xFFEEE9DC`. Curva principal oliva e controles quadrados Raised; ponto ativo/foco com marca clara. Tangentes em linha clara tracejada para diferenciá-las sem depender só de cor. Sem sombras/glow/suavização da UI. |
| RadialMenu | Conservar setores e zona morta circulares, rasterizados em pixels; paleta cinza do tema com contornos claros/escuros. Setor ativo oliva, indicador focado/hover e conteúdo contrastante. Não usar uma placa retangular que destrua a geometria radial. |
| RadialSegment | Sem superfície independente: label/item aparecem no setor, com estado disabled explícito. Não filtrar os pixels do ItemStack para tematizar. |

**Regras:** geometria de curva/radial não muda para se parecer com controles retangulares. Não arredondar ou quantizar os valores do modelo: somente a projeção visual é em pixels. Cores locais explicitamente configuradas para curvas de referência continuam válidas.

**Saída/aceite:** progresso nos extremos e duração desconhecida tratada pelo consumidor; curva com pontos, tangentes, tooltip, zoom/pan e edição; radial com múltiplos setores, itens e opções disabled. Mesmos callbacks/valores nos três temas. Não reintroduzir motion no tema Workshop ao usar componentes mais complexos.

## Fase 7 — diálogos, notificações e ajuda

**Dependência:** fases 1–6. Acrescentar demonstrações carregáveis aos layouts de `status` e ajuda em `basic`, sem exigir salvar um documento antes de testar.

| Componente | Mudança visual obrigatória |
| --- | --- |
| Modal | Janela Raised com TitlePlate, corpo de padding compacto e backdrop Overlay. Botão de header no mesmo estilo IconButton; divisória discreta. Sem fade/slide no tema Workshop. |
| Toast | Descriptor mantém níveis/duração/ação. Mapeia nível para símbolo + palavra/markup acessível; nenhuma cor de status depende de uma faixa gigante saturada. |
| ToastHost | Notificações como placas compactas Raised, texto nativo, pequeno indicador semântico e bordas de acordo com o tema. Entrada/saída imediatas neste preset; empilhamento e duração mantidos. |
| Tooltip | Caixa escura com contorno em pixels, texto claro em escala 1.0, padding curto e quebra de linha. Não herdar fundo claro de Card. Limitar à tela e renderizar acima de itens, modais e popup correto. |

**Interação de polimento:** separar hit test de ativação da consulta de tooltip para permitir explicação de disabled sem permitir clique. Exibir tooltip do controle focado pelo teclado, ancorado ao bounds; InfoBox e abas devem participar. Não confundir nome acessível com integração completa de narrador: verificar o caminho real de narração.

**Saída/aceite:** modal com Select aberto, ItemPreview por baixo, toast e tooltip simultâneos sem vazamento de clipping/Z. Escape fecha na ordem correta, foco retorna ao conteúdo, disabled mostra explicação sem executar ação. A aparência antiga continua com suas animações.

## Playground e exemplos — obrigação em cada fase

Não deixar o playground para uma integração isolada depois de tematizar os widgets.

- Em cada fase, atualizar `PlaygroundComponentFactory`, node/inspector e normalização para expor variantes novas; campos omitidos mantêm documentos v1 antigos. Não salvar classes de skin no JSON.
- Manter IDs basic/input/layout/data/status/scroll. `PALETTE_CATEGORIES` e `BUILT_IN_LAYOUTS` continuam alinhados; toda categoria nova precisa de layout próprio. Preferir acrescentar exemplos às categorias atuais.
- Expor InventoryGrid na categoria Data com itens vanilla; InfoBox em Basic; Modal/ToastHost/Tooltip por ações demonstrativas em Status/Basic, não como áreas editáveis que fingem receber eventos enquanto fechadas. Atualizar as duas listas se um novo tipo editável entrar na paleta.
- Cada exemplo deve conter estados normal, selected e disabled relevantes, rótulo comprido e nested content suficiente para avaliar skin. Hover/pressed/focus são testados em Preview com mouse/teclado; não desenhar mocks estáticos desses estados no lugar do componente real.
- O Theme picker oferece Dark/Light/Workshop com identificação correta. Trocar tema sem remontar desnecessariamente o canvas, mantendo valores e seleção; relayoutar para métricas diferentes.
- O canvas de Preview, seus popups, tooltips e notificações devem usar o tema escolhido. Toolbars e inspector atuais usam também widgets vanilla: identificá-los como chrome de editor e não como amostras de TextField/Button da lib. Harmonizar o fundo/moldura do editor, preservando edição/resize/drag e sem simular que a skin customiza widgets nativos automaticamente.
- Ao final, adicionar layout de referência `workshop-showcase.json` e entrada correspondente em BUILT_IN_LAYOUTS, sem nova categoria artificial. Composição genérica inspirada no protótipo: frame, título, ItemPreview, transporte, Tabs laterais, lista, configurações e overlays; nenhuma dependência do Better Radio ou Create.
- Layouts carregados dos recursos precisam funcionar antes do primeiro Save; não substituir documentos do usuário. Respeitar os limites de PlaygroundDocument.normalize().

## Infraestrutura e inventário de cobertura

As linhas das fases mapeiam todos os componentes públicos visuais/de layout atuais, incluindo os auxiliares de dados TableColumn, RadialSegment e Toast. Os demais arquivos também precisam de destino explícito:

| Infraestrutura | Responsabilidade neste plano |
| --- | --- |
| UiTheme, UiThemes, UiPalette, UiTokens | Novo preset, seleção, compatibilidade e papéis de estilo |
| UiPaint, UiContext, UiColor | Dispatch de skin, contexto/opacidade e preservação de overrides locais |
| UiTypography, UiTextStyle, UiText | Escala nativa, papéis e overflow compartilhado |
| UiRadii, UiMotion, UiEasing | Raios/motion zero no Workshop; defaults antigos preservados |
| UiComponent, UiContainer, UiFrame | Medida, estado e foco/input coerentes; métricas e consulta de tooltip |
| UiEditBox | Preservar cursor, seleção, IME, baseline e medidas no campo tematizado |
| ItemRenderer | Renderização vanilla, quantidade/foil e tooltip na camada correta; não recolorir o item |
| Bounds, Insets, UiSize, Alignment | Coordenadas lógicas inteiras e insets consistentes; sem novo desenho próprio |
| CurvePoint, CurveModel, CurveMath, CurveViewport | Modelos/matemática preservados; nenhuma quantização de dados |
| RadialGeometry | Geometria/hit test preservados; somente material/cores mudam |
| UiOverlayValidation | Ampliar fixtures para o terceiro tema, overlay e foco |
| UiPlatform, UiPlatformScreen, UiJeiProperties (ambos os loaders) | Ponte de ícones/texturas, bounds, input e reserva de GUI; código visual compartilhado em src/main |

Além dos helpers, auditar chamadas diretas a fill/drawString/blit/roundedRect em todos os componentes: nenhum subcontrole pode ignorar a skin e continuar com a linguagem Dark/Light dentro da tela Workshop.

## Validação técnica, entrega e aceite do usuário

### Em cada fase

- Compilar ambos os loaders e executar os testes afetados. Acrescentar testes de medidas, estado, hitbox e overflow onde o teste verifica comportamento real; manter capturas determinísticas para aparência.
- Ampliar o atlas existente de Dark/Light para incluir Workshop. Comparar os dois loaders usando o harness já presente; conservar baseline anterior como controle de regressão.
- Registrar quais componentes do grupo estão concluídos e quais ainda estão pendentes. Não marcar um grupo como pronto apenas porque a classe base mudou.

### Antes de disponibilizar para validação visual

1. `scripts/dev.ps1 -Task buildAll` com JDKs 17/21; verificações de artefatos e testes existentes.
2. `scripts/dev.ps1 -Task :forge:runClient -Validate` e equivalente NeoForge, sequencialmente; repetir com `-WithJei` se afetar itens/abas/bounds; executar `scripts/Compare-UiCaptures.ps1`.
3. Capturas dos seis exemplos, showcase e overlays nos três temas, com conteúdo/timestamps iguais entre loaders. Testar GUI 1/2/3, área lógica pequena, EN/PT-BR e resource pack que substitua a fonte.
4. Garantir texto/itens sem corte, contraste em todos os estados, abas clicáveis nas extremidades, nested clipping e popup sobre lista sem vazamento. Coordenadas de teclado/mouse correspondem aos pixels desenhados.
5. Atualizar documentação técnica implementada e exemplos reais da lib e mod-docs en/pt-BR conforme mod-docs-sync. O plano continua interno, sem ser publicado como documentação de funcionalidades existentes.
6. Entregar instruções curtas: abrir playground, escolher Theme → Workshop, carregar cada categoria e o showcase, entrar em Preview e testar. Entregar capturas e checklist por grupo para o usuário apontar ajustes.

### Gate final

O agente pode concluir implementação e verificações técnicas. **Aceite visual pertence ao usuário**, que fará a validação no playground depois. Usar estados separados no relatório: implementado, verificado tecnicamente, aguardando aceite visual. Não afirmar que o tema foi aprovado visualmente antes desse retorno e não interromper as fases para pedir aprovações intermediárias não solicitadas.

Sem commit, bump, tag, push ou publicação automática. Uma release posterior segue a política de versionamento do usuário. Este pedido autoriza criar o plano; a implementação das fases será uma tarefa posterior.

## Checklist de execução

- [ ] Fundação: UiTheme.WORKSHOP, skin/métricas/paleta e seletor inicial
- [ ] Fase 1: superfícies, texto e layouts
- [ ] Fase 2: ações e seleções básicas
- [ ] Fase 3: entradas e edição de cor
- [ ] Fase 4: itens, inventário e dados tabulares
- [ ] Fase 5: navegação e viewports
- [ ] Fase 6: progresso, gráfico e radial
- [ ] Fase 7: diálogos, notificações e ajuda
- [ ] Exemplos embutidos, inspector e showcase completos
- [ ] Testes/builds/paridade dos dois loaders e documentação implementada
- [ ] Entrega para validação do usuário no playground
- [ ] Aceite visual registrado após retorno do usuário
