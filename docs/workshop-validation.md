# Workshop — entrega e validação

Data: 27/11/2026. Branch: codex/workshop-theme. Base: b38a25e. Versão mantida em 0.10.0, sem tag, push ou publicação.

Implementação da fundação e fases 1–7 concluída. Aceite visual do usuário pendente. A referência foi reaberta entre os grupos; os checkpoints registram a continuidade. Esta sessão não expõe ferramenta de compactação manual de contexto.

## Conferir o resultado

Reinicie o cliente de desenvolvimento para carregar as classes novas. Abra F7, escolha Theme → Workshop e Load → Workshop — Reference composition. Entre em Preview e visite as cinco abas. Recarregue o exemplo embutido para receber o layout novo; um documento já montado mantém sua própria árvore.

O visor de reprodução e o alinhamento do rodapé permanecem nas cinco páginas. Histórico e biblioteca usam listas alternadas e botões originais em pixels; os formulários de configurações e feedback usam o mesmo rebaixo e as mesmas colunas. São exemplos de composição dos componentes reais, sem lógica musical do Better Radio.

| Página | Captura do cliente Forge |
| --- | --- |
| queue | [queue](workshop-previews/queue.png) |
| history | [history](workshop-previews/history.png) |
| library | [library](workshop-previews/library.png) |
| settings | [settings](workshop-previews/settings.png) |
| feedback | [feedback](workshop-previews/feedback.png) |

O catálogo Basic/Input/Layout/Data/Status/Scroll continua disponível. Em Edit, Style expõe superfície, padding, overflow/linhas, ícone/seleção, posição de abas e valor de ProgressBar conforme o componente. Os campos e a toolbar nativos do editor permanecem identificados como chrome do playground.

## Verificações executadas

| Verificação | Resultado |
| --- | --- |
| buildAll, toolchains Java 17/21 | Passou |
| Testes JUnit Forge | 13 testes, 0 falhas/erros |
| Testes JUnit NeoForge | 13 testes, 0 falhas/erros |
| Exemplo Java da documentação | Compilado nos dois loaders |
| Verificação dos JARs | 138 classes por loader, 91 fontes compartilhadas idênticas, bytecode Java 17/21 |
| Forge 1.20.1 com JEI 15 | 383 verificações, 187 capturas; passou |
| NeoForge 1.21.1 com JEI 19 | 383 verificações, 187 capturas; passou |
| Cursor com timestamps fixos | Comparador verificou os intervalos de 300 ms |
| Comparação estrita de pixels | 182/187 capturas idênticas; 5 diferentes, portanto o comando estrito termina com erro |
| mod-docs EN/PT-BR | pnpm run check e pnpm run build passaram |

As verificações reais cobrem os widgets, mouse/teclado, Unicode e overflow, seleção sem callback em setters, campos e edição de cor, abas laterais e gutter, interação de inventário/JEI, modal com Select, ordem de Escape e retorno do foco, e estados normal/foco-seleção/disabled. O conjunto novo captura 41 tipos em três estados, os seis documentos, cinco abas do showcase, escalas de GUI 1/2/3 e overlays.

## Diferenças de renderização nativa

O comparador não foi flexibilizado nem teve imagens retiradas para obter aprovação. As diferenças abaixo foram inspecionadas nas áreas dos previews de blocos 3D; as duas versões do Minecraft renderizam esses itens de modo diferente. O tema mantém o renderer vanilla, como exige o plano. Não declarar igualdade pixel a pixel de toda a suíte.

| Captura | Pixels diferentes de 1.228.800 |
| --- | --- |
| palette-controls-0 | 45332 |
| palette-controls-1 | 48580 |
| palette-controls-2 | 42943 |
| workshop-layout-page-0 | 10574 |
| workshop-data-page-0 | 11116 |

Os atlas de componentes Workshop, as cinco abas do showcase, as escalas e o overlay Workshop são idênticos entre os loaders nesta execução. O relatório bruto fica em build/reports/ui-parity.json; imagens e listas ficam em cada loader/runs/validation/ui-validation. A execução normal mantém runs/client.

## Aceite manual pendente

- Revisão estética final do usuário no playground, inclusive contraste/espaçamento nos estados interativos.
- Auditoria visual completa EN/PT-BR e resource pack que substitua a fonte; as medições usam Font, mas esses cenários não foram executados.
- IME e atalhos/dispositivos físicos; a suíte simula eventos do cliente e não substitui esses testes.
- Narrador Minecraft completo: nomes acessíveis e tooltips não constituem integração completa de narração.
- Integração no Better Radio: esta branch entrega o tema na biblioteca e no playground; áudio, persistência e regras do consumidor não foram alterados.

## Commits por grupo

| Grupo | Commit |
| --- | --- |
| Fundação | c68c1ec |
| Fase 1 — superfícies/texto/layouts | 7809bf4 |
| Fase 2 — ações e seleção | 6bf8d0f |
| Fase 3 — entradas e cor | d4b0ed1 |
| Fase 4 — itens e tabela | 411b45b |
| Fase 5 — navegação/viewports | 3fd03b5 |
| Fase 6 — progresso/gráfico/radial | 311d710 |
| Fase 7 — overlays, showcase e revisão final | Commit que introduz este relatório |

Os guias técnicos da lib e as páginas components-and-themes, playground e loaders-and-validation de mod-docs foram atualizados em EN/PT-BR. As alterações do portal permanecem locais; outras mudanças presentes nos repositórios foram preservadas. Os arquivos pessoais docs/logo.png e docs/logo.psd não fazem parte dos commits Workshop.
