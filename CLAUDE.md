# Fundamentos de Robótica — 1º Trabalho Prático

**Disciplina:** Fundamentos de Robótica — MEIM/MEET, 2º Sem 2025/2026  
**Docente:** Jorge Pais  
**Duração:** 5 semanas

---

## Objetivo

Desenvolver uma GUI em Java Swing para controlar o robot Lego EV3 via Bluetooth.

**Duas partes obrigatórias:**
1. GUI funcional com simulador de terminal
2. Classe `myRobotLego` implementada com `InterpretadorEV3.jar` (API real do EV3)

---

## Estrutura do Projeto

```
robo/
├── projeto 1/
│   ├── lib/
│   │   ├── RobotLegoEV32026.jar       # biblioteca high-level do robot
│   │   ├── InterpretadorEV32026.jar   # API low-level do EV3
│   │   └── bluecove-2.1.1-SNAPSHOT.jar # Bluetooth
│   └── src/
│       ├── module-info.java
│       └── fr_tp1/
│           ├── GUI_TP1.java           # JFrame principal
│           ├── DadosGUI_TP1.java      # modelo de dados
│           ├── IRobot.java            # interface comum
│           ├── SimuladorRobot.java    # simulador de terminal
│           └── myRobotLego.java       # robot real via InterpretadorEV3
├── artefactos/                        # ignorado pelo git (.gitignore)
├── slides/
└── Enunciado do Trabalho Prático 1.pdf
```

---

## Compilação e Execução

```bash
cd "projeto 1"
mkdir -p out
javac --module-path lib -d out src/module-info.java src/fr_tp1/*.java
java --module-path lib:out -m FR_TP1/fr_tp1.GUI_TP1
```

Em Eclipse: adicionar os JARs de `lib/` ao **module path** (Build Path → Modulepath).

---

## Estado do Código (tudo implementado)

### `DadosGUI_TP1.java` — modelo de dados
- Campos: `robotName`, `raio`, `angulo`, `distancia`, `onOff`, `debug`, `consola`

### `IRobot.java` — interface comum
- `ligar(String)`, `desligar()`, `reta(int)`, `curvarEsquerda(double, int)`, `curvarDireita(double, int)`, `parar(boolean)`, `setVelocidade(int)`, `isLigado()`

### `SimuladorRobot.java` — simulador de terminal
- Implementa `IRobot`; rastreia posição (x, y) e rumo com cinemática real (arco de circunferência)
- Imprime cada ação em `System.out` com coordenadas atualizadas

### `myRobotLego.java` — robot real via InterpretadorEV3
- Motor B = esquerdo, Motor C = direito
- `reta()` → `OnFwd(OUT_BC, vel)` + sleep calculado + `Off`
- `curvarEsquerda/Direita()` → velocidades diferentes por roda (cinemática: raio ± dbw/2)
- `parar(travar)` → `Off` (travagem) ou `Float` (livre)
- Parâmetros físicos: diâmetro roda=5.6 cm, dbw=9.5 cm, vel_base=40%

### `GUI_TP1.java` — interface gráfica
- On/Off: liga/desliga em SwingWorker (não bloqueia EDT)
- Botões de movimento: executados em SwingWorker; desativados durante execução
- `myPrint()` → consola só com Debug ativo; `myPrintSempre()` → sempre visível
- **Por defeito usa `SimuladorRobot`** — trocar para `myRobotLego` quando robot disponível

---

## Para Usar o Robot Real

Em `GUI_TP1.java`, campo `robot` (perto do topo da classe):
```java
// Trocar esta linha:
private IRobot robot = new SimuladorRobot();
// Por:
private IRobot robot = new myRobotLego();
```

---

## Notas Técnicas

- O layout da GUI usa **null layout** (posições absolutas) do WindowBuilder — não alterar para layout managers
- Todas as operações do robot correm em **SwingWorker** para não bloquear a EDT
- Motor B = esquerdo, Motor C = direito (configuração padrão EV3)
- Os ficheiros `.class` estão excluídos do git via `.gitignore`
