# Plano e arquitetura

## Objetivo

Uma moldura digital para televisão que agrega fotografias e vídeos de várias pastas. Interface em português europeu, legível à distância e operável por comando. Intervalo configurável nas fotografias; vídeos completos com áudio.

## Decisões da primeira versão

| Área | Decisão | Motivo |
|---|---|---|
| Plataforma | Android nativo, Java 17, Android 8+ | Projeto compacto, sem camada web; base compatível com HEIC do Android moderno |
| Interface | Views Android e controlos Media3 | Foco por comando e pequeno número de dependências |
| Motor | Media3 ExoPlayer, playlist mista | Um relógio de reprodução para fotografias e vídeos, pausa e avanço consistentes |
| Acesso | Navegador local e permissões Android; SAF opcional | Suporta boxes sem seletor de documentos |
| Pesquisa | Executor de pesquisa separado do trabalho de metadados | Coleção guardada pode iniciar enquanto as origens são verificadas |
| Estado | SharedPreferences e índice SQLite privado | Configuração separada do índice descartável da coleção |
| Ciclo de vida | Liberta leitor ao sair, regressa pausado | Evita som em segundo plano e recursos de vídeo retidos |
| Integridade | Não escrever nos conteúdos | A coleção não altera nem elimina os originais |

## Responsabilidades

- `MainActivity`: interface, foco, seleção de pastas, ciclo de vida e apresentação.
- `AppSettings`: opções e autorizações de pastas guardadas como URIs.
- `LibraryScanner`: leitura recursiva das árvores de documentos fora da interface, com avisos de acesso.
- `MediaEntry`: identificação de imagem/vídeo e metadados mínimos.
- `CollectionOrder`: deduplicação e ordem da sequência, sem dependência Android.
- `PhotoSlides`: pares consecutivos e geometria sem corte, com regras testáveis fora do Android.
- `VideoPairPlayback`: sequência das duas reproduções completas com som; as repetições sem som não completam o par.
- `PresentationCover`: imagem temporária SDR durante a mudança de apresentação, com memória e espera limitadas e sem leitores adicionais.
- `PhotoMetadata`: leitura de dimensões/EXIF das fotografias e dimensões/rotação dos vídeos fora da interface, com cache limitado e sem extrair fotogramas completos.
- `SettingsBackup`: formato JSON versionado e validação completa de pastas/opções antes de restaurar.
- `BackupController`: guardar/abrir ficheiros no seletor Android, pré-visualizar a restauração e voltar a autorizar pastas. A leitura/escrita do backup corre fora da interface.
- `FolderSource`, `LocalFolders`, `StorageLocations`, `StorageController`: referências locais/documentos, pesquisa local, descoberta de volumes, permissões e navegador próprio. Backups locais não dependem do seletor Android.

## Live Photos, RAW, captura, informação e DreamService — 0.11.0

`LivePhotos` associa apenas pares não ambíguos fotografia/MOV na mesma pasta por nome-base. `CaptureDates` interpreta EXIF/vídeo e define o fallback explícito. `SessionPreparer` é partilhado pela atividade e protetor: deduplicação/ordem, captura, Live Photos, Motion Photos, filtros e pares. A recuperação na atividade prepara a coleção completa antes de remover os URIs já presentes, preservando o contexto das associações.

`RawPreviewProvider` não exportado produz JPEG normalizado sob pedido, a partir da pré-visualização incorporada, com limites de bytes/píxeis e cache privada. `MediaEntry` preserva identidade e MIME do original, com URI de apresentação separado. `MediaPlayers` centraliza a fábrica de descodificadores e os MediaItems. `ContentDetails` recolhe informação pelo executor e a atividade disponibiliza INFO/Menu e botão Informação.

`MolduraDreamService` registado/habilitado por predefinição, protegido por BIND_DREAM_SERVICE; `ScreensaverSettings` abre a escolha no Android, sem alterar Settings.Secure. `DreamPlayback` apresenta pares e duas fases de som com no máximo dois leitores, recuperação e libertação no ciclo de vida. Não usa a cobertura SDR da atividade, não aplica preenchimento com corte e termina ao primeiro comando por ser não interativo. Seleção como protetor predefinido exige ação nas definições do sistema.

Índice SQLite v2 guarda captura; a migração invalida cabeçalhos antigos para os reler. Backups esquema 7 guardam Live Photos e ordem 3; anteriores importam sem associação e preservam os restantes contratos. Filtros antigos de todos os formatos expandem para RAW/MPEG-PS/FLV; seleções específicas mantêm-se. Instruções e aceitação em `LIVE-RAW-DREAM.md`.

## Filtros de reprodução — 0.10.0

