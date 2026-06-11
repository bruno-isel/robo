# Documentação Técnica do Código — Trabalhos Práticos 1 e 2

**Disciplina:** Fundamentos de Robótica — MEIM/MEET, 2º Sem 2025/2026  
**Autor:** Bruno Rodrigues (52323)  
**Docente:** Jorge Pais

---

## Índice

1. [Visão Geral da Arquitetura](#1-visão-geral-da-arquitetura)
2. [Módulos Java](#2-módulos-java)
3. [Projeto 1](#3-projeto-1)
   - 3.1 [IRobot.java — Interface Comum](#31-irobotjava--interface-comum)
   - 3.2 [DadosGUI_TP1.java — Modelo de Dados](#32-dadosgui_tp1java--modelo-de-dados)
   - 3.3 [SimuladorRobot.java — Simulador de Terminal](#33-simuladorrobotjava--simulador-de-terminal)
   - 3.4 [myRobotLego.java — Robot Real](#34-myrobotlegojava--robot-real)
   - 3.5 [GUI_TP1.java — Interface Gráfica](#35-gui_tp1java--interface-gráfica)
4. [Projeto 2](#4-projeto-2)
   - 4.1 [CalculadorTrajetoria.java — Calculador de Trajetórias](#41-calculadortrajetoriajava--calculador-de-trajetórias)
   - 4.2 [GUI_TP2.java — Interface Gráfica Estendida](#42-gui_tp2java--interface-gráfica-estendida)
5. [Modelo de Threads — EDT e SwingWorker](#5-modelo-de-threads--edt-e-swingworker)
6. [Padrão Consumer\<String\> — Log Desacoplado](#6-padrão-consumerstring--log-desacoplado)
7. [Compilar e Executar](#7-compilar-e-executar)

---

## 1. Visão Geral da Arquitetura

Ambos os projetos partilham a mesma arquitetura em camadas:

```
┌─────────────────────────────────────────────────┐
│              GUI  (Swing JFrame)                 │
│   GUI_TP1 / GUI_TP2                              │
│   - cria componentes Swing                       │
│   - lê campos de texto                           │
│   - delega comandos ao robot via IRobot          │
│   - escreve log na textArea (thread-safe)        │
└────────────────────┬────────────────────────────┘
                     │  usa interface
              ┌──────▼──────┐
              │   IRobot    │  interface comum
              └──────┬──────┘
         ┌───────────┴───────────┐
         │                       │
┌────────▼────────┐   ┌──────────▼──────────┐
│ SimuladorRobot  │   │   myRobotLego        │
│ (simulação)     │   │   (robot físico EV3) │
└─────────────────┘   └─────────────────────┘
```

A GUI não sabe se está a falar com o simulador ou com o robot real — isso é o padrão **Strategy / Polimorfismo**. Para trocar, basta mudar uma linha no construtor da GUI.

No Projeto 2 acrescenta-se uma camada de cálculo matemático:

```
GUI_TP2  →  CalculadorTrajetoria.calcular(xf, yf, φf)
                     ↓
             Resultado (lista de Passos)
                     ↓
             GUI executa cada Passo via IRobot
```

---

## 2. Módulos Java

### `projeto1/src/module-info.java`

```java
module FR_TP1 {
    requires java.desktop;
    requires RobotLegoEV32026;
    requires InterpretadorEV32026;
}
```

### `projeto2/src/module-info.java`

```java
module FR_TP2 {
    requires java.desktop;
    requires RobotLegoEV32026;
    requires InterpretadorEV32026;
}
```

O sistema de módulos do Java (introduzido no Java 9) obriga a declarar as dependências explicitamente.

| Declaração | Significado |
|---|---|
| `requires java.desktop` | Permite usar Swing (`javax.swing.*`) e AWT (`java.awt.*`). O módulo `java.desktop` agrupa toda a GUI desktop do JDK. |
| `requires RobotLegoEV32026` | Biblioteca high-level do robot fornecida pelo professor. Não usada diretamente no código (incompatibilidade BlueCove/macOS), mas necessária para o compilador não rejeitar `InterpretadorEV32026`. |
| `requires InterpretadorEV32026` | API low-level do EV3. Expõe a classe `interpretador.InterpretadorEV3` usada em `myRobotLego.java`. |

As JARs ficam em `lib/` e são passadas ao compilador/runtime via `--module-path lib`.

---

## 3. Projeto 1

### 3.1 `IRobot.java` — Interface Comum

```java
package fr_tp1;

public interface IRobot {
    boolean ligar(String nome);
    void desligar();
    void reta(double distancia);
    void recuar(double distancia);
    void curvarEsquerda(double raio, double angulo);
    void curvarDireita(double raio, double angulo);
    void parar(boolean travar);
    void setVelocidade(int vel);
    boolean isLigado();
}
```

Esta interface define o **contrato** de qualquer robot neste projeto. A GUI só conhece `IRobot` — nunca instancia `SimuladorRobot` ou `myRobotLego` diretamente (exceto na linha de criação do campo `robot`).

| Método | Descrição |
|---|---|
| `ligar(String nome)` | Estabelece ligação ao robot com o nome Bluetooth dado. Retorna `true` se sucesso. |
| `desligar()` | Fecha a ligação e para os motores. |
| `reta(double distancia)` | Move para a frente `distancia` cm. |
| `recuar(double distancia)` | Move para trás `distancia` cm. |
| `curvarEsquerda(double raio, double angulo)` | Arco anti-horário: centro à esquerda do robot, raio `raio` cm, varrendo `angulo` graus. |
| `curvarDireita(double raio, double angulo)` | Arco horário: centro à direita do robot. |
| `parar(boolean travar)` | Para os motores. `travar=true` → travagem ativa (freia); `false` → deixa rolar livremente. |
| `setVelocidade(int vel)` | Define velocidade em % (20–80%). |
| `isLigado()` | Retorna se o robot está ligado. Usado pela GUI para bloquear botões quando desligado. |

---

### 3.2 `DadosGUI_TP1.java` — Modelo de Dados

```java
package fr_tp1;

import java.util.ArrayList;

public class DadosGUI_TP1 {

    private boolean onOff, debug;
    private String robotName;
    private double raio, angulo, distancia;
    private ArrayList<String> consola;

    public DadosGUI_TP1() {
        onOff     = false;
        debug     = true;
        robotName = "Bruno";
        raio      = 20.0;
        angulo    = 50.0;
        distancia = 50.0;
        consola   = new ArrayList<>();
    }

    // getters e setters para cada campo...
}
```

É um **POJO** (Plain Old Java Object) — um contentor de dados sem lógica. Serve dois propósitos:

1. **Valores iniciais** dos campos da GUI: quando a janela abre, os `JTextField` são preenchidos com os valores deste objeto.
2. **Valor de fallback** no `parseCampoDouble`: se o utilizador apagar um campo de texto e clicar num botão, usa-se o valor guardado em `dados` em vez de rebentar com exceção.

| Campo | Valor inicial | Uso |
|---|---|---|
| `onOff` | `false` | Estado do `JRadioButton` On/Off |
| `debug` | `true` | Estado da `JCheckBox` Debug (ligado por defeito) |
| `robotName` | `"Bruno"` | Nome Bluetooth pré-preenchido |
| `raio` | `20.0 cm` | Raio de curvatura padrão |
| `angulo` | `50.0°` | Ângulo de curvatura padrão |
| `distancia` | `50.0 cm` | Distância em linha reta padrão |
| `consola` | `ArrayList` vazio | Buffer de mensagens (não usado ativamente; o log vai direto para a textArea) |

---

### 3.3 `SimuladorRobot.java` — Simulador de Terminal

É a implementação de `IRobot` que funciona sem robot físico. Em vez de enviar comandos Bluetooth, calcula matematicamente onde o robot estaria e reporta o resultado na consola da GUI.

#### Construtor e logger

```java
private final Consumer<String> logger;

public SimuladorRobot(Consumer<String> logger) {
    this.logger = logger;
}

private void log(String msg) {
    logger.accept("[SIMULADOR] " + msg);
}
```

O `Consumer<String>` é uma **função de callback** injetada no construtor. A GUI passa `this::myPrintSempre`, ou seja, cada vez que o simulador chama `log(msg)`, a mensagem aparece na `textArea` da GUI. Isto desacopla o simulador da GUI — o simulador não precisa de saber que existe um `JTextArea`, apenas chama o consumer. Ver secção 6 para mais detalhes.

#### Estado cinemático

```java
private double xi = 0.0;   // posição vertical (para cima)
private double yi = 0.0;   // posição horizontal (para a esquerda)
private double phi = 0.0;  // orientação em graus
```

O sistema de coordenadas segue os slides do professor:

```
         Xi (cima)
          ↑
          │  ← robot começa aqui, a apontar para Xi
          │
          └──────→ Yi (esquerda)

  φ = 0°  → a apontar para Xi (cima)
  φ = 90° → a apontar para Yi (esquerda)
  φ > 0   → rodou para a esquerda (anti-horário)
  φ < 0   → rodou para a direita (horário)
```

#### `ligar()` — reset de posição

```java
@Override
public boolean ligar(String nome) {
    this.nome = nome;
    this.ligado = true;
    xi = 0; yi = 0; phi = 0;
    log("Ligado ao robot: " + nome);
    log("Posição inicial: Xi=0.00  Yi=0.00  φ=0.00°  (a apontar para Xi)");
    return true;
}
```

Sempre retorna `true` (simulador nunca falha a ligar). Reset das coordenadas para a origem.

#### `reta()` — deslocamento em linha reta

```java
@Override
public void reta(double distancia) {
    if (!verificar()) return;
    double rad = Math.toRadians(phi);
    xi += distancia * Math.cos(rad);
    yi += distancia * Math.sin(rad);
    log("straight(" + fmt(distancia) + ")  →  " + pos());
}
```

**Matemática:** o robot move-se na direção do seu rumo atual `φ`. Em coordenadas cartesianas (Xi, Yi):

```
ΔXi = d × cos(φ)
ΔYi = d × sin(φ)
```

Quando `φ=0°`: `cos(0)=1`, `sin(0)=0` → move-se apenas em Xi (para cima). Quando `φ=90°`: `cos(90)=0`, `sin(90)=1` → move-se apenas em Yi (para a esquerda). Para ângulos intermédios, decompõe-se nas duas componentes.

#### `recuar()` — marcha-atrás

```java
xi -= distancia * Math.cos(rad);
yi -= distancia * Math.sin(rad);
```

Exatamente o oposto de `reta()` — subtrai em vez de somar, movendo o robot na direção oposta ao seu rumo.

#### `curvarEsquerda()` — arco anti-horário

```java
@Override
public void curvarEsquerda(double raio, double angulo) {
    if (!verificar()) return;
    double θ = Math.toRadians(phi);
    double α = Math.toRadians(angulo);
    xi += raio * (Math.sin(θ + α) - Math.sin(θ));
    yi += raio * (Math.cos(θ) - Math.cos(θ + α));
    phi = normalizarPhi(phi + angulo);
    log("curveLeft(" + fmt(raio) + ", " + fmt(angulo) + ")  →  " + pos());
}
```

**Derivação matemática:**

O robot percorre um arco de circunferência com o centro à sua esquerda. O deslocamento entre o ponto inicial e o ponto final do arco (varrendo ângulo α) é:

```
ΔXi = r × sin(φ + α) − r × sin(φ)
ΔYi = r × cos(φ)    − r × cos(φ + α)
```

E a nova orientação é `φ_new = φ + α` (virou α graus para a esquerda).

#### `curvarDireita()` — arco horário

```java
xi += raio * (Math.sin(θ) - Math.sin(θ - α));
yi += raio * (Math.cos(θ - α) - Math.cos(θ));
phi = normalizarPhi(phi - angulo);
```

Centro à direita do robot. O deslocamento é:

```
ΔXi = r × sin(φ)     − r × sin(φ − α)
ΔYi = r × cos(φ − α) − r × cos(φ)
```

E `φ_new = φ − α` (virou α graus para a direita, o ângulo diminui).

#### `normalizarPhi()` — manter φ em (−180°, 180°]

```java
private static double normalizarPhi(double p) {
    p = ((p % 360) + 360) % 360; // [0°, 360°)
    return p > 180 ? p - 360 : p; // (−180°, 180°]
}
```

Após cada curva, `φ` pode sair do intervalo. Exemplo: `φ=170° + 90° = 260°` → normaliza para `260°−360° = −100°` (virou tanto para a esquerda que agora está a apontar para baixo-direita).

O truque `((p % 360) + 360) % 360` garante que mesmo valores negativos ficam no intervalo `[0°, 360°)` antes de aplicar o ajuste final.

---

### 3.4 `myRobotLego.java` — Robot Real

Implementa `IRobot` usando a classe `InterpretadorEV3` da biblioteca fornecida pelo professor. Envia comandos reais aos motores do robot físico via Bluetooth.

#### Constantes físicas

```java
private static final double WHEEL_DIAM   = 5.6;                         // cm
private static final double DBW          = 9.5;                         // cm
private static final double WHEEL_CIRC   = Math.PI * WHEEL_DIAM;        // ≈ 17.59 cm
private static final double MAX_CM_PER_S = 720.0 * WHEEL_CIRC / 360.0; // ≈ 35.2 cm/s a 100%
```

| Parâmetro | Valor | Explicação |
|---|---|---|
| `WHEEL_DIAM` | 5.6 cm | Diâmetro das rodas do EV3 padrão |
| `DBW` | 9.5 cm | Distância entre eixos das duas rodas motrizes |
| `WHEEL_CIRC` | π × 5.6 ≈ 17.59 cm | Perímetro da roda — distância percorrida por rotação completa |
| `MAX_CM_PER_S` | 720°/s × 17.59/360 ≈ 35.2 cm/s | Velocidade linear máxima da roda a 100% |

O motor EV3 a velocidade máxima roda a 720°/s (2 rotações/segundo). A velocidade linear é `vel_angular_rad/s × raio_roda`, que simplifica para `720 × WHEEL_CIRC / 360`.

#### `ligar()` e `desligar()`

```java
@Override
public boolean ligar(String nome) {
    ligado = ev3.OpenEV3(nome);  // tenta ligação Bluetooth pelo nome
    if (ligado) {
        ev3.ResetAll();          // reset dos encoders dos motores
    }
    return ligado;
}

@Override
public void desligar() {
    if (ligado) {
        ev3.Off(InterpretadorEV3.OUT_BC); // para ambos os motores
        ev3.CloseEV3();                   // fecha conexão Bluetooth
        ligado = false;
    }
}
```

`OUT_BC` é uma constante do `InterpretadorEV3` que referencia ambos os motores B e C simultaneamente. Motor B = esquerdo, Motor C = direito (configuração padrão do EV3).

#### `tempoMs()` — duração do movimento

```java
private long tempoMs(double distancia, int vel) {
    double cmPerSec = (vel / 100.0) * MAX_CM_PER_S;
    return (long) (distancia / cmPerSec * 1000);
}
```

Como o `InterpretadorEV3` controla os motores por tempo (não por distância), é preciso calcular quantos milissegundos são necessários para percorrer `distancia` cm à velocidade `vel`%:

```
velocidade_linear = (vel/100) × MAX_CM_PER_S   [cm/s]
tempo = distancia / velocidade_linear           [s]
tempo_ms = tempo × 1000                         [ms]
```

#### `reta()` — linha reta

```java
@Override
public void reta(double distancia) {
    if (!verificar()) return;
    long ms = tempoMs(distancia, velocidade);
    ev3.OnFwd(InterpretadorEV3.OUT_BC, velocidade);  // ambas as rodas para a frente
    dormir(ms);
    ev3.Off(InterpretadorEV3.OUT_BC);                // parar
}
```

Padrão: **liga → espera → desliga**. `OnFwd` com ambos os motores à mesma velocidade produz uma linha reta.

#### `curvarEsquerda()` — cinemática diferencial

```java
@Override
public void curvarEsquerda(double raio, double angulo) {
    double raioExt = raio + DBW / 2;  // roda exterior (C = direita) percorre mais
    double raioInt = raio - DBW / 2;  // roda interior (B = esquerda) percorre menos
    int velExt = velocidade;
    int velInt = (int) Math.round(velocidade * raioInt / raioExt);
    long ms = tempoMs(raioExt * Math.toRadians(angulo), velExt);
    acionarCurva(InterpretadorEV3.OUT_C, velExt, InterpretadorEV3.OUT_B, velInt, ms);
}
```

**Cinemática diferencial:** numa curva para a esquerda, a roda direita (exterior) percorre um arco maior que a roda esquerda (interior). Para manter as rodas sincronizadas no mesmo tempo:

```
arco_exterior = (raio + DBW/2) × α    [cm]
arco_interior = (raio − DBW/2) × α    [cm]

vel_interior / vel_exterior = arco_interior / arco_exterior
                            = (raio − DBW/2) / (raio + DBW/2)
```

Se `raio < DBW/2` (curva muito fechada), `raioInt < 0` → `velInt < 0` → a roda interior vai para trás. O método `acionarCurva()` trata este caso.

O **tempo** é calculado pela distância da roda exterior (a mais rápida): `ms = tempoMs(raioExt × α_rad, velExt)`.

Para `curvarDireita()`, os motores trocam de papel: `OUT_B` (esquerdo) é a roda exterior.

#### `acionarCurva()` — activação dos motores

```java
private void acionarCurva(int motorExt, int velExt, int motorInt, int velInt, long ms) {
    ev3.OnFwd(motorExt, velExt);
    if (velInt > 0)
        ev3.OnFwd(motorInt, velInt);   // roda interior para a frente, mais devagar
    else if (velInt < 0)
        ev3.OnRev(motorInt, -velInt);  // roda interior para trás (curva muito fechada)
    else
        ev3.Off(motorInt);             // roda interior parada (rotação em pivô)
    dormir(ms);
    ev3.Off(InterpretadorEV3.OUT_BC);
}
```

Trata os três casos da velocidade interior: positiva, zero (rotação em pivô), ou negativa (roda vai ao contrário).

#### `dormir()` — sleep seguro

```java
private static void dormir(long ms) {
    try { Thread.sleep(ms); }
    catch (InterruptedException e) { Thread.currentThread().interrupt(); }
}
```

`Thread.sleep()` lança `InterruptedException` que é checked. O padrão correto quando se apanha esta exceção é re-setar a flag de interrupção com `Thread.currentThread().interrupt()` para que o código chamador a possa detetar.

---

### 3.5 `GUI_TP1.java` — Interface Gráfica

A GUI é uma `JFrame` construída com posicionamento absoluto (null layout), estilo WindowBuilder do Eclipse.

#### Arranque — `main()` e EDT

```java
public static void main(String[] args) {
    EventQueue.invokeLater(() -> {
        try {
            GUI_TP1 frame = new GUI_TP1();
            frame.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    });
}
```

`EventQueue.invokeLater()` garante que a criação da janela ocorre na **EDT** (Event Dispatch Thread) — a única thread onde é seguro criar e manipular componentes Swing. Nunca se deve criar uma `JFrame` diretamente no `main()` (que corre na main thread).

#### Campo `robot` — inicializado depois da `textArea`

```java
private IRobot robot;  // declarado mas não inicializado aqui

public GUI_TP1() {
    // ... criação de todos os componentes ...

    textArea = new JTextArea();         // textArea criada
    scrollPane.setViewportView(textArea);

    // Só DEPOIS da textArea estar criada:
    robot = new SimuladorRobot(this::myPrintSempre);
}
```

Se `robot` fosse inicializado como campo (`private IRobot robot = new SimuladorRobot(...)`), o `SimuladorRobot` seria criado antes do construtor correr, antes de `textArea` existir. O consumer `this::myPrintSempre` iria tentar usar `textArea` que seria `null` → `NullPointerException`.

#### Botões de movimento — leitura dos campos na EDT

```java
JButtton_Esquerda.addActionListener(e -> {
    // parseCampoDouble corre AQUI, na EDT (dentro do listener)
    double raio   = parseCampoDouble(textField_Raio, dados.getRaio());
    double angulo = parseCampoDouble(textField_Angulo, dados.getAngulo());

    executarComando(() -> {
        // este lambda corre em SwingWorker (fora da EDT)
        robot.curvarEsquerda(raio, angulo);
        myPrint("Esquerda  raio=" + raio + " ângulo=" + angulo + "°");
    });
});
```

**Porquê ler os campos antes de entrar no SwingWorker?** Os `JTextField` são componentes Swing e só devem ser lidos na EDT. Se `parseCampoDouble` fosse chamado dentro do `doInBackground()` do SwingWorker (que corre numa thread separada), isso seria uma **violação da EDT** — potencialmente inseguro e causa de bugs difíceis de reproduzir.

A solução é ler os valores no listener (que corre na EDT), capturá-los em variáveis locais (implicitamente `final` por serem capturadas no lambda), e passá-los ao SwingWorker.

#### `executarComando()` — padrão genérico de execução

```java
private void executarComando(Runnable cmd) {
    if (!robot.isLigado()) {
        myPrintSempre("Robot não está ligado. Use o botão On/Off.");
        return;
    }
    setBotoesMovimento(false);              // bloqueia botões (EDT)
    new SwingWorker<Void, Void>() {
        @Override protected Void doInBackground() {
            cmd.run();                      // executa o comando (thread separada)
            return null;
        }
        @Override protected void done() {
            setBotoesMovimento(true);       // desbloqueia botões (EDT)
        }
    }.execute();
}
```

- Verifica se o robot está ligado antes de executar
- Desativa todos os botões de movimento para evitar dois comandos simultâneos
- Executa o comando numa thread separada (SwingWorker) para não congelar a GUI
- Quando termina, reativa os botões na EDT (o método `done()` corre sempre na EDT)

#### `toggleConexao()` — On/Off em SwingWorker

```java
private void toggleConexao() {
    String nomeRobot = textField_Robot.getText().trim();
    if (rdbtnOnoff.isSelected()) {
        myPrintSempre("A ligar ao robot: " + nomeRobot + "...");
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                return robot.ligar(nomeRobot);  // pode demorar (Bluetooth)
            }
            @Override protected void done() {
                try {
                    boolean ok = get();
                    rdbtnOnoff.setSelected(ok);
                    myPrintSempre(ok ? "Ligado a: " + nomeRobot : "Falha na ligação");
                    setBotoesMovimento(ok);
                } catch (Exception ex) {
                    myPrintSempre("Erro: " + ex.getMessage());
                    rdbtnOnoff.setSelected(false);
                }
            }
        }.execute();
    } else {
        robot.desligar();
        myPrintSempre("Desligado");
        setBotoesMovimento(false);
    }
}
```

A ligação Bluetooth pode demorar vários segundos. Se fosse chamada diretamente na EDT, a GUI congelava. Por isso corre num `SwingWorker<Boolean, Void>` — o tipo genérico `Boolean` é o tipo de retorno de `doInBackground()`, capturado em `done()` via `get()`.

#### `myPrintSempre()` — thread-safe

```java
private void myPrintSempre(String msg) {
    SwingUtilities.invokeLater(() -> {
        textArea.append(msg + "\n");
        textArea.setCaretPosition(textArea.getDocument().getLength());
    });
}
```

Este método pode ser chamado de qualquer thread (do SimuladorRobot, do SwingWorker, da EDT). O `SwingUtilities.invokeLater()` garante que a modificação da `textArea` ocorre sempre na EDT. `setCaretPosition` ao comprimento do documento faz o scroll automático para baixo.

#### `myPrint()` — mensagens de debug

```java
private void myPrint(String msg) {
    if (!chckbxDebug_1.isSelected()) return;
    myPrintSempre(msg);
}
```

Só escreve quando a checkbox "Debug" está ativa. Mensagens de ligação/desligação usam `myPrintSempre` diretamente.

#### `parseCampoDouble()` — leitura segura de campos numéricos

```java
private double parseCampoDouble(JTextField campo, double valorPadrao) {
    try { return Double.parseDouble(campo.getText().trim()); }
    catch (NumberFormatException e) { return valorPadrao; }
}
```

Se o utilizador deixar um campo vazio ou escrever texto inválido, usa o valor de `dados` (valor padrão) em vez de lançar exceção.

#### Cores dos botões no macOS

```java
JButtton_Esquerda.setOpaque(true);                        // força preenchimento opaco
JButtton_Esquerda.setBackground(new Color(0, 0, 255));    // azul
JButtton_Esquerda.setForeground(new Color(192, 192, 192));// texto cinzento claro
JButtton_Esquerda.setFont(new Font("Times New Roman", Font.PLAIN, 14));
```

No macOS, os `JButton` têm um look-and-feel nativo que ignora `setBackground()` por defeito. O `setOpaque(true)` força o Swing a desenhar o fundo. Com ele, o macOS ainda renderiza com cantos arredondados (estilo nativo) mas usa a cor de fundo definida.

---

## 4. Projeto 2

O Projeto 2 estende o Projeto 1 com cálculo automático de trajetórias. As classes `IRobot`, `DadosGUI_TP1`, `SimuladorRobot` e `myRobotLego` são idênticas ao Projeto 1 (apenas o package muda: `fr_tp2`).

### 4.1 `CalculadorTrajetoria.java` — Calculador de Trajetórias

Esta é a classe central do Projeto 2. Dado um ponto objetivo `(xf, yf, φf)`, calcula automaticamente a sequência de movimentos necessária para o robot chegar lá.

#### Sistema de Coordenadas

```
         Xi (cima) — direção inicial do robot
          ↑
          │  φf > 0  → ponto à esquerda
          │  φf < 0  → ponto à direita
          └──────→ Yi (esquerda)

  Ponto objetivo: (xf, yf, φf)
    xf  = distância para a frente (sempre > 0)
    yf  = distância para a esquerda (positivo) ou direita (negativo)
    φf  = orientação final do robot em graus
```

#### Constantes

```java
public static final double DBW     = 9.5;  // distância entre rodas (cm)
public static final int    V_ROBOT = 40;   // velocidade base (%)
public static final int    V_MIN   = 20;   // velocidade mínima (%)
public static final int    V_MAX   = 80;   // velocidade máxima (%)
```

#### Tipos de dados internos

```java
public enum TipoMovimento { CURVA_ESQ, CURVA_DIR, RETA }

public static class Passo {
    public final TipoMovimento tipo;
    public final double raio;       // para curvas
    public final double angulo;     // para curvas (graus)
    public final double distancia;  // para reta (cm)
}

public static class Resultado {
    public final int         trajetoria; // 1, 2 ou 3
    public final double      raio;
    public final List<Passo> passos;
}
```

`Passo` é imutável (campos `final`). Um `Resultado` é uma lista ordenada de `Passo`s que a GUI executa sequencialmente.

#### API pública — `calcular()`

```java
public static Resultado calcular(double xf, double yf, double phiGraus) {
    if (xf <= 0) return null;             // ponto atrás do robot — inatingível

    Resultado r1 = tentarT1(xf, yf, phiGraus);
    if (r1 != null) return r1;            // T1 é válida, usa-a

    return calcularT2T3(xf, yf, phiGraus); // senão T2 ou T3
}
```

Estratégia de seleção: tenta T1 primeiro. Se não for válida (φf≤0 ou raio inválido), usa T2/T3 (distinguidas pelo valor de `yc2`).

---

#### Trajetória 1 — `tentarT1()`

**Forma:** `curvaEsquerda(r, α1)` + `reta(d)` + `curvaEsquerda(r, α2)`

Dois arcos de igual raio para a esquerda com uma reta no meio. Só é válida quando `φf > 0`.

**Dedução da equação:**

O robot começa em (0,0) com φ=0. O centro do 1º arco está em `c1 = (0, r)` (perpendicular à esquerda do robot). O centro do 2º arco, que leva o robot a chegar a `(xf, yf)` com orientação `φf`, está em:

```
c2 = (xf − r·sin(φf), yf + r·cos(φf))
```

Para os dois arcos serem ligados por uma reta tangente a ambos, a distância entre centros deve ser igual ao comprimento da reta `d`:

```
d = |c2 − c1|
```

A condição geométrica para os dois arcos terem o mesmo raio e a reta ser tangente a ambos implica, após desenvolvimento algébrico:

```
a·r² + b·r + c = 0

onde:
  a = 2 + 2·cos(φf)
  b = 2·yf·(1 − cos(φf)) + 2·xf·sin(φf)
  c = −(xf² + yf²)
```

```java
double a = 2 + 2 * cosP;
double b = 2 * yf * (1 - cosP) + 2 * xf * sinP;
double c = -(xf * xf + yf * yf);
double disc = b * b - 4 * a * c;
double rT = (-b + Math.sqrt(disc)) / (2 * a);  // raiz positiva
```

Com o raio teórico `rT`, calcula-se o raio prático `rP` (ver secção abaixo), e depois os ângulos dos dois arcos:

```java
double cosA1 = xc2 / d12;
double alpha1 = Math.toDegrees(Math.acos(cosA1));
double alpha2 = phiGraus - alpha1;
```

A trajetória é válida se `alpha1 > 0` e `alpha2 > 0` (ambos os arcos têm de existir).

---

#### Trajetórias 2 e 3 — `calcularT2T3()`

**Forma:** `curvaEsquerda(r, α)` + `curvaDireita(r, α−φf)`

Dois arcos de igual raio — um para a esquerda, outro para a direita. Sem reta intermédia.

**Dedução da equação:**

Centro do 1º arco (esquerda): `c1 = (0, r)`
Centro do 2º arco (direita): `c2 = (xf + r·sin(φf), yf − r·cos(φf))`

Para os dois arcos serem tangentes entre si (transição suave), a distância entre centros deve ser `2r`:

```
|c1 c2|² = (2r)²
```

Expandindo e simplificando:

```
a·r² + b·r + c = 0

onde:
  a = 2 − 2·cos(φf)
  b = 2·yf·(1 + cos(φf)) − 2·xf·sin(φf)
  c = −(xf² + yf²)
```

**Caso especial φf ≈ 0°:** `a ≈ 0` → a equação degenera numa equação linear:

```
4·yf·r = xf² + yf²
r = (xf² + yf²) / (4·yf)
```

```java
if (Math.abs(a) < 1e-9) {
    double bLin = 4 * yf;
    if (Math.abs(bLin) < 1e-9) return null;
    r = -c / bLin;
}
```

**Distinção T2 vs T3:**

Após obter `r`, calcula-se `yc2` e `yc1 = r`:

```java
double yc2 = yf - r * cosP;
double yc1 = r;

if (yc2 < yc1)  → T2  (centro do 2º arco mais perto da origem)
else            → T3  (centro do 2º arco mais longe — φf negativo)
```

O ângulo `α` do 1º arco vem de `sin(α) = xc2 / (2r)`:

```java
double baseAlpha = Math.toDegrees(Math.asin(xc2 / (2 * r)));

if (T2) alpha = baseAlpha;           // ângulo agudo (arco mais curto)
if (T3) alpha = 180.0 - baseAlpha;  // ângulo obtuso (arco mais longo)
```

O ângulo do 2º arco (direita): `alpha_direita = alpha − φf`.

---

#### `calcRaioPratico()` — raio teórico → raio prático

```java
static double calcRaioPratico(double rT) {
    if (rT <= DBW / 2) return rT;
    double f     = (rT + DBW / 2) / (rT - DBW / 2);  // rácio ideal
    int    vSlow = (int) Math.floor(2.0 * V_ROBOT / (f + 1));  // roda lenta
    vSlow = Math.max(V_MIN, Math.min(V_MAX - 1, vSlow));        // clamp [20, 79]
    int    vFast = 2 * V_ROBOT - vSlow;                         // roda rápida
    if (vSlow == 0) return rT;
    double fP = (double) vFast / vSlow;
    if (fP <= 1.0) return rT;
    return (DBW / 2) * (fP + 1) / (fP - 1);                    // raio prático
}
```

**Porquê este cálculo?** Os motores EV3 só aceitam velocidades inteiras (%). O raio teórico pode requerer, por exemplo, `vSlow = 32.60%` — mas os motores só aceitam `32%`. Isso altera ligeiramente o raio efetivo.

O algoritmo (slide 04 do professor):

1. `f = (rT + dbw/2) / (rT − dbw/2)` — rácio de velocidades ideal para o raio teórico
2. `vSlow = floor(2·vRobot / (f+1))` — velocidade da roda lenta (`floor`, não `round`)
3. `vFast = 2·vRobot − vSlow` — as duas velocidades somam sempre `2·vRobot`
4. `fP = vFast / vSlow` — rácio prático
5. `rP = (dbw/2)·(fP+1)/(fP−1)` — raio resultante das velocidades práticas

O `floor` (e não `round`) é importante: garante que `vSlow ≤ vSlow_ideal`, mantendo `fP ≥ f` e `rP ≥ rT`. Usar `round` daria resultados inconsistentes com os exemplos dos slides.

#### `toConsola()` — formatação do resultado

```java
public String toConsola() {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("── Trajetória %d  (r=%.2f cm) ──%n", trajetoria, raio));
    for (int i = 0; i < passos.size(); i++)
        sb.append(String.format("  Passo %d: %s%n", i + 1, passos.get(i)));
    return sb.toString();
}
```

Produz texto multi-linha para mostrar na `textArea`. `%n` é o separador de linha independente de plataforma.

---

### 4.2 `GUI_TP2.java` — Interface Gráfica Estendida

Estende a GUI do TP1 com um painel adicional "Ponto Objetivo".

#### Novos campos

```java
private JTextField textField_Xf, textField_Yf, textField_PhiF;
private JButton    btnCalcular, btnExecutar;
private CalculadorTrajetoria.Resultado ultimaTrajetoria = null;
```

`ultimaTrajetoria` guarda o último resultado calculado. `btnExecutar` só fica ativo depois de calcular uma trajetória válida.

#### Painel com borda (TitledBorder)

```java
JPanel panelObjetivo = new JPanel(null);
panelObjetivo.setBorder(BorderFactory.createTitledBorder(
    BorderFactory.createEtchedBorder(),
    "Ponto Objetivo  (Xf, Yf, φf)",
    TitledBorder.LEFT, TitledBorder.TOP,
    new Font("Times New Roman", Font.BOLD, 13)));
panelObjetivo.setBounds(10, 213, 440, 110);
contentPane.add(panelObjetivo);
```

`TitledBorder` cria um painel com título visível. `createEtchedBorder()` é o estilo de borda gravada. Os componentes dentro do painel usam coordenadas relativas ao painel (não à janela).

#### `botao()` — factory method para botões de movimento

```java
private JButton botao(String texto, Color cor) {
    JButton b = new JButton(texto);
    b.setOpaque(true);
    b.setBackground(cor);
    b.setForeground(new Color(192, 192, 192));
    b.setFont(new Font("Times New Roman", Font.PLAIN, 14));
    return b;
}
```

Evita repetição do código de estilo para os 5 botões de movimento. Em vez de copiar 4 linhas por botão, chama `botao("Frente", new Color(128, 255, 0))`.

#### Botões "Calcular" e "Executar" — estilo diferente

```java
btnCalcular = new JButton("Calcular Trajetória");
btnCalcular.setUI(new javax.swing.plaf.basic.BasicButtonUI());
btnCalcular.setBackground(new Color(255, 140, 0));  // laranja
btnCalcular.setForeground(Color.WHITE);
btnCalcular.setOpaque(true);
```

Estes dois botões usam `BasicButtonUI` para forçar o estilo flat (sem cantos arredondados do macOS), dando-lhes visual mais proeminente para os distinguir dos botões de movimento.

#### `calcularTrajetoria()`

```java
private void calcularTrajetoria() {
    double xf   = parseDouble(textField_Xf,   70.0);
    double yf   = parseDouble(textField_Yf,   40.0);
    double phiF = parseDouble(textField_PhiF, 70.0);

    CalculadorTrajetoria.Resultado res = CalculadorTrajetoria.calcular(xf, yf, phiF);

    if (res == null) {
        myPrintSempre("Ponto não atingível com as trajetórias suportadas.");
        ultimaTrajetoria = null;
        btnExecutar.setEnabled(false);
        return;
    }

    ultimaTrajetoria = res;
    myPrintSempre(res.toConsola());
    btnExecutar.setEnabled(robot.isLigado());
}
```

Corre na EDT (chamada pelo listener do botão). Não precisa de SwingWorker porque `CalculadorTrajetoria.calcular()` é só cálculo matemático — termina instantaneamente, sem I/O nem bloqueio.

#### `executarTrajetoria()`

```java
private void executarTrajetoria() {
    if (ultimaTrajetoria == null || !robot.isLigado()) return;
    setBotoesMovimento(false);
    btnExecutar.setEnabled(false);

    new SwingWorker<Void, Void>() {
        @Override
        protected Void doInBackground() {
            for (CalculadorTrajetoria.Passo p : ultimaTrajetoria.passos) {
                switch (p.tipo) {
                    case CURVA_ESQ: robot.curvarEsquerda(p.raio, p.angulo); break;
                    case CURVA_DIR: robot.curvarDireita(p.raio, p.angulo);  break;
                    case RETA:      robot.reta(p.distancia);                 break;
                }
            }
            return null;
        }
        @Override
        protected void done() {
            myPrintSempre("Trajetória concluída.");
            setBotoesMovimento(robot.isLigado());
            btnExecutar.setEnabled(robot.isLigado() && ultimaTrajetoria != null);
        }
    }.execute();
}
```

Percorre a lista de `Passo`s e chama o método correspondente do robot. Corre num SwingWorker porque os comandos do robot bloqueiam (sleep) — se corressem na EDT, a GUI congelava durante toda a trajetória.

---

## 5. Modelo de Threads — EDT e SwingWorker

```
Main Thread          EDT                    SwingWorker
    │                 │                         │
    │  invokeLater()  │                         │
    ├────────────────►│                         │
    │                 │  new GUI_TP1()           │
    │                 │  (cria componentes)      │
    │                 │                         │
    │                 │  botão clicado           │
    │                 │  listener corre          │
    │                 │  ler campos              │
    │                 │  .execute()             │
    │                 ├───────────────────────►│
    │                 │                   doInBackground()
    │                 │ ◄──────── myPrintSempre()
    │                 │  invokeLater()          │
    │                 │  textArea.append()      │
    │                 │                   done()
    │                 │◄────────────────────────│
    │                 │  setBotoesMovimento()    │
```

**Regras fundamentais:**
1. Todos os componentes Swing devem ser criados e modificados **apenas na EDT**
2. Operações lentas (Bluetooth, sleep) devem correr **fora da EDT** (num SwingWorker)
3. Para modificar a GUI a partir de outra thread: usar `SwingUtilities.invokeLater()`
4. `SwingWorker.done()` corre automaticamente na EDT — seguro para modificar componentes
5. `SwingWorker.doInBackground()` corre numa thread do pool — NÃO tocar em componentes Swing aqui

---

## 6. Padrão Consumer\<String\> — Log Desacoplado

O `SimuladorRobot` precisa de escrever mensagens, mas não deve depender da `JTextArea` da GUI.

**Solução:** injetar a função de escrita no construtor:

```java
// No SimuladorRobot:
private final Consumer<String> logger;

public SimuladorRobot(Consumer<String> logger) {
    this.logger = logger;
}

private void log(String msg) {
    logger.accept("[SIMULADOR] " + msg);  // chama a função injetada
}
```

```java
// Na GUI:
robot = new SimuladorRobot(this::myPrintSempre);
//                         ^^^^^^^^^^^^^^^^^^^
// "quando precisares de fazer log, chama este método"
```

`this::myPrintSempre` é uma **method reference** — equivale ao lambda `msg -> this.myPrintSempre(msg)`. O tipo `Consumer<String>` é uma interface funcional com um único método abstrato: `void accept(String t)`.

**Vantagem:** o mesmo `SimuladorRobot` pode ser usado com qualquer função de log — `System.out::println` para testes unitários, `this::myPrintSempre` para a GUI, um logger de ficheiro, etc. — sem alterar uma linha do `SimuladorRobot`.

---

## 7. Compilar e Executar

### Projeto 1

```bash
cd projeto1
mkdir -p out
javac --module-path lib -d out src/module-info.java src/fr_tp1/*.java
java --module-path lib:out -m FR_TP1/fr_tp1.GUI_TP1
```

### Projeto 2

```bash
cd projeto2
mkdir -p out
javac --module-path lib -d out src/module-info.java src/fr_tp2/*.java
java --module-path lib:out -m FR_TP2/fr_tp2.GUI_TP2
```

### Explicação das flags

| Flag | Significado |
|---|---|
| `--module-path lib` | Onde o compilador/runtime procura JARs de módulos externos |
| `-d out` | Diretório de saída dos ficheiros `.class` compilados |
| `-m FR_TP1/fr_tp1.GUI_TP1` | Módulo de arranque `FR_TP1`, classe principal `fr_tp1.GUI_TP1` |
| `lib:out` | Módulos em `lib/` mais o módulo compilado em `out/` (`:` em Unix) |

### Usar com o Robot Real

Em `GUI_TP1.java` (ou `GUI_TP2.java`), no construtor, substituir:

```java
// Simulador (por defeito):
robot = new SimuladorRobot(this::myPrintSempre);

// Robot real (quando Bluetooth disponível):
robot = new myRobotLego();
```

### Exemplos para testar (Projeto 2)

| Ponto objetivo | Trajetória | Descrição |
|---|---|---|
| Xf=70, Yf=40, φf=70° | T1 | curvaEsq + reta + curvaEsq |
| Xf=70, Yf=40, φf=20° | T2 | curvaEsq + curvaDireita (yc2 < yc1) |
| Xf=50, Yf=40, φf=−40° | T3 | curvaEsq + curvaDireita (yc2 ≥ yc1) |

Introduzir os valores nos campos do painel "Ponto Objetivo" e clicar "Calcular Trajetória" para ver os passos detalhados na consola da GUI.
