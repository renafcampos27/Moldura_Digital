# Fluxo de trabalho

- O utilizador gera sempre os APKs no Android Studio. Não gerar, montar, copiar, assinar ou entregar APKs durante o trabalho do agente.
- Para validação local, usar apenas tarefas sem empacotamento, como `:app:compileDebugJavaWithJavac`, `:app:testDebugUnitTest` e `:app:lintDebug`.
- Não executar `assemble*`, `bundle*`, `package*`, `install*` ou `connected*AndroidTest`: estes fluxos podem gerar pacotes. Os testes Android que exigem empacotamento ficam para o utilizador executar no Android Studio.
- Os APKs e relatórios já existentes são históricos; não os apresentar como novas entregas nem os apagar sem pedido.