`PlaybackFilter` contém regras imutáveis, independentes por tipo, com alternativas dentro de cada dimensão e interseção entre dimensões. `FilterDialog` permite seleção múltipla por comando, guardar/cancelar e repor. As regras são capturadas antes do trabalho em segundo plano, aplicadas após expandir Motion Photos e antes de formar pares. As fontes recuperadas usam o mesmo filtro. Metadados geométricos só são pedidos para filtrar quando necessários. Backups esquema 6, com predefinição sem filtros nos anteriores. Os filtros são locais à apresentação; o índice continua a guardar a coleção integral. Ver `PLAYBACK-FILTERS.md`.

## Memória, índice e recuperação — 0.9.0

- `ImageBudget`: calcula o limite de saída do descodificador de fotografias Media3 a partir do ecrã, heap, pares e arquitetura do processo.
- `CollectionIndex`: snapshot SQLite transacional das origens/coleção e metadados versionados por URI, data e tamanho; falhas degradam para pesquisa normal.
- `PhotoMetadata`: verifica a versão atual antes de reutilizar dados, lê cabeçalhos/EXIF/XMP em segundo plano e grava os metadados em lotes.
- `Diagnostics` e `FailureLog`: histórico privado limitado, deduplicação, categorias de reprodução e reconstrução do índice por opção do utilizador.
- `StorageWatcher` e `SessionRecovery`: eventos Android, filtragem por volume local, preservação de elementos saudáveis e inclusão de origens recuperadas no avanço seguinte.
- `MotionPhotos`: reconhecimento conservador de XMP e modos Fotografia/Movimento/Fotografia e movimento. Mantém MIME original para o extrator nativo; não adiciona codecs nem converte conteúdos.
- Backups esquema 5, compatíveis com esquemas 1–4; o índice e o diagnóstico não são exportados.

Detalhes de implementação, limites e aceitação: `MEDIA-AND-RECOVERY.md`. A recuperação preserva as duas fases de áudio do par atual. Testes JVM/compilação não demonstram comportamento de USB, SQLite Android, descodificadores ou Motion Photos no equipamento.

## Divisão do ecrã e transições — 0.8.0

Três modos persistidos/exportados no esquema 4: Automático, Individual e Dois lado a lado. Migra os esquemas 1–3 sem ligar uma opção anteriormente desligada. A opção de pares mantém as restrições de tipos/formato, incluindo fotos 16:9 sempre individuais. Automático requer altura de pelo menos 55% do ecrã e largura de cada lado de pelo menos 12%; 4:3 e retratos comuns continuam elegíveis em TV 16:9. O modo de pares ignora esses limiares, mas não força pares de tipos incompatíveis.

Na ordem aleatória, PhotoSlides procura a menor diferença logarítmica de proporções entre até 12 candidatos do mesmo tipo. Não atravessa uma fronteira foto/vídeo nem modifica a lista original. Nome/data mantêm pares consecutivos. A geometria encaixa a soma das larguras inteiras nos limites do ecrã mesmo com dimensões ímpares.

onVideoSizeChanged confirma a proporção de apresentação com o pixelWidthHeightRatio do Media3, cujas dimensões já consideram a rotação aplicada. O par só confirma a geometria quando ambos os lados responderem e depois mantém as regiões durante a troca de som. Vídeo afinal horizontal ou par que falhe o limiar automático passa à tentativa individual preservando ambos os originais.

Na mudança manual/automática, PresentationCover tenta copiar a apresentação anterior: Canvas para fotografias SDR e PixelCopy para SurfaceView de vídeos SDR. Até 960×540 ARGB por lado (menos de 4 MiB no total), espera máxima de captura de 250 ms, sem ficheiros em disco e sem terceiro/quarto descodificador. Só cobre a área quando a captura estiver completa. Remove a cobertura depois de ambos os novos lados estarem disponíveis (imagem visível/primeiro fotograma), ou ao sair. A cobertura não é usada para vídeos identificados como HDR nem fotos wide gamut/gainmap identificadas; falhar a cópia não bloqueia a navegação. Não existe promessa de transições sem preto em todos os modelos nem preloading de quatro vídeos. O fundo permanece preto, sem blur.

