# Erros Encontrados e Correções — Trabalho Prático 2

**Disciplina:** Fundamentos de Robótica — MEIM/MEET, 2º Sem 2025/2026
**Docente:** Jorge Pais

Registo dos problemas encontrados no `myRobotLego`/`GUI_TP2` do TP2, portados a partir das correções já feitas no TP1 (ver [`ERROS_E_CORRECOES.md`](ERROS_E_CORRECOES.md)). O TP2 partilha a mesma biblioteca `InterpretadorEV3`, mas o `myRobotLego` do TP2 usa uma abordagem diferente de controlo de movimento (tempo/`Thread.sleep`, em vez de contagem de rotações do TP1), pelo que algumas correções tiveram de ser adaptadas.

---

## 1. Curvas (`curvarEsquerda`/`curvarDireita`) com o mesmo bug de exclusão mútua do TP1

**Sintoma esperado:** tal como no TP1 antes da correção, as curvas arriscavam não mover o robot ou lançar uma exceção de exclusão mútua do `InterpretadorEV3`.

**Causa:** `acionarCurva()` fazia duas chamadas `OnFwd` separadas, uma por motor — exatamente o mesmo padrão problemático corrigido no TP1 (ver ponto 5 do documento do TP1).

**Correção:** passa a usar a chamada combinada de 4 argumentos sempre que ambas as rodas rodam para a frente:
```java
private void acionarCurva(int motorExt, int velExt, int motorInt, int velInt, long ms) {
    if (velInt >= 0) {
        ev3.OnFwd(motorExt, velExt, motorInt, velInt);
    } else {
        ev3.OnFwd(motorExt, velExt);
        ev3.OnRev(motorInt, -velInt);
    }
    dormir(ms);
    terminarMovimento();
}
```

---

## 2. Botão "Parar" não interrompia um movimento em curso

**Sintoma:** tal como no TP1, `setBotoesMovimento()` desativava o botão `Parar` ao mesmo tempo que os outros botões de movimento, ficando indisponível precisamente enquanto o robot se movia.

**Causa:** mesma raiz do TP1 (ver ponto 7 do documento do TP1) — `Parar` gerido pela mesma função que desativa `Frente`/`Retaguarda`/`Esquerda`/`Direita` durante `executarComando()`.

**Correção em `GUI_TP2`:** `Parar` deixou de estar em `setBotoesMovimento()`; fica sempre ativo enquanto ligado, e o seu `ActionListener` chama `robot.parar(true)` diretamente, sem passar por `executarComando()`.

**Correção em `myRobotLego` (TP2) — adaptada, porque aqui não há polling:**

O TP1 controla o movimento fazendo *polling* a `RotationCount` num ciclo (`esperarRotacao`), por isso bastava verificar uma flag a cada iteração do ciclo. O TP2 controla o movimento com um `Thread.sleep(ms)` bloqueante — não há ciclo onde verificar uma flag. A solução usada foi interromper a própria thread que está a dormir:

```java
private volatile Thread threadMovimento = null;
private volatile boolean travarAoParar = true;

@Override
public void parar(boolean travar) {
    if (!verificar()) return;
    Thread t = threadMovimento;
    if (t != null) {
        // Movimento em curso: so pede a interrupcao da thread que la esta;
        // e essa thread quem fala com o ev3 (nunca duas threads ao mesmo tempo).
        travarAoParar = travar;
        t.interrupt();
    } else {
        if (travar) ev3.Off(InterpretadorEV3.OUT_BC);
        else ev3.Float(InterpretadorEV3.OUT_BC);
    }
}

private static void dormir(long ms) {
    try { Thread.sleep(ms); }
    catch (InterruptedException e) { /* pedido de paragem: sai mais cedo */ }
}
```

**Nota importante:** o `catch` de `dormir()` **não** repõe o estado de interrupção da thread (`Thread.currentThread().interrupt()`), ao contrário do padrão Java habitual. Isto é deliberado: as threads que executam os comandos vêm de uma *pool* reutilizada pelo `SwingWorker` — se repuséssemos o estado de interrompido, a *próxima* tarefa (não relacionada) a cair nessa mesma thread da pool herdaria uma interrupção "fantasma" e abortaria sem motivo.

