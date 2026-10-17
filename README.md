# MolduraTV

App de álbum multimédia para Android TV, Google TV e boxes Android. Fotografias e vídeos na mesma apresentação, com navegação pelo comando. Nome provisório. Versão atual: **0.11.0** (`versionCode 12`).

## Utilização

1. Abrir o projeto no Android Studio, gerar o APK e instalá-lo em Android 8.0 ou superior.
2. Abrir **MolduraTV** no menu de aplicações.
3. Em **Permissões → Autorizar / gerir**, autorizar o acesso aos ficheiros partilhados para ler o armazenamento interno e os discos. As pastas da própria app não exigem essa autorização.
4. Escolher **Adicionar pasta**, navegar dentro da própria MolduraTV até à pasta pretendida e escolher **Usar esta pasta**. Repetir para juntar várias origens. As subpastas são incluídas.
5. Em **Opções da apresentação**, escolher a duração das fotografias, ordem, modo inteligente, enquadramento, som e repetição.
6. Escolher **Iniciar apresentação**.

Por predefinição, cada fotografia dura 60 segundos. Os vídeos mantêm a duração original e o som. A coleção repete. A app apenas lê os conteúdos; retirar uma pasta da coleção não apaga ficheiros.

## Fotografias e vídeos lado a lado

Em **Opções da apresentação → Apresentação**, escolher:

- **Automático** (predefinido): forma pares de fotografias elegíveis e vídeos verticais quando as duas áreas ficam suficientemente legíveis. Evita pares que deixariam uma imagem demasiado estreita ou baixa. Os formatos 9:16, 9:20, 9:21 e 3:4 continuam elegíveis numa TV 16:9.
- **Individual**: apresenta um elemento por vez.
- **Dois lado a lado**: forma pares elegíveis sem aplicar o limiar de tamanho do modo automático. Não força vídeos horizontais nem fotografias 16:9 a formar pares.

**Fotografias horizontais 16:9 aparecem sempre sozinhas**, mesmo num ecrã ultralargo. Fotografias quase tão largas como o ecrã também ficam individuais. Fotografias 4:3 e verticais podem formar pares. Vídeos são emparelhados apenas se forem verticais. Não se misturam fotografias com vídeos. A orientação EXIF das fotografias e a rotação dos vídeos são consideradas quando os metadados estão disponíveis.

Nas ordens **Nome** e **Data**, os pares usam elementos consecutivos, preservando a sequência. Na ordem **Aleatória**, a app procura um formato semelhante entre os próximos elementos do mesmo tipo (até 12 candidatos), sem atravessar uma mudança entre foto/vídeo. A ordem aleatória continua a variar entre sessões e nenhum elemento é duplicado ou descartado por esse agrupamento.

Os dois conteúdos são apresentados inteiros, encostados e sem separador. Partilham a mesma altura; cada largura respeita a proporção original. Podem sobrar faixas pretas nas margens exteriores: preencher todo o ecrã nem sempre é possível sem cortar ou deformar. O modo inteligente nunca aplica corte às fotografias nem aos vídeos verticais identificados, mesmo que o preenchimento com corte esteja ligado para vídeos horizontais/modo clássico.

Ao abrir um vídeo, a app confirma as dimensões e proporção dos píxeis comunicadas pelo leitor. Corrige a geometria uma vez quando ambos os lados estão identificados e mantém-na durante as duas fases de som. Se o leitor identificar um vídeo horizontal onde os metadados indicavam retrato, o par passa a elementos individuais.

Nas mudanças de apresentação SDR, a app tenta conservar uma imagem temporária da apresentação anterior até a seguinte estar visível. A cópia tem resolução limitada e não abre leitores adicionais. Se a cópia falhar, a troca continua normalmente; não é prometida ausência total de ecrãs pretos em todos os equipamentos. Vídeos identificados como HDR e fotografias identificadas como gamut alargado/gainmap não são convertidos para essa imagem SDR temporária. O fundo permanece preto, sem efeito desfocado.

