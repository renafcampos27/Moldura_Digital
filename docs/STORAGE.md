# Armazenamento e permissões — 0.4.0

## Utilização numa box sem seletor de documentos

1. Abrir **Permissões → Autorizar / gerir**.
2. No Android 11 ou superior, ativar o acesso a todos os ficheiros para a MolduraTV. No Android 8–10, aceitar leitura/escrita. Voltar à app. Se a autorização tiver sido recusada definitivamente, usar **Definições da app**.
3. Abrir **Adicionar pasta** e escolher o armazenamento interno, o volume USB/cartão/disco montado ou uma das pastas da MolduraTV.
4. Entrar nas subpastas e escolher **Usar esta pasta**. A navegação também permite subir, regressar aos armazenamentos, atualizar discos e introduzir um caminho absoluto.
5. Iniciar a apresentação. A pesquisa inclui as subpastas e preserva os conteúdos originais.

A autorização pertence à MolduraTV: a autorização concedida a um gestor de ficheiros não é partilhada com esta app. A instalação de um gestor não garante que ele implemente o seletor de documentos do Android. A seleção local da MolduraTV funciona através do seu próprio navegador.

## Locais apresentados

- Armazenamento interno partilhado, normalmente `/storage/emulated/0`.
- Volumes montados reportados pelo Android, incluindo USB/SD/discos quando o sistema os reconhece.
- Pasta privada da app, obtida por `getFilesDir()`.
- Pastas próprias externas da app, obtidas por `getExternalFilesDirs()`, em cada volume disponível.
- Diretórios montados em `/storage` e opção de indicar um caminho para variantes de fabricantes ou montagens acessíveis.

Os ficheiros e pastas são listados fora da interface, com navegação por comando. Pastas inacessíveis produzem uma indicação de erro/permissões; os restantes locais continuam disponíveis. Se um disco for ligado depois de abrir o navegador, usar **Atualizar discos e locais**.

## Limites do Android

O acesso geral permite leitura/escrita nas áreas partilhadas autorizadas e nos volumes montados. Não elimina a proteção de dados privados de outras apps nem torna um volume só de leitura gravável. O Android continua a bloquear o acesso às áreas privadas de outras aplicações, incluindo diretórios de outras apps em `Android/data`. A MolduraTV apresenta diretamente as suas próprias pastas, sem precisar de atravessar esses diretórios protegidos.

O navegador não monta discos, não instala drivers e não descobre servidores SMB. Um disco deve estar montado e ser reconhecido pelo sistema. Uma pasta de rede montada como diretório pode ser aberta se o Android permitir o acesso; fornecedores de documentos continuam disponíveis separadamente em **Seletor do Android / rede**.

As pastas específicas da MolduraTV são removidas ao desinstalar a app. Para um backup que sobreviva à desinstalação, escolher USB ou armazenamento partilhado fora da pasta da app.

## Integração e compatibilidade

- A coleção aceita árvores `content://` e pastas locais `file:`. O leitor recebe URIs locais para reprodução pelo Media3.
- A pesquisa local é recursiva, respeita interrupção e evita repetir subárvores sobrepostas. Não segue links simbólicos encontrados dentro da árvore.
- A alteração de pastas durante uma pesquisa agenda nova pesquisa, para incluir a seleção mais recente.
- Os backups atuais usam esquema 7 para guardar Live Photos, filtros, modos de apresentação e Motion Photos; importam os esquemas 1–6 (sem associação de Live Photos nos anteriores ao 7 e sem filtros nos anteriores ao 6). O esquema 1 continua limitado a árvores de documentos; os esquemas 2 a 7 aceitam também caminhos locais absolutos, sem referências remotas `file://servidor` nem segmentos `..`. Uma opção inteligente anteriormente desligada migra para Individual; quando ligada, migra para Automático.
- Guardar/restaurar backup usa por predefinição o navegador incluído. A escrita local cria um novo ficheiro e não substitui silenciosamente um backup existente. A alternativa de documentos/rede permanece no menu de backups.
- As permissões são pedidas através das páginas do Android e nunca concedidas automaticamente pela app.

## Validação na box

Testar sem qualquer seletor de documentos instalado; autorização concedida/recusada; pastas próprias sem autorização geral; armazenamento interno; USB em leitura e escrita; disco só de leitura; remoção/religação do disco; seleção por comando; backup JSON local e restauração; importação de backup anterior; retoma após sair das definições. A validação automática em computador não confirma estas particularidades de cada fabricante.

## Referências

- [Android: acesso a todos os ficheiros](https://developer.android.com/training/data-storage/manage-all-files)
- [Android: ficheiros específicos da aplicação](https://developer.android.com/training/data-storage/app-specific)
- [Android: volumes de armazenamento](https://developer.android.com/reference/android/os/storage/StorageVolume)

## Recuperação de volumes — 0.9.0

A app observa os eventos de montagem/remoção enquanto está visível e volta a pesquisar ao regressar. Retira da sessão os caminhos locais pertencentes ao volume removido, mantendo as pastas na configuração. Elementos de volumes recuperados entram num ponto de avanço da apresentação. O mecanismo não monta discos, não altera autorizações e não contorna as restrições Android. Os URIs de fornecedores de documentos nem sempre permitem identificar o volume: seguem o tratamento normal de falhas e a verificação da coleção. Ver MEDIA-AND-RECOVERY.md para os limites e o protocolo de aceitação.



As pré-visualizações RAW são guardadas exclusivamente na cache privada (até 96 MiB), não nas pastas lidas. O protetor Android reutiliza as autorizações da app e não pede autorizações nem monta discos por conta própria. Ver LIVE-RAW-DREAM.md.