**Limitação conhecida:** se o "Parar" for pressionado durante a execução de uma **trajetória completa** (`executarTrajetoria()`, vários passos calculados pelo `CalculadorTrajetoria`), a interrupção só para o passo atual — o ciclo `for` em `executarTrajetoria()` continua automaticamente para o próximo passo. Isto ainda não foi corrigido (ver "Estado atual" abaixo); no TP1 este caso não existe porque não há execução de sequências multi-passo.

**Atualização (ver ponto 7):** este mecanismo de `Thread.interrupt()` foi entretanto substituído — deixou de ser necessário depois de se mudar o `myRobotLego` de `Thread.sleep` para `RotationCount` (ponto 7), passando a usar o mesmo mecanismo simples de flag do TP1.

---

## 3. `myRobotLego` sem ligação à consola da GUI

**Sintoma:** ao trocar `SimuladorRobot` por `myRobotLego` no TP2, os logs do robot real (`[EV3] ...`) só apareciam no `System.out`, nunca na consola da GUI — ao contrário do TP1, onde `myRobotLego` já recebia um `Consumer<String>` para escrever na consola em tempo real.

**Causa:** o construtor `myRobotLego()` do TP2 não aceitava nenhum callback de log.

**Correção:** adicionado um construtor `myRobotLego(Consumer<String> guiLog)` (mantendo o construtor sem argumentos por compatibilidade), com um `log()` interno que escreve em `System.out` e no `guiLog`, tal como no TP1. O comentário em `GUI_TP2` que indica como trocar o simulador pelo robot real foi atualizado para `new myRobotLego(this::myPrintSempre)`.

---

## 4. Botão On/Off ficava "ligado" visualmente antes de confirmar a ligação

Mesmo problema e mesma correção do TP1 (ver ponto 6 do documento do TP1) — `toggleConexao()` reverte o toggle visual do `JRadioButton` imediatamente e só marca como ligado depois de `robot.ligar()` confirmar.

---

## 5. `reta()`/`recuar()` falhavam com "Identificação do motor ilegal!"

**Sintoma:** testado no robot real com a trajetória do slide 04 (`Xf=70, Yf=40, φf=70` → `curveLeft, straight, curveLeft`), as duas curvas executaram-se sempre corretamente, mas o passo `straight` (e também `Retaguarda`) falhava sempre com o erro da própria biblioteca:
```
OnFWD(motor, vel)- Erro: Identificação do motor ilegal!
OnRev(motor, vel)- Erro: Identificação do motor ilegal!
```
Os botões "Frente"/"Retaguarda" pareciam não fazer nada.

**Causa:** `reta()` e `recuar()` chamavam `ev3.OnFwd(OUT_BC, vel)` / `ev3.OnRev(OUT_BC, vel)` — a versão de **2 argumentos**, com o bitmask combinado `OUT_BC` a identificar as duas rodas de uma vez. Esta forma nunca tinha sido confirmada como funcional; só a versão de **4 argumentos** com portas individuais (`OnFwd(OUT_B, vel, OUT_C, vel)`) estava validada — é a que o TP1 usa desde o início, e a que já corrigimos nas curvas do TP2 (ponto 1). O `InterpretadorEV3` rejeita o bitmask combinado nesta chamada como "motor ilegal".

**Correção:**
```java
ev3.OnFwd(InterpretadorEV3.OUT_B, velocidade, InterpretadorEV3.OUT_C, velocidade);
...
ev3.OnRev(InterpretadorEV3.OUT_B, velocidade, InterpretadorEV3.OUT_C, velocidade);
```
(`ev3.Off(OUT_BC)` continua a usar o bitmask combinado sem problema — só `OnFwd`/`OnRev` de 2 argumentos é que falha.)

---

## 6. Trajetórias 2 e 3 sem segmento reto e sem raio prático

**Sintoma:** testado com o exemplo do slide 05 (Trajetória 2: `Xf=70, Yf=40, φf=20`), a GUI calculou `curvaEsquerda(56.94, 51.79°)` + `curvaDireita(56.94, 31.79°)` — só 2 passos, sem `reta` no meio — quando o slide mostra 3 passos: `curveLeft(47.5, 39.32)`, `straight(33.68)`, `curveRight(47.5, 19.32)`. O raio também não batia certo (56.94 vs 47.5).