O par de fotografias usa **um único intervalo**: com 60 segundos definidos, ambas ficam visíveis durante 60 segundos. O intervalo das fotografias não corta vídeos.

**Vídeos em duas fases, com som de um de cada vez:** primeiro, o da esquerda passa integralmente com som enquanto o da direita passa sem som e repete se necessário. Quando o da esquerda termina, o da direita **recomeça do início, agora com som**, e passa integralmente. Durante essa segunda fase, o da esquerda passa em repetição sem som. Quando o da direita termina a sua vez com som, a app avança para a apresentação seguinte. Assim, cada vídeo tem uma reprodução completa com som, independentemente das durações ou das repetições anteriores sem som.

O reinício da pré-visualização sem som não reinicia nem pausa o vídeo com som. A duração do par corresponde às duas reproduções com som, mais o tempo de preparação necessário. **Desligar som** silencia ambos, mantendo as duas fases. Pausa e avanço pelo comando atuam sobre o par. Nos pares de vídeos, os controlos apresentam pausa/retoma e navegação, sem uma barra de procura única para dois tempos diferentes.

Elementos sem par ou sem dimensões conhecidas aparecem individualmente; vídeos horizontais/quadrados continuam individuais. O modo **Individual** repõe um elemento por vez. Ao sair da app, ambos os leitores são libertados; ao regressar, as duas posições e a vez de reprodução com som são recuperadas com a apresentação pausada.

Se uma das fotografias de um par falhar, a outra continua sozinha. Se falhar a reprodução de dois vídeos em simultâneo, a sessão passa a tentar os dois individualmente, preservando a sua ordem. A capacidade de reproduzir dois vídeos ao mesmo tempo, sobretudo 4K/HDR, precisa de confirmação na box. Os originais não são alterados nem convertidos numa montagem. Os testes Android preparados foram apenas compilados; não foram executados nesta versão.

**Comando:** OK abre os controlos do leitor; com os controlos ocultos, esquerda/direita muda de elemento; Reproduzir/Pausa controla a reprodução; Voltar regressa à coleção. Os botões físicos de volume controlam o volume do Android/TV. O ecrã mantém-se ligado durante a apresentação. Ao sair para outra app, o leitor liberta os recursos; ao regressar, a sequência fica pausada na posição anterior. Após o processo ser encerrado pelo sistema, a app reabre na coleção.

## Origens e limites atuais

- USB, disco externo e armazenamento interno: navegador incluído na MolduraTV, sem depender de gestores de ficheiros nem do seletor Android. São apresentados os volumes montados, as pastas da própria app e a opção de introduzir um caminho absoluto.
- **Permissões** mostra o estado de leitura/escrita, abre a autorização de todos os ficheiros no Android 11+ e pede permissões de armazenamento no Android 8–10. Inclui acesso às definições da app se for necessário gerir uma autorização recusada.
- As áreas privadas de outras apps e certas pastas do sistema continuam protegidas pelo Android. As pastas próprias da MolduraTV são apresentadas diretamente, incluindo a pasta externa dentro de `Android/data/pt.molduratv/files`, quando o volume está disponível. Ficheiros nessas pastas são removidos ao desinstalar a app.
- Rede/NAS: opção separada **Seletor do Android / rede**, quando existir um seletor/fornecedor compatível. Instalar um gestor de ficheiros não garante esse suporte. **Esta versão não tem cliente SMB/WebDAV próprio**, nem descoberta de NAS. Uma pasta de rede montada como diretório e acessível pelo Android também pode ser indicada por caminho.
- Várias pastas são agregadas. Entradas com o mesmo URI são deduplicadas; fornecedores diferentes podem atribuir URIs diferentes ao mesmo ficheiro físico.
- A coleção é verificada ao abrir/regressar à app e após eventos de montagem/remoção de volumes; também pode ser atualizada manualmente. Pastas sem acesso produzem um aviso e não impedem a leitura das restantes.
- A ordem pode usar nome, modificação, aleatória ou **data de captura**. Captura usa metadados EXIF/vídeo e recorre à modificação quando não há data válida. A ordem aleatória é gerada ao iniciar e repete essa sequência.
- Ficheiros que falham são retirados da sessão. Após 30 segundos em buffering ativo, tenta-se o seguinte. Se todos falharem, a app regressa à coleção.
- JPEG, HEIC/HEIF, PNG, WebP, BMP, AVIF e os contentores de vídeo comuns são candidatos à reprodução. A extensão reconhecida não garante descodificação.
- 4K/1080p/720p/480p, codecs, áudio e HDR dependem do dispositivo, versão Android, ficheiro e ecrã. Não existe conversão/transcodificação nesta versão. HEIC e HDR precisam de validação real; AVIF exige suporte do sistema.
- Os conteúdos são processados pelo Media3; não há filtros artificiais para aumentar resolução nem promessa de HDR em equipamento SDR.

