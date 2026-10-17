# Validação — 0.11.0

O estado final dos testes automáticos está em `BUILD-RESULTS.txt`, gerado na entrega. Esta lista é o protocolo de aceitação em equipamento real; itens sem evidência permanecem pendentes.

## Evidência automática

**Versão atual (0.11.0):** compilação Java app/testes Android, R8 na variante release, redução/otimização de recursos, lint debug/release e 90 testes unitários debug aprovados, sem geração de APKs. Os testes Android executados abaixo pertencem à versão 0.2.0 e são históricos; não foram repetidos nesta alteração.

- Quatro testes unitários: deduplicação entre pastas, ordem por data/nome, aleatoriedade sem perder elementos e reconhecimento de formatos.
- Três testes no emulador **Google TV / Android 16 / 1080p**: fotografia de 5 segundos → vídeo de 3 segundos → fotografia de 5 segundos e fim; imagem inválida seguida de imagem válida; libertação do leitor ao sair e regresso pausado.
- As amostras são sintéticas: JPEG 640×360 e MP4 H.264/AAC 640×360. O fornecedor de ficheiros é um componente exclusivo do APK de testes que simula consultas de documentos. Estes testes não validam o seletor de pastas real, autorizações persistentes, USB, NAS, qualidade audível, orientação EXIF, HEIC, HDR ou 4K.
- Os relatórios XML e a análise estática são preservados em `docs/evidence/`.

## Live Photos, RAW, captura, informação e protetor — 0.11.0

- 16 testes JVM novos: Live Photos (6), datas de captura (5), formatos/identidade da pré-visualização/migração de filtros/backups (5). Total final **90 testes em 16 suites**, 0 falhas, 0 erros, 0 ignorados, contados nos XML finais.
- Java da app e testes Android compilado. Lint debug/release: 0 erros e 8 avisos existentes por variante. R8 e recursos release aprovados; sem APKs gerados.
- Sete testes Android novos preparados e compilados: captura EXIF vs modificação e persistência; associação Live Photo e filtragem independente; RAW sintético com JPEG incorporado, fornecedor apenas de leitura e original preservado; registo/permissão DreamService e fornecedor privado; migração de índice v1→v2; diálogo INFO; duas fases de vídeo no leitor do protetor e libertação.
- **Testes Android não executados.** O teste do leitor do protetor numa janela de atividade não prova a ativação do DreamService real pelo Android. RAW sintético não substitui ficheiros de câmaras; a associação por nome não prova identificador Apple.
- Pendente no equipamento: originais Live Photos/RAW, contentores/codecs/áudio, INFO/Menu/comando, captura sem fuso, seleção/inatividade/saída do serviço nativo, USB/HDR e memória em processo de 32 bits. Protocolo em `LIVE-RAW-DREAM.md`; relatórios em `docs/evidence/0.11.0/`.

## Filtros de reprodução — 0.10.0

- Sete testes unitários novos: seleção total/desconhecidos sem leitura de metadados; combinações independentes de tipos/orientações/proporções/formatos; exclusão e seleção vazia; tolerância e formatos verticais próximos; regras de Motion Photos; backup esquema 6 e migração do 5; rejeição de filtros inválidos.
- Total final: **74 testes unitários em 13 suites**, 0 falhas, 0 erros, 0 ignorados. Compilação da app e testes Android, lint debug/release e R8/recursos release aprovados. Lint mantém 0 erros e 8 avisos existentes por variante.
- Novo teste Android preparado/compilado para cancelar/guardar no diálogo e persistência independente das regras de fotografias/vídeos. Não executado. Filtragem real, comando, orientações/metadados e fontes recuperadas pendentes na TV/box.
- Evidência em `docs/evidence/0.10.0/`; protocolo e limites em `PLAYBACK-FILTERS.md`. Nenhum APK gerado pelo agente.

## Memória, índice, diagnóstico, recuperação e Motion Photos — 0.9.0

- 15 testes JVM adicionais: orçamento de imagem (3), histórico limitado/deduplicação (3), XMP Motion Photos e classificação (4), remoção/recuperação de fontes e preservação de fotografia+movimento (4), backup esquema 5 e migração (1).
- Total final: **67 testes**, em 12 suites, com 0 falhas, 0 erros e 0 ignorados. A contagem é extraída dos XML finais, sem somar execuções repetidas.
- Compilação Java da app e das classes de testes Android aprovada. Lint debug/release: 0 erros e 8 avisos existentes em cada variante. R8 e otimização de recursos release aprovados. Nenhuma tarefa de geração de APK foi solicitada.
- Cinco testes Android novos preparados e apenas compilados: índice SQLite e invalidação por versão; substituição de fotografia mantendo o caminho; saída limitada do descodificador; fotografia+vídeo incorporado sintético; remoção simulada de uma origem com continuidade noutra. O teste de backups aguarda também o carregamento inicial do índice.
- Motion Photo sintética: JPEG de teste com XMP e MP4 incorporado, sem conteúdos do utilizador. Não substitui amostras originais de telemóveis.
- **Testes Android não executados.** SQLite Android, reprodução real, remoção/reconexão USB, permissões, qualidade visual, consumo de memória e desempenho continuam pendentes. Protocolo em `MEDIA-AND-RECOVERY.md`.
- Relatórios preservados em `docs/evidence/0.9.0/`. As secções seguintes descrevem verificações históricas, com os respetivos totais à data.