**Causa:** `calcularT2T3()` tinha dois problemas:
1. Nunca chamava `calcRaioPratico()` — usava sempre o raio **teórico** (o valor 56,94 é na verdade o raio teórico, que até bate certo com o slide "Theoretical Trajectory 2": `r=56,95`).
2. A fórmula do ângulo assumia os dois arcos tangentes entre si (sem espaço para uma reta), quando na prática — com o raio menor (prático) — os arcos deixam de se tocar e é preciso um segmento reto a ligar os dois, exatamente como a Trajetória 1 já fazia.

**Correção:** `calcularT2T3()` passa a: converter o raio teórico para prático (`calcRaioPratico`, a mesma função já usada por `tentarT1`), recalcular os centros dos dois arcos com esse raio prático, calcular a distância `d12` entre os centros, e obter o ângulo extra (`δ = arccos(rP/(d12/2))`) e o comprimento da reta (`dStraight = d12·sen(δ)`) que a ligam — replicando o método usado nos slides "Trajetória 2/3 Prática". Passos passam a ser `curvaEsquerda + reta + curvaDireita`.

**Validação:** testado contra os 3 exemplos dos slides (04, 05, 06):
- Trajetória 1 (regressão): inalterada, continua correta.
- Trajetória 2: `curveLeft(47.50, 39.35°)`, `reta(33.64)`, `curveRight(47.50, 19.35°)` — praticamente idêntico ao slide (`47.5`, `39.32°`, `33.68`, `19.32°`).
- Trajetória 3: estrutura correta (3 passos, raio prático calculado), mas o raio final (19.00) difere do slide (17.82) porque esse exemplo específico do slide usa `vrobot=30`, enquanto o código usa `V_ROBOT=40` fixo (o mesmo valor que já funciona bem na Trajetória 1/2) — diferença de parâmetro, não bug.

---

## 7. `myRobotLego` movido de `Thread.sleep` (tempo) para `RotationCount` (rotação real da roda)

**Contexto:** o `myRobotLego` original do TP2 controlava o movimento por **tempo estimado**: calculava quantos `ms` o motor precisaria de rodar a uma dada velocidade para percorrer a distância/arco pretendido, e simplesmente esperava esse tempo (`Thread.sleep(ms)`) antes de desligar o motor. Isto assume velocidade constante e execução perfeita — não há nenhuma confirmação de que a roda rodou mesmo o que devia (arranques mais lentos, atrito, variação de bateria, etc. não são compensados). Note-se que isto **não vinha do `SimuladorRobot`** — o simulador nem sequer usa `Thread.sleep` (é cálculo instantâneo); era só a abordagem original escolhida para o robot real.

O TP1 usa uma abordagem mais precisa desde o início: em vez de tempo, usa os **encoders dos motores** (`ev3.RotationCount(porta)`) — calcula quantos **graus a roda tem de rodar** e fica em polling a perguntar ao robot até confirmar que rodou mesmo esse número de graus.

**Mudança:** o `myRobotLego` do TP2 foi atualizado para usar o mesmo mecanismo do TP1 (`RotationCount` + polling em `esperarRotacao`), tornando os dois projetos consistentes e o movimento mais fiel ao que o robot realmente percorre.

**Efeito secundário — simplifica o Parar (ponto 2):** com o polling de volta, o mecanismo de interromper um movimento em curso deixou de precisar de `Thread.interrupt()` numa thread guardada (`threadMovimento`) — voltou a ser uma simples flag (`pedidoParar`) verificada a cada iteração do ciclo de polling, exatamente como no TP1. Mais simples e sem necessidade de gerir referências a threads.

---

## 8. `[EV3] Falha na ligação a: EVA` — robot ligado, emparelhado e Bluetooth ativo

**Sintoma:** ao clicar On/Off na GUI com o nome "EVA" (e também testado com "eva" e outro nome), a ligação falha sempre — `myRobotLego.ligar()` reporta `[EV3] Falha na ligação a: EVA`. Confirmado que o robot está ligado, emparelhado no Windows e com o Bluetooth ativo no ecrã do EV3. O nome do robot não foi alterado depois de emparelhado.