Referências: [eventos de dimensões/primeiro fotograma](https://developer.android.com/reference/androidx/media3/common/Player.Listener) e [PixelCopy](https://developer.android.com/reference/android/view/PixelCopy). Verificação visual/memória/HDR em dispositivo permanece pendente.

## Release otimizada — 0.7.1

R8, redução otimizada de recursos e depuração desativada na variante release. A configuração usa regras padrão otimizadas e consumer rules das dependências, sem bloquear a otimização de pacotes inteiros. Assinatura existente preservada. O script `build.ps1 -Release` valida R8, recursos e lint release sem gerar APK; testes unitários continuam na variante debug. Resultados e limites em `RELEASE.md`. A aceitação da release ofuscada continua a exigir o APK gerado pelo utilizador no Android Studio e testes na box.

## Reprodução integral de cada vídeo com som — 0.7.0

A política dos pares de vídeos passa a ter três estados explícitos, testados em Java puro: LEFT, RIGHT e COMPLETE. Em LEFT, o leitor principal toca integralmente com som e o auxiliar repete a pré-visualização sem som. Ao terminar LEFT, ambos são silenciados/pausados na troca, o auxiliar perde a repetição e reinicia na posição zero para a sua vez completa com som; o principal reinicia como pré-visualização muda. Em RIGHT, terminar a pré-visualização principal reinicia apenas esse leitor, sem procurar nem pausar o auxiliar. Terminar a vez RIGHT muda para COMPLETE e agenda o avanço do par. Não existe alternância interminável.

O estado LEFT/RIGHT, as duas posições e a intenção de pausa são preservados ao sair/regressar à atividade. Foco e volume acompanham o lado da vez atual; desligar som silencia ambos mas preserva as duas fases. A espera por buffering de uma pré-visualização não suspende a reprodução com som. Preparação inicial, mudanças de par e falhas mantêm as verificações de validade dos leitores/callbacks.

A regra de fotografias 16:9 fica independente da proporção física do ecrã: aparecem sempre sozinhas, incluindo em ecrãs 21:9/32:9. As fotografias 4:3 e verticais mantêm os pares elegíveis. Backups permanecem no esquema 3, sem nova opção.

Esta política substitui a espera com último fotograma e mudança de som sem reinício da versão 0.6.0. Aceitação no dispositivo continua pendente; testes Java não executam os leitores Android.

## Vídeos verticais em pares — 0.6.0 (introdução)

O mesmo modo inteligente passa a agrupar dois vídeos verticais consecutivos (proporção inferior a 1 e a 95% da proporção do ecrã). Não forma pares mistos fotografia/vídeo. MediaMetadataRetriever lê dimensões e rotação em segundo plano, sem extrair fotogramas. Os dois leitores usam FIT em regiões adjacentes, sem corte ou separador.

O leitor principal usa setPauseAtEndOfMediaItems(true) e repetição desativada; a atividade decide o avanço e a repetição da coleção. Cada lado tem estado de conclusão independente. Pares de fotografias avançam no intervalo principal; pares de vídeos só avançam quando ambos terminam. O vídeo mais curto conserva o fotograma final. A preparação inicial e buffering suspendem o outro lado; a pausa manual preserva a intenção de reprodução. Sair guarda as duas posições e conclusões e regressa pausado.

Por escolha do utilizador, apenas o vídeo esquerdo tem som inicialmente. Ao terminar primeiro, o áudio passa para o vídeo direito restante, que assume também a gestão do foco de áudio. Desligar som silencia ambos. O controlo de pausa representa o par, mesmo com o leitor principal já terminado. A barra de procura e avanço/recuo temporal ficam ocultos em pares de vídeos, pois não representam duas durações independentes; navegação entre apresentações e pausa continuam disponíveis.

Se um leitor falhar num par de vídeos, a sessão transforma o par em dois elementos individuais consecutivos. O segundo original não é descartado por falta de dois descodificadores. Um vídeo individual que também falhe segue o tratamento habitual de erros. Nenhum ficheiro é alterado. Mantém o esquema 3 dos backups e a chave smartPhotos, agora relativa a todo o modo inteligente; uma opção anteriormente desligada permanece desligada.

Referências: [pausa no fim de cada elemento no Media3](https://developer.android.com/reference/androidx/media3/exoplayer/ExoPlayer#setPauseAtEndOfMediaItems(boolean)) e [dimensões/rotação de vídeo](https://developer.android.com/reference/android/media/MediaMetadataRetriever). Aceitação visual, som, foco, dois descodificadores, HDR e sessão longa continuam pendentes na box.

## Fotografias inteligentes — 0.5.0 (introdução)

O modo vem ligado por defeito e agrupa duas fotografias consecutivas com proporção inferior a 95% da proporção do ecrã. Não atravessa vídeos nem dá a volta ao fim da sequência para formar pares. Metadados indisponíveis resultam numa fotografia individual. Imagens quase panorâmicas ficam individuais. A geometria usa altura comum e larguras proporcionais, sem divisória nem corte; o espaço exterior permanece preto.

A playlist principal tem um elemento por apresentação (individual/par). O Media3 principal continua a controlar duração, vídeos e navegação; um segundo leitor, sem áudio e apenas com a fotografia direita, existe exclusivamente durante o par. O relógio principal aguarda a preparação da segunda fotografia (até 30 segundos). Os controlos ocupam a largura total do ecrã. Falhar uma fotografia mantém a outra; sair da atividade liberta ambos os leitores e o regresso fica pausado. Os originais não são recompostos nem regravados. Backups no esquema 3 incluem a opção e importam os esquemas 1/2 com o modo ligado.

Compilação e testes unitários não validam o resultado visual, cores/HDR, consumo de memória ou comandos na box. A aceitação dos pares e dos dois leitores está pendente no equipamento real.

## Navegação e permissões — 0.4.0

Adicionar pasta passa a usar um navegador incluído, com armazenamento interno, volumes externos montados e pastas da própria MolduraTV. Permissões abre as autorizações Android apropriadas à versão do sistema. O seletor de documentos fica como alternativa para fornecedores externos/rede. Guardar/restaurar backups também dispõe de navegação própria, com esquema 2 compatível na importação com o esquema 1. Ver `STORAGE.md` para utilização e limites reais do Android.
- `ReleaseUpdate`, `GitHubUpdates`, `UpdateIdentity`, `UpdatePackageVerifier` e `UpdateController`: contrato das Releases, rede, verificação de identidade e fluxo de atualização com confirmação do Android.

## APIs atuais e atualizações — 0.3.0

Substituídas as chamadas antigas do modo imersivo por `WindowInsetsControllerCompat`. Seleção de pastas e ficheiros migrada para Activity Result APIs, com registo estável dos callbacks e preservação da pasta pendente de autorização.

A app procura versões estáveis no repositório público indicado pelo utilizador, descarrega o anexo compatível e verifica hash/identidade/assinatura antes de pedir instalação ao Android. A geração e publicação dos APKs são manuais, pelo utilizador. O procedimento e os testes de equipamento estão em `GITHUB-UPDATES.md`.

## Backups de configuração — 0.2.0

O botão **Backups** exporta e importa exclusivamente a seleção de pastas e todas as opções da apresentação. A importação substitui a configuração apenas depois da validação e de confirmação na interface. Não copia conteúdos nem transfere autorizações Android. O formato `molduratv-settings`, esquema 1, limita a leitura a 1 MiB, admite até 512 pastas, recusa referências fora de árvores `content://` e valida os tipos/valores das opções. Autorizações em falta podem ser recuperadas escolhendo novamente a pasta; cancelar ou falhar essa autorização preserva a referência anterior.

O leitor usa a duração de imagem do Media3 e a duração do próprio vídeo. Não existe temporizador independente que corte um vídeo ao fim do intervalo das fotografias. Os erros eliminam apenas o elemento da sessão; a biblioteca original é preservada.

## Etapas e critérios de aceitação

1. **Base funcional — implementação inicial:** projeto compilável, pastas persistentes, sequência mista, opções, controlo por comando, erros e testes das regras da coleção. O utilizador gera os APKs no Android Studio; o agente valida sem empacotar. A compilação não substitui os testes de equipamento.
2. **Validar na TV/box:** modelo e Android exatos; USB real; som; navegação; JPEG/HEIC; vídeo SDR/HDR de várias resoluções; pausa e retoma; sessão prolongada. Corrigir problemas reproduzidos antes de aumentar o âmbito.
3. **Rede direta:** escolher SMB2/3 e/ou WebDAV segundo o NAS real. Implementar credenciais protegidas, ligação de leitura, leitura com seek, limites de espera e recuperação. Critérios: reprodução contínua, vídeos com procura, falha/retoma de rede e credenciais ausentes/incorretas sem bloquear a interface.
4. **Experiência de coleção:** miniaturas paginadas, seleção de álbuns, ordem EXIF e atualização incremental. Critério: biblioteca grande sem bloqueios nem consumo excessivo de memória.
5. **Robustez e distribuição:** validação prolongada, acessibilidade, ícone/banner finais e versão release assinada. Autoarranque e modo protetor de ecrã só entram se forem pedidos.

## Limites técnicos a resolver com equipamento real

Descodificação HEIC e variantes HDR, passagem de SDR para HDR, orientação EXIF, resolução efetiva apresentada e combinações de contentor/codec/áudio. Testar com amostras originais. A app não deve anunciar compatibilidade universal.

## Referências de implementação

- [Aplicações para Android TV](https://developer.android.com/training/tv/get-started/create)
- [Acesso a pastas e autorizações persistentes](https://developer.android.com/training/data-storage/shared/documents-files)
- [Imagens numa playlist Media3 e limites de formato](https://developer.android.com/media/media3/exoplayer/images)
- [Media3 e versões](https://developer.android.com/jetpack/androidx/releases/media3)
- [Android Gradle Plugin 9.4](https://developer.android.com/build/releases/agp-9-4-0-release-notes)