## Backups — verificações da versão 0.2.0

- Seis testes unitários adicionais: preservação de todas as opções e pastas na exportação/importação; coleção vazia e duplicados; identificação/versão do formato; tipos e valores inválidos; referências de pastas inválidas; limites de tamanho e de número de pastas.
- Quatro testes Android adicionais: exportação legível pelo formato da app; pré-visualização sem alteração, cancelamento e restauração confirmada persistente; rejeição de backup inválido mantendo a configuração; seleção preservada quando a autorização é cancelada ou recusada.
- Os testes de ficheiros usam o fornecedor simulado do APK de testes. A escolha de um destino real no seletor da box, a remoção de uma pen durante escrita e a recuperação real de autorizações persistentes continuam a exigir validação no equipamento.

## Atualizações — verificações da versão 0.3.0

- Cinco testes do contrato de Releases: comparação numérica de versões; Release nova/igual/antiga; drafts/prereleases e anexos ausentes/ambíguos; origem, hash e tamanho; domínios e HTTPS dos redirecionamentos.
- Quatro testes da política de instalação: identidade compatível; pacote/versão incorretos; `versionCode`, versão semântica e Android mínimo; assinatura ausente, diferente ou conjunto de assinaturas diferente.
- Três testes de transferência, com bytes sintéticos em memória: cópia completa com SHA-256; ficheiros truncados, demasiado grandes ou alterados; cancelamento antes e durante a leitura.
- Total da versão 0.3.0: **22 testes unitários**, incluindo os dez testes anteriores da coleção/backups. Não foram gerados APKs nem executados testes Android que exijam pacotes.
- Pendente: fluxo real de rede, extração de certificados de um APK publicado, seletor moderno, autorização de instalação, `FileProvider`, instalador da box e atualização com preservação dos dados. O protocolo está em `GITHUB-UPDATES.md`.

## Armazenamento — verificações da versão 0.4.0

- Cinco testes de navegação/pesquisa local: listar pastas e filtrar JSON; pesquisa recursiva sem duplicar subárvores; caminhos canónicos com espaços/acentos; pasta ausente; interrupção da pesquisa.
- Dois testes adicionais de backups: importar esquema 1 e restaurar coleção mista no esquema 2; criar backup local legível e preservar o ficheiro anterior se houver colisão de nome.
- Os testes de referências foram atualizados para recusar caminhos relativos, autoridades remotas e segmentos de travessia.
- Total da versão 0.4.0: **29 testes unitários**. Os testes de disco usam diretórios temporários no computador, sem gerar pacotes Android.
- Pendente na box: catálogo real de USB/volumes, ecrãs de permissões do fabricante, Android 8–10 e 11+, navegação por comando, seleção dentro da própria app, backups locais e reprodução de URIs locais pelo Media3. Protocolo em `STORAGE.md`.

## Fotografias inteligentes — verificações da versão 0.5.0

- Seis testes unitários novos: pares retrato/4:3 e número ímpar; ordem com vídeo/panorâmica entre fotografias; dimensões desconhecidas/inválidas; modo desligado sem ler metadados; pares 9:16; geometria de pares com proporções diferentes.
- Um teste adicional de backup: modo ligado/desligado preservado, importação de esquemas 1/2 com predefinição ligada e rejeição de valores em falta/inválidos no esquema 3.
- Total da versão 0.5.0: **36 testes unitários**, sem falhas, erros ou testes ignorados. Compilação das classes Java da app e dos testes Android aprovada. Análise estática: 0 erros, 8 avisos existentes.
- Três testes Android preparados e apenas compilados: par durante um intervalo → fotografia ímpar → vídeo completo; libertação dos dois leitores ao sair e regresso pausado; transição automática para outro par e pausa/retoma dos dois leitores. Usam JPEG sintético 360×640 e o vídeo H.264/AAC existente. **Não executados**: exigem pacotes que o utilizador gera no Android Studio.
- Pendente na box: pares JPEG/HEIC com EXIF, pares de proporções diferentes, verificação visual de todas as bordas, ausência de separador, pausa e avanço, falha de cada lado, HDR e memória em sessão longa. Dimensões EXIF e descodificação Android não são executadas pelos testes unitários.

