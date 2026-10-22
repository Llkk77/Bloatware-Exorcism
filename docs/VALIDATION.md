# Coleta de validação e fixtures

## Relatório de validação

Em Resurrection, registre o teste funcional da operação. As opções começam em Não testado. Preencha somente o que conferiu. Exporte o relatório JSON pelo botão da mesma tela.

O formato `bloatware-exorcism.validation/1` inclui device (modelo, ROM, região quando reconhecível pelo código MIXM/CNXM, fingerprint, Android e usuário), operations (ação, resultado e estados), validations (relatos) e catalogValidationPatch (propostas de validatedBuilds).

Para propor validatedBuilds, a alteração precisa permanecer no estado registrado, sem reversão, com câmeras, Wi-Fi, chamadas, notificações e reinício aprovados. NFC e Bluetooth aceitam Não se aplica. O app exige um identificador de boot diferente daquele da operação e `sys.boot_completed=1`. A observação é rotulada como relato funcional do usuário; não é comprovação independente. Se o último relato para pacote/build/usuário/ação não cumprir os critérios, o relatório não inclui a proposta anterior correspondente.

Operações da versão 0.1.0 não possuem identificador de boot original. Elas continuam exportáveis e podem receber relatos, mas não passam automaticamente pelo critério de novo boot observado.

O catálogo embarcado não é modificado automaticamente. Quem mantém o catálogo deve revisar o relatório, preservar origem/evidência e nunca generalizar o resultado de uma build para todas as ROMs.

## Fixture real

Abra um pacote em Detect Demons ou Deep Exorcism e escolha Exportar fixture real. A captura exige root e exporta o bloco escalar do usuário de dumpsys package com metadados do aparelho. O estado esperado vem da API PackageManager, lida antes/depois; o relatório identifica se a observação ficou estável.

Os inodes CE/DE são zerados. Não há dump completo, serial, dados dos apps ou captura de logs globais. Revise o arquivo antes de compartilhar. O hash detecta corrupção do arquivo; não prova que uma captura veio de hardware real.

No projeto:

```sh
python3 scripts/import_fixture.py captura.json --destination tests/fixtures/device
javac -d /tmp/exorcism-tests app/src/main/java/dev/bloatware/exorcism/Policy.java tests/FixtureTest.java
java -cp /tmp/exorcism-tests dev.bloatware.exorcism.FixtureTest tests/fixtures/device
```

Preserve o JSON lateral da captura. Se o parser rejeitar ou discordar do estado independente, mantenha a regressão e investigue o formato; não ajuste o estado esperado para fazer o teste passar.

Não foram incluídas capturas reais nesta entrega. As 11 fixtures existentes são sintéticas e testam apenas os casos de formato e rejeição documentados.
