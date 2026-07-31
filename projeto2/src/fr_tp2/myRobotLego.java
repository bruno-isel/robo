package fr_tp2;

import interpretador.InterpretadorEV3;
import java.util.function.Consumer;

/**
 * Implementação real do robot EV3 usando InterpretadorEV3.
 *
 * Parâmetros físicos do robot (configuração padrão EV3):
 *   - Diâmetro das rodas: 5.6 cm  (raio ≈ 2.8 cm)
 *   - Distância entre rodas (dbw): 9.5 cm
 *   - Motor esquerdo: porta B  /  Motor direito: porta C
 *   - Velocidade base: 40%
 */
public class myRobotLego implements IRobot {

    private static final double WHEEL_DIAM   = 5.6;
    private static final double WHEEL_RADIUS = WHEEL_DIAM / 2;
    private static final double DBW          = 9.5;

    private final InterpretadorEV3 ev3;
    private final Consumer<String> guiLog;
    private boolean ligado = false;
    private int velocidade = 40;

    // Usados para interromper um movimento em curso a partir do botao Parar,
    // sem chamar ev3 a partir de duas threads ao mesmo tempo (exclusao mutua
    // do InterpretadorEV3): quem chama ev3.Off/Float e sempre a thread do
    // movimento em curso; parar() so define as flags quando ha movimento ativo.
    private volatile boolean pedidoParar = false;
    private volatile boolean travarAoParar = true;
    private volatile boolean emMovimento = false;

    public myRobotLego() {
        this(null);
    }

    public myRobotLego(Consumer<String> guiLog) {
        this.guiLog = guiLog;
        ev3 = new InterpretadorEV3();
    }

    @Override
    public boolean ligar(String nome) {
        // O InterpretadorEV3 imprime os passos da procura/ligação Bluetooth
        // (ex: "Dispositivo bluetooth X encontrado/não encontrado.") direto
        // para System.out, sem passar pelo guiLog - por isso só apareciam no
        // Console do Eclipse. Redireciona System.out temporariamente para que
        // essas linhas também apareçam na consola da GUI.
        java.io.PrintStream original = System.out;
        java.io.PrintStream comGuiLog = new java.io.PrintStream(original) {
            @Override public void println(String x) {
                original.println(x);
                if (guiLog != null) guiLog.accept(x);
            }
        };
        System.setOut(comGuiLog);
        try {
            ligado = ev3.OpenEV3(nome);
        } finally {
            System.setOut(original);
        }
        if (ligado) {
            log("[EV3] Ligado a: " + nome);
            ev3.ResetAll();
        } else {
            log("[EV3] Falha na ligação a: " + nome);
        }
        return ligado;
    }

    @Override
    public void desligar() {
        if (ligado) {
            ev3.Off(InterpretadorEV3.OUT_BC);
            ev3.CloseEV3();
            ligado = false;
            log("[EV3] Desligado");
        }
    }

    @Override
    public void reta(double distancia) {
        if (!verificar()) return;
        iniciarMovimento();
        int graus = distanciaParaGraus(distancia);
        log(String.format("[EV3] straight(%.2f cm) → %d graus de roda", distancia, graus));
        ev3.OnFwd(InterpretadorEV3.OUT_B, velocidade, InterpretadorEV3.OUT_C, velocidade);
        esperarRotacao(InterpretadorEV3.OUT_B, graus);
        terminarMovimento();
    }

    @Override
    public void recuar(double distancia) {
        if (!verificar()) return;
        iniciarMovimento();
        int graus = distanciaParaGraus(distancia);
        log(String.format("[EV3] Recuar(%.2f cm) → %d graus de roda", distancia, graus));
        ev3.OnRev(InterpretadorEV3.OUT_B, velocidade, InterpretadorEV3.OUT_C, velocidade);
        esperarRotacao(InterpretadorEV3.OUT_B, graus);
        terminarMovimento();
    }

    @Override
    public void curvarEsquerda(double raio, double angulo) {
        if (!verificar()) return;
        iniciarMovimento();
        // curva esquerda: roda direita (OUT_C) e exterior
        double raioExt = raio + DBW / 2;
        double raioInt = raio - DBW / 2;
        int velExt = velocidade;
        int velInt = (int) Math.round(velocidade * raioInt / raioExt);
        int graus = arcParaGrausRoda(raioExt, angulo);
        log(String.format("[EV3] curveLeft(%.2f, %.2f) → %d graus roda ext", raio, angulo, graus));
        acionarCurva(InterpretadorEV3.OUT_C, velExt, InterpretadorEV3.OUT_B, velInt, graus);
    }