## Vídeos verticais em pares — verificações da versão 0.6.0

- Três testes unitários adicionais: vídeos verticais consecutivos e número ímpar; vídeos horizontais/quadrados ou dimensões desconhecidas; preservação da ordem e fronteiras entre fotos/vídeos.
- Total da versão 0.6.0: **39 testes unitários**, sem falhas, erros ou testes ignorados. Java da app e testes Android compilados; análise estática com 0 erros e 8 avisos existentes. Sem geração de APK.
- Três testes Android novos preparados e apenas compilados: vídeo esquerdo de 2 s e direito de 7 s, espera pelo direito e saída/regresso com as duas posições; durações invertidas, sem cortar ao fim dos 5 s das fotografias; falha simulada do par seguida da reprodução individual de ambos. Amostras sintéticas H.264/AAC 180×320, 15 fps, 2 s e 7 s, verificadas por ffprobe. Não são amostras HDR ou 4K.
- Todos os testes Android desta versão permanecem **não executados**. A compilação não valida Media3, MediaMetadataRetriever, retenção do fotograma final, áudio/foco ou limites de descodificadores reais.

## Reprodução completa com som e fotografias 16:9 — 0.7.0

- Cinco testes unitários da política de fases: direita curta repete sem completar a vez com som; pré-visualização esquerda repete sem avançar o par; direita longa também reinicia antes da vez com som; restauração da vez direita; eventos tardios após conclusão ignorados.
- Dois testes de fotografias: 16:9 sempre individuais em ecrãs 16:9/21:9/32:9; fotografia horizontal entre retratos preserva a ordem e não é incluída num par.
- Total da versão 0.7.0: **46 testes unitários**, 0 falhas, 0 erros, 0 ignorados. Compilação da app e classes dos testes Android aprovada; lint 0 erros e 8 avisos existentes. Sem APKs novos.
- Testes Android atualizados para exigir reinício da direita perto de zero já com som, esquerda em repetição sem reiniciar a direita, preservação da vez direita ao sair/regressar e repetição da direita curta antes da sua vez com som. Teste Android novo para duas fotografias 16:9 adjacentes, sem leitor auxiliar e com a imagem na área total do ecrã.
- Estes testes Android foram apenas compilados, **não executados**. Repetições, som/foco, transições, metadados reais e comportamento visual permanecem pendentes na TV/box.

## R8 e recursos da release — 0.7.1

- `minifyReleaseWithR8`: aprovado, com DEX otimizado/ofuscado, mapping, lista de código removido e configuração efetiva. Sem regras que desativem shrinking/otimização/ofuscação; regras Media3 incluídas automaticamente.
- `optimizeReleaseResources`: aprovado, após redução integrada no R8 e conversão dos recursos restantes. `update_paths` preservado pelo manifesto.
- `lintRelease`: 0 erros e 8 avisos existentes. Os 46 testes unitários debug continuam aprovados, sem falhas/erros/ignorados. Nenhum teste executa o DEX otimizado no dispositivo.
- `build.ps1 -Release` executado com sucesso, depois de verificar o grafo sem geração de APK. Relatórios em `docs/evidence/0.7.1/`; detalhes em `RELEASE.md`.
- Pendente: APK release gerado pelo utilizador no Android Studio, aceitação de reprodução/comando/USB/backups/atualização na box e medições reais de desempenho/tamanho.

## Divisão adaptativa e transições — 0.8.0

- Quatro testes unitários adicionais de apresentação: melhor correspondência de proporções na ordem aleatória sem alterar a entrada; fronteira entre foto/vídeo preservada; Automático evita áreas minúsculas e Dois lado a lado respeita as regras de formato; 9:16/9:20/9:21/3:4 em ecrã com dimensões ímpares e proporção dos píxeis.
- Dois testes adicionais de backups: três modos preservados e migração do esquema 3; rejeição de modo em falta/inválido/contraditório no esquema 4. Importação dos esquemas 1/2 continua coberta pelos testes anteriores.
- Total da versão 0.8.0: **52 testes unitários**, sem falhas/erros/ignorados. Java app e classes Android compilados; lint debug/release com 0 erros e 8 avisos existentes. R8 e recursos release validados sem APK.
- Testes Android preparados/compilados: modo Individual sem segundo leitor e persistência; cobertura na mudança de par de fotografias, remoção ao abrir a nova apresentação e libertação ao sair. O teste da vez direita verifica também que a geometria permanece igual à da vez esquerda.
- Testes Android **não executados**. Permanecem pendentes no dispositivo: callbacks reais de dimensões, estabilidade visual, cópia de SurfaceView, cancelamento/remoção da cobertura, custo de memória, HDR/wide gamut, comando e falhas reais de descodificação.

