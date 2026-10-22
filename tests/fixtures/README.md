# Origem das fixtures

Todas as fixtures incluídas nesta pasta são SINTÉTICAS: casos de formato e rejeição, sem alegação de captura de MIUI, HyperOS ou aparelho real. Os nomes não representam versões de ROM.

Capturas reais são exportadas no app pela inspeção de um pacote. Elas identificam modelo, versão, fingerprint, horário, usuário, pacote, saída reduzida de dumpsys e estado independente do PackageManager. Não incluem o dump completo nem serial do aparelho. Os campos de inode são zerados no trecho exportado.

Use scripts/import_fixture.py para converter uma captura real estável em regressão. A origem é mantida em um JSON lateral, com hash da captura, resultado do parser e estado esperado obtido pelo PackageManager. Uma discrepância não deve ser corrigida alterando o resultado esperado sem investigar a ROM.