    @Override
    public void curvarDireita(double raio, double angulo) {
        if (!verificar()) return;
        iniciarMovimento();
        // curva direita: roda esquerda (OUT_B) e exterior
        double raioExt = raio + DBW / 2;
        double raioInt = raio - DBW / 2;
        int velExt = velocidade;
        int velInt = (int) Math.round(velocidade * raioInt / raioExt);
        int graus = arcParaGrausRoda(raioExt, angulo);
        log(String.format("[EV3] curveRight(%.2f, %.2f) → %d graus roda ext", raio, angulo, graus));
        acionarCurva(InterpretadorEV3.OUT_B, velExt, InterpretadorEV3.OUT_C, velInt, graus);
    }

    @Override
    public void parar(boolean travar) {
        if (!verificar()) return;
        log("[EV3] Parar");
        travarAoParar = travar;
        pedidoParar = true;
        // Se ha um movimento em curso, e a thread desse movimento que chama
        // ev3.Off/Float (assim que detetar pedidoParar em esperarRotacao) -
        // nunca a partir daqui, para nao violar a exclusao mutua do ev3.
        if (!emMovimento) {
            if (travar) ev3.Off(InterpretadorEV3.OUT_BC);
            else ev3.Float(InterpretadorEV3.OUT_BC);
        }
    }

    @Override
    public void setVelocidade(int vel) {
        this.velocidade = Math.max(20, Math.min(80, vel));
        log("[EV3] Velocidade: " + this.velocidade + "%");
    }

    @Override
    public boolean isLigado() {
        return ligado;
    }

    private void acionarCurva(int motorExt, int velExt, int motorInt, int velInt, int graus) {
        if (velInt >= 0) {
            ev3.OnFwd(motorExt, velExt, motorInt, velInt);
        } else {
            ev3.OnFwd(motorExt, velExt);
            ev3.OnRev(motorInt, -velInt);
        }
        esperarRotacao(motorExt, graus);
        terminarMovimento();
    }

    // Marca inicio de um movimento: reset das flags de paragem pedida.
    private void iniciarMovimento() {
        pedidoParar = false;
        travarAoParar = true;
        emMovimento = true;
    }

    // Fim natural ou interrompido de um movimento: para os motores respeitando
    // o modo pedido por parar() (travar/Off ou deixar andar livre/Float).
    private void terminarMovimento() {
        if (travarAoParar) ev3.Off(InterpretadorEV3.OUT_BC);
        else ev3.Float(InterpretadorEV3.OUT_BC);
        emMovimento = false;
    }

    // Converte distancia linear (cm) em graus de rotacao da roda
    // graus = distancia / raio_roda * (180/pi)
    private int distanciaParaGraus(double distancia) {
        return (int) Math.round(distancia / WHEEL_RADIUS * (180.0 / Math.PI));
    }

    // Converte arco (raio em cm, angulo em graus) em graus de rotacao da roda
    // arc = raio * toRadians(angulo)  |  graus = arc / raio_roda * (180/pi)
    // simplifica para: raio * angulo / raio_roda  (os pi cancelam-se)
    private int arcParaGrausRoda(double raio, double angulo) {
        return (int) Math.round(raio * angulo / WHEEL_RADIUS);
    }

    // Espera ate a roda rodar o numero de graus alvo (abordagem do slide com RotationCount),
    // ou ate ser pedida uma paragem (botao Parar) - o que ocorrer primeiro.
    private void esperarRotacao(int port, int grausAlvo) {
        int id = ev3.RotationCount(port);
        while (!pedidoParar && Math.abs(ev3.RotationCount(port) - id) < grausAlvo) {
            dormir(10);
        }
    }

    private boolean verificar() {
        if (!ligado) {
            log("[EV3] ERRO: robot não está ligado");
            return false;
        }
        return true;
    }

    private void log(String msg) {
        System.out.println(msg);
        if (guiLog != null) guiLog.accept(msg);
    }

    private static void dormir(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
