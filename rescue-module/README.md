# Rescue 0.2.0 — experimental

Módulo opcional para KernelSU, armado explicitamente pelo Bloatware Exorcism 0.2.0 antes de um lote de desativação ou remoção por usuário. Não foi validado em aparelho físico. Não é recuperação universal de bootloop.

## Instalar

Instale o ZIP pelo gerenciador do KernelSU e reinicie. Não use recovery. Não exige metamódulo nem monta arquivos de sistema. O modo late-load não é suportado. O módulo deve estar ativado, com os scripts de boot funcionando e BusyBox com flock.

Na tela Deep Exorcism, consulte o módulo e marque "Armar o próximo lote". O app consulta os estados originais e envia uma transação limitada a 30 pacotes. O módulo valida os campos, a whitelist e o hash da fingerprint e grava os estados em seu diretório root antes de confirmar o registro. Se o registro não for confirmado, o app cancela as novas alterações.

Somente um lote pode permanecer armado. O app bloqueia novos lotes enquanto o módulo informa uma transação pendente; as restaurações continuam disponíveis. Não use outras ferramentas para alterar esses pacotes durante o teste.

## Como funciona

O serviço late_start observa o próximo boot após o armamento. Quando `sys.boot_completed=1`, registra o boot observado e zera a contagem de tentativas incompletas. Não mantém o lote automaticamente: depois de testar as funções, confirme pelo app.

Se o boot não completar durante aproximadamente 180 segundos de espera do monitor, tenta restaurar o lote. Se o aparelho reiniciar antes disso, a segunda tentativa consecutiva de boot incompleto permite a restauração assim que o PackageManager responder. Os comandos individuais têm timeout, portanto indisponibilidade do sistema pode alongar esse prazo.

A restauração usa install-existing e o valor original de ativação, inclusive default-state. Cada pacote é conferido por dumpsys. Somente quando todos os estados são confirmados a transação sai de pending. Em caso de falha parcial, indisponibilidade do PackageManager ou mudança de fingerprint, o registro permanece e a operação não é anunciada como concluída.

O monitor compara o identificador da transação para não reverter um lote novo por engano. O acesso ao registro usa flock e permissões root; o conteúdo é dado validado, nunca executado com source ou eval.

## Limites

- Precisa de /data acessível, KernelSU e scripts ativos, SELinux permitindo os comandos e PackageManager em funcionamento suficiente para reinstalar/ativar pacotes.
- Não corrige kernel panic, falha antes de montar /data, corrupção de firmware, outros módulos, alterações externas ou serviços que nunca chegam ao PackageManager.
- Um boot marcado como concluído pode ainda apresentar falhas de interface ou funções. Nesse caso use a reversão manual enquanto houver acesso.
- Não recupera dados privados de apps nem modifica arquivos nas partições de sistema.
- Não reinicia o aparelho à força, altera watchdogs/SELinux, modifica proteções térmicas ou desativa o próprio KernelSU.
- Não atualize/desinstale/desative o módulo com um lote pendente. O instalador recusa atualização quando encontra pending no módulo ativo.
- A remoção do módulo elimina sua capacidade de resgate e pode apagar o registro. Exporte os relatórios antes de fazer manutenção.

Logs e resultado ficam em `state/events.log` e `state/last_result`, dentro do diretório do módulo. O status consultado no aplicativo inclui o último resultado. O histórico do app e o registro do módulo são independentes: após um resgate, consulte os estados atuais; não interprete o histórico anterior como estado atual.

## Testes

Os testes Python substituem getprop/pm/cmd/dumpsys por funções locais em um diretório temporário. Nenhum comando Android real é executado. Cobrem rejeição de campos, fingerprint, falha parcial, boot observado, rollback, isolamento de transação e paridade com as fixtures do parser. Isso não valida os tempos de boot nem permissões no aparelho.

Fonte do formato do módulo: https://kernelsu.org/guide/module.html
