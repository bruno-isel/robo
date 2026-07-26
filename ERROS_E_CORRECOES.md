# Erros Encontrados e Correções — Trabalho Prático 1

**Disciplina:** Fundamentos de Robótica — MEIM/MEET, 2º Sem 2025/2026
**Docente:** Jorge Pais

Registo dos problemas encontrados ao correr o projeto pela primeira vez noutro PC e durante os testes com o robot real, e de como foram resolvidos.

---

## 1. Ambiente novo (PC sem Java/Git instalados)

**Sintoma:** Eclipse aberto com o workspace já importado, mas ao tentar correr `GUI_TP1.java` aparecia `Unable To Launch — the selection cannot be launched`.

**Causa:** o PC não tinha nenhum JDK instalado — só o runtime interno do próprio Eclipse (JustJ), que estava com o caminho inválido (aparecia a vermelho em `Window → Preferences → Java → Installed JREs`).

**Correção:**
1. Instalar o JDK via `winget install EclipseAdoptium.Temurin.21.JDK --source winget` (a source `msstore` falhava por certificado, foi preciso forçar `winget` como source)
2. Registar o JDK em `Window → Preferences → Java → Installed JREs → Add... → Standard VM`, apontando para a pasta instalada em `C:\Program Files\Eclipse Adoptium\jdk-21...`
3. Marcar esse JDK como default

---

## 2. Projeto não compilava (140 erros) — JARs fora do module path

**Sintoma:** depois de corrigir o JRE, o projeto continuava com 140 erros, incluindo em classes básicas do Java (`Font`, `EventQueue`, `EmptyBorder` do `java.desktop`) e nas classes do robot (`InterpretadorEV3`).

**Causa:** os 3 `.jar` de `lib/` (`RobotLegoEV32026.jar`, `InterpretadorEV32026.jar`, `bluecove-2.1.1-SNAPSHOT.jar`) não estavam associados ao *Build Path* do projeto — só lá estava o `JRE System Library`.

**Correção:** `Build Path → Configure Build Path → Libraries → Add External JARs...`, selecionando os 3 `.jar` de `lib/`. Ficaram corretamente no **Modulepath** (não no Classpath), devido ao `module-info.java`.

---

## 3. `module-info.java` vazio

**Sintoma:** mesmo com os JARs no modulepath, os 140 erros mantinham-se.

**Causa:** o `module-info.java` do workspace novo tinha só:
```java
module robo {
}
```
sem nenhum `requires` — nem sequer `java.desktop`. Por isso nada do JDK nem das bibliotecas do robot era visível ao módulo, causando a cascata de erros.

**Correção:** substituído por:
```java
module FR_TP1 {
	requires java.desktop;
	requires RobotLegoEV32026;
	requires InterpretadorEV32026;
}
```
(o nome do módulo tem de ser `FR_TP1` porque é assim que o projeto é corrido depois: `java --module-path lib:out -m FR_TP1/fr_tp1.GUI_TP1`)

---

## 4. Nome da pasta do package não batia certo (`fr-tp1` vs `fr_tp1`)

**Sintoma:** depois da correção do módulo, ainda restavam 4 erros: `The declared package "fr_tp1" does not match...`

**Causa:** a pasta dentro de `src/` chamava-se `fr-tp1` (hífen), mas todos os ficheiros `.java` declaravam `package fr_tp1;` (underscore). Java exige que a estrutura de pastas corresponda exatamente ao package declarado.

**Correção:** `Refactor → Rename...` na pasta, de `fr-tp1` para `fr_tp1`.

---

## 5. Curvas (`curvarEsquerda`/`curvarDireita`) não moviam o robot

**Sintoma:** com o robot real ligado por Bluetooth, `reta()` e `recuar()` funcionavam normalmente, mas ao chamar as curvas o log aparecia na consola (ex: `curveRight(20.0, 50.0) -> 449 graus roda ext`) e o robot não se mexia. Por vezes surgia também uma exceção de **exclusão mútua** vinda do `InterpretadorEV3`.

**Causa:** `acionarCurva()` fazia **duas chamadas separadas** a `OnFwd`, uma por motor:
```java
ev3.OnFwd(motorExt, velExt);
ev3.OnFwd(motorInt, velInt);
```
Isto viola a exclusão mútua interna da biblioteca `InterpretadorEV3` (dois comandos direct-command em sequência rápida sem sincronização). Em contraste, `reta()`/`recuar()` já usavam com sucesso a versão combinada de 4 argumentos, que arranca os dois motores num único comando:
```java
ev3.OnFwd(InterpretadorEV3.OUT_B, velocidade, InterpretadorEV3.OUT_C, velocidade);
```
Confirmado via `javap` no `.jar` que `InterpretadorEV3` tem as três sobrecargas: `OnFwd(int,int)`, `OnFwd(int,int,int)` e `OnFwd(int,int,int,int)`.