**Investigação:** descompilado `InterpretadorEV32026.jar` e `bluecove-2.1.1-SNAPSHOT.jar` (`javap -c`) para perceber o que `ligar()` → `ev3.OpenEV3(nome)` faz internamente:
1. `ProcuraDeviceRemoto(nome)` pede ao `DiscoveryAgent` a lista de dispositivos **já emparelhados** (nesta versão do bluecove, `PREKNOWN = 1`, ao contrário da spec JSR-82 standard onde seria `CACHED`) — não faz inquiry/scan ao vivo.
2. Compara o nome de cada dispositivo dessa lista com o texto do campo "Robot" usando `String.equals()` — comparação exata, sensível a maiúsculas/minúsculas e a espaços invisíveis.
3. Só se encontrar uma correspondência exata é que tenta `Open()` (liga o canal RFCOMM/SPP `btspp://<endereço>:1`).

A biblioteca imprime também, diretamente para `System.out` (não para a consola da GUI, mas visível na aba **Console do Eclipse**), mensagens adicionais que isolam onde a falha ocorre:
- `Dispositivo bluetooth <nome> não encontrado.` → falha no passo 1/2 (o Windows/bluecove não devolveu o dispositivo com esse nome exato na lista de emparelhados).
- `Dispositivo bluetooth <nome> encontrado.` seguido de `Insucesso no estabelecimento do canal de comunicação` ou `Erro na abertura das streams` → o dispositivo foi encontrado, mas o canal RFCOMM em si não abriu.

**Checklist de troubleshooting (por ordem de probabilidade):**
- [ ] Verificar a aba **Console do Eclipse** (não a caixa de texto da GUI) para ver qual das duas mensagens acima aparece — isto distingue "não encontrado" de "encontrado mas falha o canal".
- [ ] Confirmar o nome exato em **Definições → Bluetooth e dispositivos** do Windows (copiar/colar para a GUI, para evitar espaços ou diferenças de maiúsculas invisíveis).
- [ ] Se o nome bater certo mas continuar "não encontrado": esquecer o dispositivo no Windows e voltar a emparelhar do zero (o Windows guarda o nome/estado do momento do emparelhamento; um emparelhamento antigo ou incompleto pode não aparecer corretamente na lista PREKNOWN que o bluecove lê).
- [ ] Se aparecer "encontrado" mas o canal falhar: confirmar que não há **outra aplicação** (ou outra instância da GUI) já ligada ao robot — o EV3 só aceita uma ligação RFCOMM de cada vez — e que o robot não está em standby/suspenso.
- [ ] Correr o Eclipse como Administrador — o `bluecove` (2011, pensado para Windows XP/7) por vezes precisa de privilégios elevados para aceder à API de Bluetooth em Windows 10/11.
- [ ] Confirmar que o emparelhamento foi feito com PIN clássico (Bluetooth Classic/SPP), não via "Adicionar dispositivo" rápido (Swift Pair) que por vezes não regista o serviço SPP necessário para o `InterpretadorEV3`.

**Estado:** não resolvido — falta o utilizador verificar a aba Console do Eclipse para confirmar qual das duas mensagens aparece e seguir o checklist a partir daí.

---

## Estado atual

Correções portadas do TP1 e commitadas, mais os fixes dos pontos 5 e 6 (confirmados: curvas, reta/recuar e Trajetória 1 testados no robot real; Trajetórias 2/3 validadas por cálculo contra os slides). Falta ainda:
- [ ] Testar Trajetória 2/3 no robot real (só a Trajetória 1 foi executada fisicamente até agora)
- [ ] Testar Parar a meio de um movimento simples (`Frente`/`Retaguarda` isolados, fora de uma trajetória)
- [ ] Corrigir a limitação do ponto 2: Parar durante uma trajetória multi-passo (`executarTrajetoria`) só interrompe o passo atual, não a sequência toda
- [ ] Parte II do guião do TP2 (robot seguidor de parede com sonar + sensor de toque) — ainda não iniciada
