package fr_tp2v2;

import interpretador.InterpretadorEV3;
import java.util.function.Consumer;

/**
 * Implementação real do robot seguidor de parede via InterpretadorEV3.
 *
 * Parâmetros físicos (mesmos do fr_tp2.myRobotLego):
 *   - Diâmetro das rodas: 5.6 cm (raio 2.8 cm)
 *   - Distância entre rodas (dbw): 9.5 cm
 *   - Motor esquerdo: porta B  /  Motor direito: porta C
 *   - Sonar: porta S2  /  Sensor de toque: porta S1
 */
public class myRobotParede implements IRobotParede {

    private static final double WHEEL_DIAM   = 5.6;
    private static final double WHEEL_RADIUS = WHEEL_DIAM / 2;
    private static final double DBW          = 9.5;
    private static final int    VELOCIDADE   = 40;

    private final InterpretadorEV3 ev3;
    private final Consumer<String> guiLog;
    private boolean ligado = false;

    // Usados para interromper a manobra de recuperacao (recuar/curvarEsquerda)
    // a partir do botao Parar, sem chamar ev3 a partir de duas threads ao
    // mesmo tempo (exclusao mutua do InterpretadorEV3) - mesmo padrao do
    // fr_tp2.myRobotLego.
    private volatile boolean pedidoParar = false;
    private volatile boolean emMovimento = false;

    public myRobotParede(Consumer<String> guiLog) {
        this.guiLog = guiLog;
        ev3 = new InterpretadorEV3();
    }

    @Override
    public boolean ligar(String nome) {
        ligado = ev3.OpenEV3(nome);
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
    public boolean isLigado() {
        return ligado;
    }

    @Override
    public void andarDiferencial(int velEsquerda, int velDireita) {
        if (!verificar()) return;
        // Assume sempre as duas rodas para a frente (velocidades >= 0) - e o
        // unico caso usado pela SeguidorParedeLogica. A chamada combinada de
        // 4 argumentos e a unica forma confirmada fiavel nesta biblioteca
        // (ver ERROS_E_CORRECOES_TP2.md, ponto 5).
        ev3.OnFwd(InterpretadorEV3.OUT_B, velEsquerda, InterpretadorEV3.OUT_C, velDireita);
    }

    @Override
    public void recuar(double distanciaCm) {
        if (!verificar()) return;
        iniciarMovimento();
        int graus = distanciaParaGraus(distanciaCm);
        log(String.format("[EV3] recuar(%.2f cm) → %d graus de roda", distanciaCm, graus));
        ev3.OnRev(InterpretadorEV3.OUT_B, VELOCIDADE, InterpretadorEV3.OUT_C, VELOCIDADE);
        esperarRotacao(InterpretadorEV3.OUT_B, graus);
        terminarMovimento();
    }

    @Override
    public void curvarEsquerda(double raioCm, double anguloGraus) {
        if (!verificar()) return;
        iniciarMovimento();
        // curva esquerda: roda direita (OUT_C) e exterior
        double raioExt = raioCm + DBW / 2;
        double raioInt = raioCm - DBW / 2;
        int velExt = VELOCIDADE;
        int velInt = (int) Math.round(velExt * raioInt / raioExt);
        int graus = arcParaGrausRoda(raioExt, anguloGraus);
        log(String.format("[EV3] curvarEsquerda(%.2f, %.2f) → %d graus roda ext", raioCm, anguloGraus, graus));
        if (velInt >= 0) {
            ev3.OnFwd(InterpretadorEV3.OUT_C, velExt, InterpretadorEV3.OUT_B, velInt);
        } else {
            ev3.OnFwd(InterpretadorEV3.OUT_C, velExt);
            ev3.OnRev(InterpretadorEV3.OUT_B, -velInt);
        }
        esperarRotacao(InterpretadorEV3.OUT_C, graus);
        terminarMovimento();
    }

    @Override
    public void parar() {
        if (!verificar()) return;
        log("[EV3] Parar");
        pedidoParar = true;
        if (!emMovimento) {
            ev3.Off(InterpretadorEV3.OUT_BC);
        }
    }

    @Override
    public int lerDistanciaSonar() {
        return ev3.SensorUS(InterpretadorEV3.S_2);
    }

    @Override
    public boolean tocouObstaculo() {
        return ev3.SensorTouch(InterpretadorEV3.S_1) != 0;
    }

    // Marca inicio de um movimento pontual bloqueante (recuar/curvarEsquerda).
    private void iniciarMovimento() {
        pedidoParar = false;
        emMovimento = true;
    }

    // Fim natural ou interrompido do movimento: para sempre com travagem.
    private void terminarMovimento() {
        ev3.Off(InterpretadorEV3.OUT_BC);
        emMovimento = false;
    }

    // Converte distancia linear (cm) em graus de rotacao da roda
    private int distanciaParaGraus(double distancia) {
        return (int) Math.round(distancia / WHEEL_RADIUS * (180.0 / Math.PI));
    }

    // Converte arco (raio em cm, angulo em graus) em graus de rotacao da roda
    private int arcParaGrausRoda(double raio, double angulo) {
        return (int) Math.round(raio * angulo / WHEEL_RADIUS);
    }

    // Espera ate a roda rodar o numero de graus alvo, ou ate ser pedida uma
    // paragem (botao Parar) - o que ocorrer primeiro.
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
