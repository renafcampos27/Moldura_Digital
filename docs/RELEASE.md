# Release otimizada — 0.11.0

## Configuração

Em `app/build.gradle`, a variante release ativa `minifyEnabled true`, `shrinkResources true`, `debuggable false` e `proguard-android-optimize.txt`. O R8 elimina código não utilizado, otimiza o restante e reduz nomes de classes/membros quando permitido. A redução otimizada de recursos está integrada no AGP 9.4.1 usado pelo projeto; não precisa de flags experimentais em `gradle.properties`. A etapa `optimizeReleaseResources` processa os recursos binários restantes.

As regras padrão do Android, as regras geradas a partir do manifesto/XML e as regras fornecidas pelas dependências são aplicadas em conjunto. Foram confirmadas as regras do Media3 na configuração efetiva e a retenção do recurso `update_paths`, usado pelo FileProvider das atualizações. A app não usa reflexão em produção nem serialização baseada em nomes de campos; os backups usam chaves JSON explícitas. Os testes Android que usam reflexão não fazem parte do código da release.

`app/proguard-rules.pro` é o ponto para futuras exceções específicas. Não contém regras globais `keep`, `dontwarn`, `dontoptimize`, `dontshrink` ou `dontobfuscate`. Os codecs e funcionalidades Media3 não foram removidos manualmente, e não foram eliminadas línguas ou arquiteturas da app.

A assinatura existente foi preservada: `signingConfig signingConfigs.debug`. Esta alteração não cria nem substitui chaves. As atualizações continuam a exigir a mesma assinatura da versão instalada. A escolha de outra chave no Android Studio exige gerir essa compatibilidade.

## Validação sem APK

Executar `./scripts/build.ps1 -Release`. O fluxo solicita:

```text
:app:minifyReleaseWithR8
:app:optimizeReleaseResources
:app:lintRelease
:app:testDebugUnitTest
```

O grafo foi revisto previamente com `--dry-run`: não chega à geração/assinatura/instalação de APK. As tarefas de recursos produzem ficheiros intermédios de recursos, não aplicações instaláveis. O R8 produz DEX otimizado e relatórios. O script normal continua a validar debug sem executar R8.

Validação de 17/10/2026: R8 executado com sucesso; recursos reduzidos e otimizados; 90 testes unitários aprovados; lint debug/release com 0 erros e 8 avisos existentes. Os testes unitários correm na JVM sobre a variante debug; não executam o DEX ofuscado numa box. As classes dos testes Android foram compiladas. Não foram gerados APKs nem executados testes Android da release.

Os tamanhos e hashes dos resultados intermédios constam de `docs/evidence/0.11.0/r8-artifacts.json`. Estes valores não são o tamanho de um APK e não demonstram uma redução percentual face à mesma versão sem R8, nem ganhos de fluidez. Os artefactos antigos não são uma base de comparação controlada.

## Geração no Android Studio e diagnóstico

Gerar a variante release no Android Studio aplica automaticamente estas opções. Guardar o `app/build/outputs/mapping/release/mapping.txt` da **mesma geração do APK publicado**, fora das pastas de build que podem ser limpas. Associá-lo à versão/código e ao hash desse APK. Não usar o mapping desta validação como se correspondesse a um APK publicado: não foi gerado nenhum.

O diretório de relatórios inclui também `configuration.txt`, `usage.txt`, `seeds.txt` e `resources.txt`. A evidência compacta em `docs/evidence/0.11.0/` guarda a configuração efetiva, recursos, lint, testes e hashes dos relatórios maiores, sem duplicar vários MB de mapping.

Na box, validar a release gerada pelo utilizador: arranque, comando, fotos 16:9 individuais, pares de fotos, duas fases de vídeos com som, permissões/USB, backups e atualizações pelo GitHub. Compilar com R8 não confirma este comportamento no equipamento.

Referência: [ativar a otimização com R8 — Android Developers](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization).



