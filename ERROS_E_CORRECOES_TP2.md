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

## Estado atual

Correções portadas do TP1 e commitadas, mais o fix do ponto 5 (confirmado em teste real: curvas OK, reta/recuar corrigido). Falta ainda:
- [ ] Voltar a testar `Frente`/`Retaguarda` no robot real depois do fix do ponto 5
- [ ] Testar Parar a meio de um movimento simples
- [ ] Corrigir a limitação do ponto 2: Parar durante uma trajetória multi-passo (`executarTrajetoria`) só interrompe o passo atual, não a sequência toda
- [ ] Parte II do guião do TP2 (robot seguidor de parede com sonar + sensor de toque) — ainda não iniciada
