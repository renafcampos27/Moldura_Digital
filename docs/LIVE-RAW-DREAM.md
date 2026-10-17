# Live Photos, RAW, informação, captura e protetor — 0.11.0

## Live Photos com ficheiros separados

**Opções da apresentação → Live Photos (fotografia + MOV)** permite escolher Só fotografia, Só vídeo associado, Fotografia e vídeo juntos ou Não associar. A instalação usa Fotografia e vídeo juntos por predefinição. A associação procura exatamente uma fotografia JPEG/HEIC/HEIF e um MOV com o mesmo nome-base na mesma pasta. Exemplo: `IMG_1234.HEIC` e `IMG_1234.MOV`. Nos modos associados, o grupo ocupa a posição da fotografia na ordem selecionada; o MOV não volta a aparecer na sua posição anterior.

Esta é uma regra explícita de associação por nome/localização, não uma confirmação do identificador Apple. Não procura apenas nomes parecidos, não associa RAW, não atravessa pastas e não resolve automaticamente nomes ambíguos como JPEG e HEIC do mesmo nome. Ficheiros sem associação continuam independentes. Em fornecedores com IDs opacos, sem uma hierarquia e nome verificáveis no URI, não inventa a pasta de origem: deixa os ficheiros separados. Exportações com nomes diferentes exigem renomear os dois ficheiros ou usar o modo independente.

Depois da associação, aplica Motion Photos incorporadas, os filtros de cada tipo e o agrupamento lado a lado. Assim, num grupo fotografia+MOV, desligar vídeos nos filtros deixa apenas a fotografia. A atualização de origens considera a coleção completa antes de filtrar os elementos já presentes, para não reintroduzir componentes ocultados pelo modo Live Photos. Os backups anteriores ao esquema 7 importam com ficheiros independentes, preservando o comportamento anterior.

## RAW através de pré-visualização incorporada

Extensões candidatas: DNG, NEF, NRW, ARW, CR2, ORF, RW2, PEF, SRW e RAF. A app usa AndroidX ExifInterface para tentar extrair a pré-visualização JPEG incorporada. O reconhecimento da extensão não garante que uma câmara/variante tenha uma pré-visualização acessível. Não existe revelação dos dados do sensor, conversão RAW completa, nem suporte CR3 neste lote.

O fornecedor privado `RawPreviewProvider` gera a imagem apenas quando é necessária. Mantém o URI original para identificar, ordenar, filtrar e recuperar a origem; o leitor recebe um URI privado de pré-visualização. Só aceita leitura e fontes locais/de documentos. Não altera os RAW nem cria JPEGs junto dos originais.

A extração limita o JPEG incorporado a 16 MiB, descodifica com o orçamento de imagem da app, aplica orientação/espelhamento e guarda JPEG normalizado na cache privada. A cache tem limite de 96 MiB, removendo os ficheiros mais antigos; o Android também pode limpá-la. Chaves incluem URI, data e tamanho. Quando os atributos não são conhecidos, volta a extrair em vez de reutilizar uma versão sem validação. O JPEG normalizado pode ter qualidade/resolução inferiores ao RAW e perde os metadados do ficheiro de pré-visualização; data de captura e identificação continuam a ser lidas do original. Não preserva HDR RAW. Sem JPEG incorporado compatível, segue o tratamento de falha da apresentação.

O filtro **RAW (pré-visualização JPEG)** é independente de JPEG normal. Seleções antigas que representavam todos os formatos passam a incluir os novos; subconjuntos explícitos continuam restritos.

## Contentores adicionais

- MPEG-PS: MPG, MPEG, MPE, VOB e MOD.
- MPEG-TS: acrescenta M2T e TOD aos TS/MTS/M2TS existentes.
- FLV.
- F4V, classificado como contentor MP4.
- 3G2, além de 3GP.

Os novos contentores entram na pesquisa e nos filtros. O Media3 continua a exigir suporte de extrator, vídeo e áudio. FLV tem limitações de procura; VOB não inclui menus DVD nem desencriptação. Não foram adicionados codecs nativos, FFmpeg ou transcodificação. 4K, HEVC, AV1, Dolby Vision, HDR e dois vídeos simultâneos continuam dependentes da box e da cadeia de apresentação.

## Informação pelo comando

Durante a apresentação, **INFO** ou **Menu** abre Informação do conteúdo. Também existe o botão **Informação** nos controlos abertos com OK. Nos pares, mostra separadamente Esquerda e Direita. A informação corresponde ao elemento atual e não altera a lista nem a opção de som.