## Live Photos, RAW, captura, informação e protetor — 0.11.0

- **Live Photos:** em Opções da apresentação, associar JPEG/HEIC/HEIF e MOV com o mesmo nome-base e pasta. Escolher fotografia, vídeo, ambos juntos ou ficheiros independentes. Casos ambíguos e fornecedores com IDs opacos ficam separados. A predefinição é fotografia e vídeo juntos.
- **RAW:** tenta a pré-visualização JPEG incorporada em DNG, NEF/NRW, ARW, CR2, ORF, RW2, PEF, SRW e RAF. Não revela dados do sensor nem altera originais. Cache privada limitada; sem pré-visualização compatível, aplica o tratamento de falhas. Incluído nos filtros.
- **Vídeo:** pesquisa/filtros incluem MPEG/MPG/MPE/VOB/MOD, FLV, F4V, M2T/TOD e 3G2. Contentor reconhecido não garante codecs compatíveis. Sem menus DVD, codecs adicionais ou transcodificação.
- **Informação:** INFO/Menu no comando, ou botão Informação nos controlos, mostra detalhes de cada lado: formato, tamanho, dimensões, datas e dados de vídeo/áudio disponíveis no leitor.
- **Ordem pela captura:** Opções → Ordem → Data de captura; usa EXIF/vídeo, com alternativa pela modificação. Metadados sem fuso usam a hora local da box.
- **Protetor de ecrã Android TV:** serviço nativo incluído e habilitado por predefinição. No ecrã inicial, abrir Protetor de ecrã Android TV → Abrir definições e selecionar MolduraTV. A app não pode substituir silenciosamente a escolha do sistema. Usa as pastas/opções da app, apresenta os conteúdos inteiros e termina ao usar o comando. Disponibilidade e início automático dependem do firmware.

Instruções e limites em [Live Photos, RAW e protetor de ecrã](docs/LIVE-RAW-DREAM.md). Backups atuais no esquema 7; testes de equipamento continuam pendentes.

## Filtros de reprodução — 0.10.0

Em **Opções da apresentação → Filtros de reprodução**, definir regras independentes para **Fotografias** e **Vídeos**. Permite incluir/excluir cada tipo e combinar várias orientações, proporções e formatos de ficheiro. Exemplo: fotografias apenas em 4:3/3:4 juntamente com vídeos apenas verticais. Dentro de cada lista basta corresponder a uma escolha; entre listas é necessário cumprir todas. Os filtros são aplicados antes dos pares, também a origens recuperadas, sem alterar os originais. As opções ficam guardadas nos backups esquema 6; backups anteriores importam sem filtros.

Inclui **Todos / Nenhum**, **Ver seleção atual** e reposição de todos os conteúdos. Se nada corresponder, a app avisa antes de abrir o leitor. A contagem da coleção mantém o total encontrado nas pastas. Dimensões desconhecidas e outras proporções têm opções próprias. Detalhes, exemplos e limites em [Filtros de reprodução](docs/PLAYBACK-FILTERS.md).

