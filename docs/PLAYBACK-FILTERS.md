# Filtros de reprodução — 0.10.0

Abrir **Opções da apresentação → Filtros de reprodução**. Fotografias e vídeos têm regras independentes. Em cada tipo, escolher se deve ser incluído, as orientações, as proporções e os formatos de ficheiro permitidos. Podem ser selecionados vários valores. **Todos / Nenhum** altera a lista; **Guardar** confirma e **Cancelar** preserva a seleção anterior. **Ver seleção atual** mostra os detalhes. **Repor todos os conteúdos** repõe os dois tipos sem restrições após confirmação.

Dentro de uma lista, as escolhas são alternativas. Entre listas, o elemento tem de cumprir todas as condições. Os tipos fotografia/vídeo são alternativas entre si. Uma lista vazia impede conteúdos desse tipo. Os filtros não apagam ficheiros nem alteram a seleção das pastas. A contagem da coleção continua a mostrar os ficheiros encontrados nas origens, antes dos filtros. Uma apresentação sem correspondências regressa às opções com um aviso.

| Objetivo | Configuração |
|---|---|
| Apenas fotografias | Fotografias incluídas; vídeos excluídos |
| Apenas vídeos verticais | Fotografias excluídas; vídeos incluídos; orientação Vertical |
| Fotos 4:3 ou 3:4 e vídeos horizontais | Fotografias com proporções 4:3 e 3:4; vídeos com orientação Horizontal |
| Apenas JPEG/HEIC verticais | Fotografias com formatos JPEG e HEIC/HEIF e orientação Vertical; vídeos excluídos |
| Vídeos MP4/MKV em 9:16 ou 9:20 | Vídeos com esses formatos e proporções; fotografias excluídas |

## Proporções e dimensões desconhecidas

Proporções disponíveis: 16:9, 9:16, 4:3, 3:4, 3:2, 2:3, 1:1, 9:20, 9:21, 21:9, Outras e Desconhecida. Não existe introdução de proporções personalizadas nesta versão. O reconhecimento usa a proporção de apresentação disponível nos metadados, considerando rotação, e escolhe o valor conhecido mais próximo com tolerância logarítmica de 0,025 (cerca de 2,5%). Quadrados admitem cerca de 2% de diferença. Dimensões ausentes/inválidas correspondem a Desconhecida e não são inferidas pelo nome. Os metadados dos vídeos podem não representar todos os casos de píxeis não quadrados; o leitor continua a confirmar a geometria durante a reprodução.

O filtro é aplicado antes de formar os pares: elementos excluídos não entram na sequência. As restrições de apresentação existentes mantêm-se: fotografias 16:9 individuais e pares de vídeos apenas verticais. O modo Individual continua a mostrar um elemento por vez. O filtro também é aplicado a conteúdos recuperados após voltar a ligar uma origem.

## Formatos e Motion Photos

Os filtros de ficheiro agrupam os tipos reconhecidos: JPEG/JPG, HEIC/HEIF, PNG, WebP, BMP, AVIF; MP4/M4V, MOV, MKV, WebM, TS/MTS/M2TS, AVI, 3GP, vídeo incorporado de Motion Photo e Outros. Não são filtros de codec: selecionar MP4 não garante que a box descodifique o vídeo ou o áudio desse ficheiro. Não acrescentam suporte a formatos que o leitor não consiga abrir.

A opção Motion Photos é aplicada antes dos filtros. Em Fotografia, só a parte estática é candidata; em Movimento, a parte de vídeo; em Fotografia e movimento, cada parte passa pela regra do respetivo tipo. O vídeo incorporado tem uma categoria própria de formato. As dimensões da fotografia não são reutilizadas como se fossem as do vídeo incorporado: se o sistema não conseguir ler estas últimas, o vídeo entra em Desconhecida. Com filtros geométricos de fotografias ativos, uma falha do movimento não introduz uma fotografia estática sem validação desses filtros.

## Persistência e validação

As opções ficam guardadas localmente e nos backups esquema 6. Esquemas 1–5 importam sem filtros para preservar o comportamento anterior. Tipos JSON incorretos, valores desconhecidos e entradas duplicadas no backup são recusados antes de restaurar.

Sete testes JVM verificam predefinições, regras independentes/combinadas, seleção vazia, tipos excluídos, tolerâncias, Motion Photos e backup/migração/rejeição. O teste Android de interface cobre cancelar, guardar e persistência independente dos dois tipos; foi preparado e compilado, não executado. A aceitação por comando, dimensões reais, USB e codecs continua pendente na box. Nenhum APK é gerado pelo agente.

## Extensão na versão 0.11.0

Os filtros incluem RAW (pré-visualização JPEG), MPEG-PS e FLV; F4V pertence ao grupo MP4, 3G2 ao grupo 3GP e M2T/TOD ao grupo TS. A associação de Live Photos é aplicada antes dos filtros de cada componente. Os backups atuais são esquema 7 e aceitam os anteriores. Seleções antigas de todos os formatos migram incluindo os novos; subconjuntos explícitos são preservados. Estas regras também são usadas pelo protetor de ecrã. Detalhes em LIVE-RAW-DREAM.md.

