# Relatório — 1º Trabalho Prático  
**Fundamentos de Robótica** | MEIM/MEET | 2º Sem 2025/2026  
**Autor:** Bruno Rodrigues  
**Docente:** Jorge Pais

---

## 1. Objetivo

Desenvolvimento de uma interface gráfica em Java Swing para controlar o robot didático Lego EV3 via Bluetooth, composta por duas partes:

1. **GUI funcional** com simulador de terminal para teste local
2. **Classe `myRobotLego`** que implementa os movimentos usando a biblioteca `InterpretadorEV3.jar`

---

## 2. Como Compilar e Executar

### Pré-requisitos
- Java 11 ou superior (`java --version`)
- Estar dentro da pasta `projeto 1/`

### Compilar

```bash
cd "projeto 1"
mkdir -p out
javac --module-path lib -d out src/module-info.java src/fr_tp1/*.java
```

### Executar a GUI

```bash
java --module-path lib:out -m FR_TP1/fr_tp1.GUI_TP1
```

### Testar com o Simulador (sem robot físico)

1. Executar o comando acima — a janela GUI abre
2. O campo **Robot** já tem o nome pré-preenchido (`Bruno`)
3. Clicar em **On/Off** — o simulador "liga" e os botões de movimento ativam-se
4. Marcar a checkbox **Debug** para ver os comandos na consola da GUI
5. Clicar em qualquer botão de movimento (Frente, Esquerda, etc.)
6. **No terminal** onde a aplicação foi lançada aparece o output do simulador:

```
[SIMULADOR] Ligado ao robot: Bruno
[SIMULADOR] Posição inicial: (0.00, 0.00) rumo: 0.00°
[SIMULADOR] Reta 50 cm  →  pos: (50.00, 0.00)  rumo: 0.00°
[SIMULADOR] Curvar esquerda raio=20.00 ângulo=50°  →  pos: (65.32, 14.70)  rumo: 50.00°
```

### Usar com o Robot Real (quando Bluetooth disponível)

Em `GUI_TP1.java`, substituir a linha do campo `robot`:

```java
// Antes (simulador):
private IRobot robot = new SimuladorRobot();

// Depois (robot real):
private IRobot robot = new myRobotLego();
```

Recompilar e executar. Escrever o nome Bluetooth do robot no campo **Robot** e clicar **On/Off**.

---

## 3. Estrutura de Ficheiros

```
projeto 1/
├── lib/
│   ├── RobotLegoEV32026.jar          # biblioteca high-level do EV3
│   ├── InterpretadorEV32026.jar      # API low-level de motores/sensores
│   └── bluecove-2.1.1-SNAPSHOT.jar  # stack Bluetooth
└── src/
    ├── module-info.java
    └── fr_tp1/
        ├── IRobot.java               # interface comum
        ├── DadosGUI_TP1.java         # modelo de dados da GUI
        ├── SimuladorRobot.java       # simulador de terminal
        ├── myRobotLego.java          # robot real (InterpretadorEV3)
        └── GUI_TP1.java              # janela principal (Swing)
```

---

## 4. Descrição das Classes

### 4.1 `IRobot` — Interface Comum

Define o contrato que tanto o simulador como o robot real implementam:

```java
boolean ligar(String nome);
void desligar();
void reta(int distancia);
void curvarEsquerda(double raio, int angulo);
void curvarDireita(double raio, int angulo);
void parar(boolean travar);
void setVelocidade(int vel);
boolean isLigado();
```

Esta interface permite trocar o simulador pelo robot real sem alterar a GUI — basta mudar a instância do campo `robot`.

---

### 4.2 `DadosGUI_TP1` — Modelo de Dados

Classe de dados (POJO) que guarda os valores iniciais dos campos da GUI:

| Campo | Tipo | Valor inicial | Descrição |
|-------|------|--------------|-----------|
| `robotName` | `String` | `"Bruno"` | Nome Bluetooth do robot |
| `raio` | `int` | `20` | Raio de curvatura (cm) |
| `angulo` | `int` | `50` | Ângulo de curvatura (graus) |
| `distancia` | `int` | `50` | Distância em linha reta (cm) |
| `debug` | `boolean` | `true` | Estado inicial da checkbox Debug |
| `onOff` | `boolean` | `false` | Estado inicial do botão On/Off |

---

### 4.3 `SimuladorRobot` — Simulador de Terminal

Implementa `IRobot` simulando os movimentos no terminal. Mantém um estado cinemático interno com posição `(x, y)` e `rumo` (orientação em graus, 0°=Este, 90°=Norte).

#### Cinemática implementada

**Reta** — desloca o robot na direção do rumo atual:

```
x += distancia × cos(rumo)
y += distancia × sin(rumo)
```

**Curva à esquerda** (arco counter-clockwise, raio `r`, ângulo `α`):  
O centro do arco fica perpendicular à esquerda. A posição final resulta da rotação em torno desse centro:

```
x += r × (sin(θ + α) − sin(θ))
y += r × (cos(θ) − cos(θ + α))
rumo += α
```

**Curva à direita** (arco clockwise, raio `r`, ângulo `α`):  
Centro à direita do robot:

```
x += r × (sin(θ) − sin(θ − α))
y += r × (cos(θ − α) − cos(θ))
rumo -= α
```