## Memória, índice, diagnóstico e Motion Photos — 0.9.0

- Fotografias grandes são descodificadas com um limite adaptado à resolução, à memória disponível para a app e ao processo de 32/64 bits. Em boxes com pouca memória, a resolução apresentada pode ser reduzida para limitar o tamanho do bitmap. Os originais e os vídeos não são convertidos.
- Um índice privado guarda a última coleção e metadados com data/tamanho conhecidos. Permite começar a apresentação da coleção anterior enquanto a app verifica as pastas. A informação guardada é provisória: a verificação atualiza a coleção e os conteúdos indisponíveis continuam sujeitos ao tratamento de falhas.
- **Diagnóstico** apresenta as últimas falhas e eventos de armazenamento. Permite **Limpar histórico** e **Reconstruir índice**, sem apagar fotografias, vídeos ou pastas selecionadas.
- A remoção de um volume local retira os seus elementos da sessão e tenta continuar nas outras origens. Ao voltar a ligar o disco, novos elementos encontrados podem entrar nas próximas apresentações, sem interromper a vez de reprodução com som do par atual. Depende dos eventos enviados pelo Android; fornecedores de documentos/rede podem exigir atualização manual.
- Em **Opções da apresentação → Motion Photos**, escolher **Fotografia** (predefinido), **Movimento** ou **Fotografia e movimento**. Reconhece metadados XMP de determinadas fotografias com vídeo incorporado; a reprodução depende do Media3 e do sistema. Para fotografia e MOV separados, existe agora a opção Live Photos, com associação pelo mesmo nome-base na mesma pasta.

Detalhes e limites em [Multimédia e recuperação](docs/MEDIA-AND-RECOVERY.md). O backup inclui as opções, mas não inclui o índice nem o histórico de diagnóstico.

## Guardar e restaurar backups

No ecrã inicial, abrir **Backups**:

- **Guardar backup**: navegar no armazenamento da própria app e escolher a pasta de destino do ficheiro `MolduraTV-backup-AAAAmmdd-HHmmss.json`. Inclui a seleção de pastas, o tempo das fotografias, a ordem, o modo inteligente, o enquadramento, o som, a repetição, os filtros e os modos Motion Photos/Live Photos. As fotografias e os vídeos não são copiados. Um backup existente com o mesmo nome é preservado.
- **Restaurar backup**: escolher um ficheiro, rever o resumo e confirmar **Restaurar**. A seleção de pastas e as opções atuais são substituídas. **Cancelar** mantém a configuração anterior. Ficheiros inválidos são recusados antes de alterar a configuração.
- **Rever acesso às pastas**: escolher uma pasta e voltar a associá-la a uma localização; pastas locais usam o navegador incluído e pastas de documentos usam o seletor Android.
- **Outros locais do Android / rede**: exportação/importação opcional através do seletor Android, quando este existir.

O backup contém as referências das pastas; as autorizações de acesso são geridas pelo Android e não são transferidas no ficheiro. Depois de reinstalar a app, mudar de box ou mudar uma pasta, pode ser necessário autorizá-la novamente. Uma pasta marcada como **Autorizada** tem uma autorização guardada, mas o disco/rede ainda precisa de estar disponível. Cancelar a autorização preserva a seleção anterior.

O ficheiro JSON pode ser guardado numa pasta gravável escolhida no navegador, incluindo pen, disco ou armazenamento interno. Para o conservar após desinstalar a app, escolher uma localização fora das pastas próprias da MolduraTV. O esquema 7 guarda também a opção Live Photos, filtros e Motion Photos. Importa os esquemas 1 a 6; os anteriores ao 6 usam filtros livres e os anteriores ao 7 mantêm Live Photos independentes, usando Fotografia para Motion Photos quando essa opção não existe: uma opção inteligente anteriormente desligada passa a Individual; quando ligada, passa a Automático. Admite até 512 pastas e limita a leitura a 1 MiB. O acesso de escrita depende também de o disco estar montado para escrita.

