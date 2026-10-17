# Atualizações pelo GitHub

## Publicação manual

O repositório usado pela app é **renafcampos27/Moldura_Digital**. O atualizador consulta apenas Releases públicas estáveis; não necessita de login nem inclui tokens. O agente não gera APKs. A geração e assinatura são feitas pelo utilizador no Android Studio.

1. Em `app/build.gradle`, aumentar **ambos** os campos ao preparar uma versão nova. A versão atual do projeto já está definida como `versionName '0.11.0'` e `versionCode 12`.
2. No Android Studio, gerar um APK universal assinado com **a mesma chave da versão instalada**. Manter também `applicationId 'pt.molduratv'`. A app não instala AAB nem conjuntos de APKs divididos por arquitetura.
3. No GitHub, abrir [Releases](https://github.com/renafcampos27/Moldura_Digital/releases) e criar uma publicação com a tag **`v0.11.0`**. A tag deve corresponder ao `versionName` do APK, com prefixo `v` opcional e formato numérico `MAJOR.MINOR.PATCH`.
4. Anexar **um único APK elegível**, preferencialmente chamado **`MolduraTV-0.11.0.apk`**. Também são aceites `MolduraTV-v0.11.0.apk` ou `app-release.apk`. Se houver vários ficheiros elegíveis, a app pede para corrigir a publicação em vez de escolher um arbitrariamente.
5. Publicar como Release estável, sem marcar como draft/prerelease, e selecioná-la como a versão mais recente. O GitHub deve disponibilizar o campo `digest` SHA-256 do anexo. Se estiver ausente, remover e voltar a anexar o ficheiro através da interface do GitHub.
6. Na versão anterior instalada na box, abrir **Atualizações**, confirmar o descarregamento e seguir a confirmação do Android.

Uma instalação debug e uma release assinadas com chaves diferentes não são atualizações compatíveis. Usar a mesma chave em toda a sequência de versões instaladas; guardar a chave fora do repositório. O atualizador compara os certificados atuais e não implementa rotação de chave nesta versão.

## Comportamento da aplicação

- Mostra a versão instalada; pesquisa apenas quando o utilizador abre Atualizações.
- Compara versões numericamente: `0.11.0` é superior a `0.10.0`.
- Sem Release, sem rede, acesso limitado pelo GitHub ou publicação incompleta: mostra uma mensagem e preserva a configuração.
- Descarrega por HTTPS para a cache privada. Aceita o endereço exato do anexo no repositório e redirecionamentos apenas para domínios GitHub explicitamente permitidos.
- Limita a resposta de metadados a 1 MiB e o APK a 256 MiB. Verifica espaço disponível, tamanho final e SHA-256. Cancela transferências incompletas ou alteradas e remove o ficheiro parcial.
- Antes de abrir o instalador, verifica pacote, versão, `versionCode`, Android mínimo e certificados. Repete a validação depois de regressar das definições de instalação.
- Se necessário, abre a autorização Android de instalação por esta app. Cancelar não altera a instalação atual. A confirmação final pertence sempre ao instalador do Android.
- Partilha com o instalador apenas o ficheiro de atualização por `FileProvider`, através de uma autorização de leitura temporária. Não expõe as pastas da coleção.
- A cache contém, no máximo, o ficheiro completo mais recente e o parcial da operação em curso. O sistema pode limpar a cache; nesse caso é necessário voltar a descarregar.

## Teste manual no Android Studio e na box

1. Instalar uma versão inicial assinada no Android Studio e criar uma coleção com pastas/opções conhecidas.
2. Gerar e publicar uma versão com `versionName` e `versionCode` superiores, usando a mesma chave.
3. Na versão inicial, verificar descoberta, versão/tamanho apresentados, cancelamento e descarregamento.
4. Testar autorização recusada e concedida; confirmar que o instalador abre corretamente na box.
5. Instalar a atualização e confirmar que a coleção e as opções foram preservadas e que a nova versão já não é anunciada como atualização.
6. Validar interrupção da rede, falta de espaço, ficheiro truncado/alterado, chave diferente, pacote diferente e APK que exija um Android superior. Estes casos devem falhar sem substituir a app instalada.
7. Repetir o seletor de pastas e os backups após a migração para Activity Result APIs. Testar cancelamento e regresso após o Android destruir/recriar a atividade.

Em 16/10/2026, `GET /repos/renafcampos27/Moldura_Digital/releases/latest` devolveu **404**. Não foi possível testar uma atualização real sem uma Release e sem gerar pacotes, que fica a cargo do utilizador no Android Studio.

## Referências

- [GitHub Releases API](https://docs.github.com/en/rest/releases/releases)
- [Activity Result APIs](https://developer.android.com/training/basics/intents/result)
- [FileProvider e partilha de ficheiros](https://developer.android.com/training/secure-file-sharing/setup-sharing)
- [Assinatura de aplicações Android](https://developer.android.com/studio/publish/app-signing)