Mostra nome, MIME, tamanho do original, dimensões disponíveis, proporção, captura e modificação. Quando o leitor disponibiliza os dados, acrescenta duração, codec/MIME do vídeo, resolução, fps, sinal PQ/HLG/SDR e formato/canais/frequência do áudio. Não confunde a indicação de transferência HDR com prova de que a TV está efetivamente a emitir HDR, nem inventa dados ausentes. RAW é identificado como pré-visualização. A leitura dos cabeçalhos corre no executor, com cancelamento da publicação se o elemento já tiver mudado.

## Ordem por data de captura

**Opções da apresentação → Ordem → Data de captura (antigas primeiro)** lê DateTimeOriginal nas fotografias, usando offset e frações de segundo quando disponíveis. Na sua ausência, tenta DateTimeDigitized. Sem offset, interpreta a hora no fuso local da box. Nos vídeos usa a data comunicada pelo MediaMetadataRetriever.

Sem captura válida, usa a modificação do ficheiro; se nenhuma existir, coloca no fim. Empates são ordenados pelo nome. Não usa o nome do ficheiro como data e não altera os metadados. A primeira preparação pode ser mais demorada porque precisa de consultar os ficheiros. O índice SQLite passa à versão 2, inclui captura e invalida os cabeçalhos antigos na migração para os reler. Os metadados continuam versionados por URI/data/tamanho.

## Protetor de ecrã Android TV

`MolduraDreamService` é um serviço DreamService real, registado e habilitado por predefinição, protegido por BIND_DREAM_SERVICE. Não abre a atividade por cima de outras apps. **Protetor de ecrã Android TV → Abrir definições** conduz à escolha do protetor do sistema.

**Uma app normal não pode substituir silenciosamente o protetor selecionado pelo Android.** O utilizador tem de escolher MolduraTV nas definições do equipamento e definir o início por inatividade. Algumas boxes substituem ou não disponibilizam esse ecrã; nesses casos a app apresenta a indicação para procurar Protetor de ecrã/Modo ambiente no firmware. A disponibilidade do serviço não prova a sua seleção nem a execução pelo sistema.

O protetor usa um snapshot das pastas/opções da app ao iniciar: filtros, captura, Live/Motion Photos, intervalo, repetição, divisão e som. Usa a mesma preparação de coleção e a mesma fábrica de leitores com limite de imagem. Tem no máximo dois leitores, FIT sem cortes, pares de fotografias e vídeos verticais elegíveis. Nos vídeos: esquerda completa com som; direita reinicia e passa completa com som; o outro lado repete sem som; depois avança. Vídeos cujas dimensões reais invalidem o par e pares com falha passam à tentativa individual. Os conteúdos indisponíveis são retirados da sessão.

A remoção de um volume local pode retirar os seus conteúdos; a montagem desencadeia pesquisa. Uma coleção atualizada é aplicada num avanço normal, sem interromper a fase de som em curso. O protetor faz uma pesquisa nova ao iniciar, não usa um snapshot de coleção desatualizado como origem final. Não tem a cobertura SDR de transição da atividade. A opção Preencher com corte da atividade não é aplicada: o protetor apresenta os conteúdos inteiros. As opções alteradas durante a sua execução entram no próximo início.

Por ser não interativo, o primeiro evento de comando/toque termina o protetor, regressando ao sistema. Parar, desligar a janela ou destruir o serviço cancela o trabalho pendente e liberta os dois leitores; não mantém som em segundo plano. Perda de foco áudio suspende a reprodução até terminar/reabrir o protetor. Não é autoarranque após ligar a box, nem contorna permissões de armazenamento.

## Validação e aceitação

16 novos testes JVM cobrem associação, isolamento de pastas/ambiguidade/IDs opacos, datas/fusos/fallback, classificação de formatos, migração dos filtros e backups. Total: 90 testes unitários. Sete novos testes Android foram preparados e compilados: ordem EXIF/índice, associação+filtro, RAW sintético com JPEG incorporado e preservação dos bytes, manifesto/fornecedor privado, migração do índice, informação por INFO e duas fases do leitor do protetor/libertação. **Não executados.**

Antes de aceitar na TV/box: usar Live Photos exportadas de um iPhone; RAW originais de câmaras diferentes (incluindo ausência de thumbnail); MPEG-PS/TS/FLV/3G2 com áudio; captura com e sem fuso; INFO/Menu por comando; ativação/inatividade/saída do DreamService real; USB durante as duas fases; HDR/SDR e sessão longa em processo de 32 bits. Os testes do leitor do protetor não substituem a ativação real do serviço pelo Android. Nenhum APK foi gerado pelo agente.

Referências: [DreamService](https://developer.android.com/reference/android/service/dreams/DreamService), [AndroidX ExifInterface](https://developer.android.com/reference/androidx/exifinterface/media/ExifInterface), [formatos Media3](https://developer.android.com/media/media3/exoplayer/supported-formats).