Cada operação imprime no terminal (`System.out`) a nova posição e rumo.

---

### 4.4 `myRobotLego` — Robot Real

Implementa `IRobot` usando `InterpretadorEV3`. Controla os motores B (esquerdo) e C (direito) do EV3.

#### Parâmetros físicos

| Parâmetro | Valor |
|-----------|-------|
| Diâmetro das rodas | 5,6 cm |
| Circunferência das rodas | π × 5,6 ≈ 17,59 cm |
| Distância entre rodas (dbw) | 9,5 cm |
| Velocidade base | 40% |
| Velocidade do motor a 100% | 720°/s ≈ 35,2 cm/s |

#### Cálculo do tempo de execução

Para percorrer uma distância `d` cm à velocidade `v`%:

```
cmPerSec = (v / 100) × 720 × (circunferência / 360)
tempo_ms  = (d / cmPerSec) × 1000
```

A execução segue o padrão: `OnFwd` → `Thread.sleep(tempo_ms)` → `Off`.

#### Cinemática das curvas

Para uma curva à esquerda com raio `r` e ângulo `α`:

```
raio_exterior (roda C) = r + dbw/2
raio_interior (roda B) = r − dbw/2

vel_exterior = velocidade_base
vel_interior = velocidade_base × (raio_interior / raio_exterior)
```

Se `vel_interior < 0` (curva muito fechada), a roda interior usa `OnRev`. Para curva à direita os motores B e C trocam de papel.

---

### 4.5 `GUI_TP1` — Interface Gráfica

Janela principal criada com o editor WindowBuilder do Eclipse. Usa **null layout** (posicionamento absoluto). Componentes principais:

| Componente | Tipo Swing | Função |
|-----------|-----------|--------|
| `textField_Robot` | `JTextField` | Nome do robot |
| `rdbtnOnoff` | `JRadioButton` | Liga/desliga comunicação |
| `textField_Raio` | `JTextField` | Raio de curvatura (cm) |
| `textField_Angulo` | `JTextField` | Ângulo de curvatura (°) |
| `textField_Distancia` | `JTextField` | Distância em linha reta (cm) |
| `JButtton_Frente` | `JButton` (verde) | Movimento para a frente |
| `JButtton_Retaguarda` | `JButton` (magenta) | Movimento para trás |
| `JButtton_Esquerda` | `JButton` (azul) | Curva à esquerda |
| `JButtton_Direita` | `JButton` (amarelo) | Curva à direita |
| `JButtton_Parar` | `JButton` (vermelho) | Parar o robot |
| `chckbxDebug_1` | `JCheckBox` | Ativa mensagens na consola |
| `textArea` | `JTextArea` | Consola de log |

#### Gestão de threads — SwingWorker

Os comandos do robot (que bloqueiam em `Thread.sleep`) correm em `SwingWorker` para não congelar a EDT do Swing:

```java
private void executarComando(Runnable cmd) {
    setBotoesMovimento(false);           // desativa botões durante execução
    new SwingWorker<Void, Void>() {
        @Override protected Void doInBackground() {
            cmd.run();                   // corre no thread worker
            return null;
        }
        @Override protected void done() {
            setBotoesMovimento(true);    // reativa botões na EDT
        }
    }.execute();
}
```

#### Consola de log

- `myPrint(msg)` — só escreve se a checkbox **Debug** estiver ativa
- `myPrintSempre(msg)` — escreve sempre (mensagens de ligação/erro)

---

## 5. Diagrama de Arquitectura

```
┌─────────────────────────────────────────┐
│              GUI_TP1 (JFrame)           │
│                                         │
│  [Robot: Bruno]      [○ On/Off]         │
│  [Raio: 20] [Ângulo: 50] [Distância:50] │
│                                         │
│         [  Frente  ]                    │
│  [Esquerda] [Parar] [Direita]           │
│         [Retaguarda]                    │
│                                         │
│  [✓ Debug]         Consola              │
│  ┌─────────────────────────────────┐    │
│  │ Ligado a: Bruno                 │    │
│  │ Frente  distância=50 cm         │    │
│  └─────────────────────────────────┘    │
└────────────────┬────────────────────────┘
                 │ IRobot
        ┌────────┴────────┐
        │                 │
 SimuladorRobot     myRobotLego
 (terminal)         (InterpretadorEV3)
                         │
                   [Motor B] [Motor C]
                   esquerdo  direito
```

---

## 6. Exemplo de Sessão de Teste (Simulador)

Sequência com os valores por defeito (Raio=20, Ângulo=50, Distância=50):

```
[SIMULADOR] Ligado ao robot: Bruno
[SIMULADOR] Posição inicial: (0.00, 0.00) rumo: 0.00°
[SIMULADOR] Reta 50 cm          →  pos: (50.00,  0.00)  rumo:  0.00°
[SIMULADOR] Curvar esquerda r=20 α=50°  →  pos: (65.32, 14.70)  rumo: 50.00°
[SIMULADOR] Curvar direita  r=20 α=50°  →  pos: (80.64,  0.00)  rumo:  0.00°
[SIMULADOR] Parar (travagem)    →  pos: (80.64,  0.00)
[SIMULADOR] Desligado do robot: Bruno
```
