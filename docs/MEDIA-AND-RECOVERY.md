# Multimédia, índice e recuperação — 0.9.0

## Fotografias grandes

O leitor usa o descodificador de fotografias do Media3 com um limite de saída calculado a partir da dimensão do ecrã, do heap máximo da app, do uso de pares e de Process.is64Bit(). O lado máximo fica limitado a 2048 píxeis em processos de 32 bits e a 4096 em processos de 64 bits; o orçamento de memória pode impor um valor inferior. O cálculo reserva conservadoramente oito bytes por píxel e usa uma fração menor do heap nos pares.

O valor enviado ao descodificador considera a amostragem por potências de dois. A imagem efetiva pode ficar abaixo da resolução do ecrã em boxes com pouco heap. O limite diz respeito ao bitmap descodificado, não à memória total da app, buffers de ficheiros, superfícies de vídeo ou RSS. As imagens retidas no leitor principal são libertadas ao sair. Não há conversão dos originais, transcodificação de vídeo ou medições de ganhos de memória/FPS no dispositivo.

## Índice privado

SQLite guarda a última lista de ficheiros para a seleção exata de pastas. A publicação do snapshot é transacional; cancelamento ou falha não grava uma coleção parcial. A coleção guardada permite iniciar durante a pesquisa de verificação. Uma origem entretanto removida pode aparecer provisoriamente até essa verificação ou ao tratamento de erro.

O snapshot admite até 50 000 entradas. Acima desse limite, a biblioteca continua a ser pesquisada e usada em memória, mas não fica um snapshot parcial para a abertura seguinte. Os metadados persistidos também são limitados a 50 000 registos. São reutilizados apenas com URI, tamanho e data de modificação positivos e coincidentes; antes de os consultar, lê-se a versão atual do ficheiro. Versões desconhecidas voltam a ler o cabeçalho. O índice depende da correção dos atributos comunicados pelo sistema/fornecedor; alterações com exatamente o mesmo tamanho e data podem exigir reconstrução manual.

O índice fica na área privada da app e não pertence ao backup de configuração. **Diagnóstico → Reconstruir índice** elimina os dados derivados e volta a pesquisar. Não altera os originais nem concede acesso a pastas. Erros na base de dados deixam a pesquisa normal disponível.

## Diagnóstico

O histórico privado guarda até 50 entradas, com campos limitados e contagem de repetições iguais. Mostra o nome do elemento, hora, categoria e descrição; erros do leitor incluem o código Media3. Não guarda fotografias nem vídeos. **Limpar histórico** apaga estes registos. Não existe envio de telemetria nem exportação automática do histórico.

## Remover e voltar a ligar um disco

Enquanto a atividade está visível, observa montagem, ejeção e remoção de volumes comunicadas pelo Android. Na remoção, filtra os URIs locais pertencentes ao caminho desse volume. Se apenas um lado do par desaparecer, tenta apresentar o lado saudável individualmente; pares de outras origens preservam o estado. As pastas continuam selecionadas.

Ao montar um disco, pesquisa novamente sem substituir o par em reprodução. Elementos novos ficam pendentes e são acrescentados no avanço seguinte, após a conclusão normal das duas fases de som, ou num avanço manual. Não volta a inserir URIs que já estejam na sessão. A ordem da sessão existente é preservada; uma nova apresentação volta a aplicar a ordenação normal à coleção completa.

O comportamento depende dos eventos do firmware e do acesso ao armazenamento. URIs de fornecedores de documentos não permitem sempre relacionar o elemento com o caminho do volume: nesses casos aplica-se o tratamento normal de falhas e a pesquisa da coleção. Regressar à app também verifica as pastas. Pode usar a atualização manual quando um fornecedor não sinaliza alterações. A app não monta discos, não instala drivers e não recupera autorizações revogadas automaticamente.

## Motion Photos

Em **Opções da apresentação → Motion Photos**:

- **Fotografia**: comportamento predefinido, apresenta a imagem estática.
- **Movimento**: tenta reproduzir o vídeo incorporado reconhecido; se a tentativa individual falhar, tenta a fotografia estática.
- **Fotografia e movimento**: apresenta a fotografia pelo intervalo escolhido e depois tenta o vídeo incorporado. Se o vídeo falhar, avança, pois a fotografia já foi apresentada.

A identificação usa XMP limitado, namespaces e campos reconhecidos de Motion Photo/MicroVideo, com validação dos intervalos do vídeo incorporado. XML inválido, declarações de entidades e dados incoerentes não são aceites. O MIME JPEG/HEIC original é mantido para o extrator do Media3; a presença de metadados não garante um vídeo descodificável. O vídeo segue as regras de apresentação, incluindo pares quando elegível. Fotografias normais permanecem estáticas.

Não associa automaticamente JPEG+MOV separados, nem promete todas as variantes Apple Live Photos, Samsung/Google ou HEIC. Não foram adicionadas bibliotecas nativas de codecs. AVIF, HEIC/HEIF, HDR e codecs de vídeo continuam dependentes do Android, equipamento e ficheiro. GIF animado não foi adicionado neste lote.

Referências de implementação: [imagens e Motion Photos no Media3](https://developer.android.com/media/media3/exoplayer/images), [formato Motion Photo](https://developer.android.com/media/platform/motion-photo-format) e [descodificador da versão Media3 usada](https://raw.githubusercontent.com/androidx/media/1.11.1/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/image/BitmapFactoryImageDecoder.java).

## Aceitação no equipamento

Estes cenários permanecem pendentes numa TV/box com a aplicação gerada pelo utilizador:

1. Fotografias JPEG/HEIC grandes, individuais e em pares, num processo de 32 bits; observar resolução, memória, orientação, cores e sessão prolongada.
2. Reabrir uma coleção grande com índice; começar durante a verificação; substituir uma imagem; retirar origens; reconstruir o índice; confirmar ausência de bloqueios.
3. Retirar USB durante cada lado/fase de um par, continuar noutra origem e voltar a ligar o disco. Confirmar inclusão dos recuperados sem cortar o vídeo com som, sem duplicação e com repetição ligada/desligada.
4. Motion Photos originais de telemóveis, nos três modos, incluindo ficheiros incompatíveis. Confirmar duração, som, rotação, fallback e backup/restauro da opção.
5. Provocar uma falha de leitura/codec, consultar o diagnóstico, repetir, limpar e reconstruir. Testar release com R8, comando e saída/regresso da app.

Os testes Android de SQLite, descodificador, Motion Photo e remoção simulada foram preparados e compilados, mas não executados. Os testes JVM validam regras e contratos; não demonstram estes comportamentos no dispositivo.

## Extensões posteriores (0.11.0)

Fotografia e MOV separados dispõem agora de associação por nome-base/pasta configurável em Live Photos. RAW usa JPEG incorporado com cache privada; mais contentores entram na pesquisa e filtros. A ordem pela captura e o protetor nativo partilham a preparação da coleção. Os detalhes e limites atuais estão em LIVE-RAW-DREAM.md; as secções anteriores descrevem a introdução na versão 0.9.0.