## Organização

```text
app/src/main/java/pt/molduratv/  Interface, definições, pesquisa e coleção
app/src/main/res/               Tema e identidade visual TV
app/src/test/                   Testes das regras da coleção
docs/                          Plano, arquitetura e validação
scripts/                       Validar sem gerar APK; utilitários manuais
releases/                      Artefactos históricos das primeiras versões
```

## Atualizar a aplicação pelo GitHub

O botão **Atualizações** procura uma Release estável em [renafcampos27/Moldura_Digital](https://github.com/renafcampos27/Moldura_Digital/releases). A pesquisa é iniciada pelo utilizador. Se existir uma versão superior, apresenta a versão e o tamanho, permite descarregar e abre o instalador do Android depois de verificar o ficheiro. O Android pode pedir autorização para instalar atualizações a partir da MolduraTV.

O atualizador verifica o SHA-256 indicado pelo GitHub, o tamanho, a identificação `pt.molduratv`, a versão, o `versionCode`, o Android mínimo e a mesma assinatura da aplicação instalada. A instalação exige confirmação no Android. As operações de rede têm limites de tempo e tamanho e podem ser canceladas.

O utilizador gera e assina cada APK no Android Studio e anexa-o manualmente a uma Release pública, com tag como `v0.4.1`. Não são gerados pacotes por scripts ou workflows deste projeto. Seguir o [guia de publicação e teste das atualizações](docs/GITHUB-UPDATES.md).

Se não houver Releases ou não existir um APK compatível, a app apresenta uma mensagem. A consulta ao endpoint `releases/latest` feita em 16/10/2026 devolveu HTTP 404: ainda não havia uma Release publicada na data desta implementação.

## Desenvolvimento

A variante **release** tem R8 ativo: remoção de código não utilizado, otimização e ofuscação, além de redução e otimização de recursos. A depuração fica desativada. A configuração usa `proguard-android-optimize.txt` e as regras das bibliotecas; não preserva indiscriminadamente pacotes inteiros. Ver [configuração e validação da release](docs/RELEASE.md).

Abrir esta pasta no Android Studio. Requisitos: JDK do Android Studio, Android SDK Platform 37 e Build Tools instalados. O projeto usa AGP 9.4.1, Gradle 9.8.0 e Media3 1.11.1, com versões fixas. `local.properties` contém apenas o caminho local do SDK e é ignorado pelo Git.

**O utilizador gera sempre os APKs no Android Studio.** O agente trabalha no código, documentação e validação sem empacotamento. Os APKs já existentes em `releases/` são históricos e não são atualizados por estes scripts.

No PowerShell, `./scripts/build.ps1` compila as classes, executa os testes unitários e a análise estática; não gera nem copia APKs. Para instalar manualmente um APK já gerado no Android Studio: `./scripts/install.ps1 -ApkPath 'caminho-do-apk'`. A instalação necessita de depuração autorizada no equipamento.

`./scripts/build.ps1 -Release` executa o R8, a otimização de recursos e a análise estática release, e verifica os testes unitários. Produz apenas os resultados intermédios e relatórios de otimização, sem gerar, assinar ou copiar APKs. Ao publicar uma versão gerada no Android Studio, conservar o `mapping.txt` dessa geração para interpretar relatórios de erros ofuscados.

Os testes de integração que exigem a geração de pacotes devem ser executados pelo utilizador no Android Studio. O utilitário `./scripts/test-device.ps1 -Serial emulator-5554` apenas executa os testes se a app e o respetivo pacote de testes já estiverem instalados; não gera nem instala pacotes. Os testes usam imagens e vídeo sintéticos e substituem temporariamente as opções da coleção, repondo-as no fim de cada teste.

Consultar [plano e arquitetura](docs/PLAN.md) e [validação](docs/VALIDATION.md) antes de considerar esta versão pronta para uso permanente.