| Teste no equipamento | Resultado esperado | Estado |
|---|---|---|
| Abrir no launcher TV e no launcher de uma box Android | App visível; interface horizontal | Pendente |
| Navegar em todos os botões e diálogos com o comando | Foco claro; todas as ações acessíveis | Pendente |
| Autorizar pasta USB e reiniciar a app | Pasta guardada e coleção acessível | Pendente |
| Misturar JPEG → vídeo com som → JPEG | Fotografia usa o intervalo; vídeo termina antes de avançar | Pendente |
| Pausar uma fotografia durante o intervalo | Permanece pausada; retoma a duração restante | Pendente |
| Pausar vídeo e mudar de elemento | Controlo consistente, sem áudio residual | Pendente |
| Sair para Home e regressar | Sem som em segundo plano; regresso pausado | Pendente |
| Repetição ligada/desligada | Repete ou termina conforme a opção | Pendente |
| Ficheiro corrompido seguido de ficheiro válido | Salta o erro e continua | Pendente |
| Todos os ficheiros inválidos | Regressa à coleção; não entra num ciclo de erros | Pendente |
| Remover USB ou interromper rede durante reprodução | Erro limitado e recuperação do controlo | Pendente |
| HEIC e JPEG de telemóvel em retrato | Orientação e proporções corretas | Pendente |
| Duas fotos 9:16, 3:4 ou 4:3, incluindo proporções diferentes | Ambas inteiras, encostadas, sem separador nem distorção | Pendente |
| Par com intervalo de 60 segundos, pausa e avanço manual | Um intervalo por par; pausa/avanço afetam a apresentação | Pendente |
| Dois vídeos verticais de durações diferentes (ambas as ordens) | Esquerdo completo com som, direita reinicia para passar completa com som, depois avança | Pendente |
| Vídeo sem som termina enquanto o outro toca com som | Pré-visualização repete sem reiniciar nem pausar o vídeo com som | Pendente |
| Som dos pares de vídeos e opção sem som | Um lado audível por vez; ambos silenciosos com som desligado, mantendo as duas fases | Pendente |
| Pausar, sair e regressar na vez da direita | Ambos param; duas posições e vez da direita preservadas; regresso pausado | Pendente |
| Fotografias horizontais 16:9 adjacentes, também em ecrã ultralargo | Uma fotografia de cada vez, na área completa do ecrã, sem par nem cortes | Pendente |
| Selecionar Automático/Individual/Dois lado a lado e restaurar backup | Modo preservado; 16:9 continuam individuais em todos os modos | Pendente |
| Vídeos 9:16/9:20/9:21/3:4 com rotação/proporção de píxel | Imagens inteiras; proporção confirmada pelo leitor; regiões estáveis nas duas fases | Pendente |
| Mudar de par SDR e avançar rapidamente, pausar ou sair durante a troca | Cobertura temporária limitada; navegação recuperável; sem imagem antiga presa | Pendente |
| HDR/wide gamut e falha de PixelCopy | Troca continua sem impor uma captura SDR aos conteúdos identificados | Pendente |
| Falha do par ou box sem dois descodificadores | Tenta ambos individualmente pela ordem original | Pendente |
| Vídeo com rotação 90/270 e pares SDR/HDR/4K | Orientação sem corte; limites de desempenho/cores documentados no equipamento | Pendente |
| Número ímpar de fotos, panorâmica ou vídeo entre retratos | Sem perda/reordenação; elemento sem par aparece sozinho | Pendente |
| Falha ao abrir uma das fotos de um par | A outra continua sozinha; originais preservados | Pendente |
| Desligar modo inteligente, reiniciar e restaurar backup | Opção preservada; apresentação individual reposta | Pendente |
| Vídeos 480p, 720p, 1080p e 4K com áudio | Reprodução e som adequados às capacidades da box | Pendente |
| Vídeo HDR10/HLG e fotografia HDR em TV HDR | Brilho e cores corretos; saída HDR confirmada na TV | Pendente |
| Mesmos conteúdos num ecrã SDR | Comportamento de apresentação documentado | Pendente |
| NAS através de fornecedor de documentos | Pesquisa e reprodução com acesso persistente | Pendente |
| Coleção grande e reprodução durante 8 horas | Sem bloqueios ou crescimento contínuo de memória | Pendente |

Registar modelo da box/TV, versão Android, ligação HDMI, formato/codec das amostras e resultado observado. Não inferir compatibilidade HDR ou 4K de um teste em emulador.