**Correção** em `myRobotLego.acionarCurva(...)`:
```java
private void acionarCurva(int motorExt, int velExt, int motorInt, int velInt, int graus) {
    if (velInt >= 0) {
        ev3.OnFwd(motorExt, velExt, motorInt, velInt);
    } else {
        ev3.OnFwd(motorExt, velExt);
        ev3.OnRev(motorInt, -velInt);
    }
    esperarRotacao(motorExt, graus);
    ev3.Off(InterpretadorEV3.OUT_BC);
}
```
Passa a usar o comando combinado sempre que ambas as rodas rodam para a frente (caso normal de curva com raio > metade da distância entre rodas); só recorre a duas chamadas separadas no caso de pivô (roda interior a rodar ao contrário), que não tem equivalente combinado na API.

---

## 6. Botão On/Off ficava "ligado" visualmente antes de confirmar a ligação

**Sintoma (reportado pelo docente):** ao clicar no botão On/Off, o botão aparecia imediatamente premido/ligado, antes de se saber se a ligação Bluetooth ao robot tinha sido bem-sucedida.

**Causa:** `rdbtnOnoff` é um `JRadioButton` — o próprio clique já altera visualmente o estado selecionado do botão de forma síncrona, antes do `ActionListener` (`toggleConexao()`) correr. O `SwingWorker` só confirmava/corrigia o estado no `done()`, ou seja, o utilizador via sempre o botão "ligado" durante a tentativa de ligação, mesmo que esta viesse a falhar.

**Correção** em `GUI_TP1.toggleConexao()`: reverter imediatamente o toggle visual do clique e só marcar como ligado depois de `robot.ligar()` confirmar (e desativar o botão durante a tentativa, para evitar cliques duplos):
```java
if (rdbtnOnoff.isSelected()) {
    rdbtnOnoff.setSelected(false);   // reverte o clique visual imediato
    rdbtnOnoff.setEnabled(false);
    ...
    new SwingWorker<Boolean, Void>() {
        ...
        @Override protected void done() {
            try {
                boolean ok = get();
                rdbtnOnoff.setSelected(ok);   // só aqui reflete o estado real
                ...
            } finally {
                rdbtnOnoff.setEnabled(true);
            }
        }
    }.execute();
}
```

---

## 7. Botão "Parar" não interrompia um movimento em curso

**Sintoma:** ao carregar em "Parar" enquanto o robot estava a mover-se (ex: durante um `Frente` longo), nada acontecia — o botão parecia não fazer nada.

**Causa:** `GUI_TP1.setBotoesMovimento(boolean ativo)` desativava **todos** os botões de movimento, incluindo o próprio `Parar`, assim que qualquer comando começava a correr (`executarComando()` chama `setBotoesMovimento(false)` no início). Ou seja, precisamente enquanto o robot se movia — quando faria sentido carregar em Parar — o botão estava desligado. Só voltava a ficar ativo quando o movimento terminava sozinho.

Corrigir isto de forma ingénua (só reativar o botão) introduziria um novo problema: clicar em Parar durante um movimento lançaria uma *segunda* thread a chamar `ev3.Off(...)` ao mesmo tempo que a thread do movimento em curso está a fazer polling a `ev3.RotationCount(...)` — a mesma violação de exclusão mútua do ponto 5.

**Correção:**
- Em `GUI_TP1`: o botão `Parar` deixou de ser gerido por `setBotoesMovimento()`; passa a ficar sempre ativo enquanto o robot está ligado (ativado/desativado só em `toggleConexao()`), e o seu `ActionListener` chama `robot.parar(true)` diretamente na EDT, sem passar por `executarComando()`.
- Em `myRobotLego`: adicionadas flags `volatile` (`pedidoParar`, `travarAoParar`, `emMovimento`). `esperarRotacao()` verifica `pedidoParar` a cada iteração do polling e sai do ciclo assim que for pedida uma paragem. `parar(travar)` **nunca** chama `ev3` diretamente se houver um movimento em curso — só define as flags; é sempre a thread do movimento (a única a "falar" com o `ev3` nesse momento) que efetivamente chama `Off`/`Float`, evitando qualquer acesso concorrente ao `ev3`.

---

## Estado atual

Todos os pontos acima estão corrigidos e commitados. Falta ainda testar no robot real:
- [ ] Confirmar que as curvas movem o robot corretamente com o fix do `OnFwd` combinado
- [ ] Confirmar visualmente que o botão On/Off só acende depois da confirmação de ligação
- [ ] Confirmar que "Parar" interrompe mesmo um `Frente`/`Retaguarda`/curva em curso
