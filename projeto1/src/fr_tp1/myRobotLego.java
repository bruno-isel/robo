package fr_tp1;

import interpretador.InterpretadorEV3;
import java.util.function.Consumer;

/**
 * Implementacao real do robot EV3 usando InterpretadorEV3.
 *
 * Parametros fisicos do robot (conforme slide 01, pag. 9: rr=27.3mm, der=90mm):
 *   - Raio das rodas: 2.73 cm
 *   - Distancia entre rodas (DBW): 9.0 cm
 *   - Motor esquerdo: porta B  /  Motor direito: porta C
 *   - Velocidade base: 40%
 */
public class myRobotLego implements IRobot {

    private static final double WHEEL_RADIUS = 2.73;
    private static final double DBW          = 9.0;

    private final InterpretadorEV3 ev3;
    private final Consumer<String> guiLog;
    private boolean ligado = false;
    private int velocidade = 40;

    public myRobotLego(Consumer<String> guiLog) {
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
            log("[EV3] Falha na ligacao a: " + nome);
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
        int graus = distanciaParaGraus(distancia);
        log("[EV3] straight(" + distancia + " cm) -> " + graus + " graus de roda");
        ev3.OnFwd(InterpretadorEV3.OUT_B, velocidade, InterpretadorEV3.OUT_C, velocidade);
        esperarRotacao(InterpretadorEV3.OUT_B, graus);
        ev3.Off(InterpretadorEV3.OUT_BC);
    }

    @Override
    public void recuar(double distancia) {
        if (!verificar()) return;
        int graus = distanciaParaGraus(distancia);
        log("[EV3] Recuar(" + distancia + " cm) -> " + graus + " graus de roda");
        ev3.OnRev(InterpretadorEV3.OUT_B, velocidade, InterpretadorEV3.OUT_C, velocidade);
        esperarRotacao(InterpretadorEV3.OUT_B, graus);
        ev3.Off(InterpretadorEV3.OUT_BC);
    }

    @Override
    public void curvarEsquerda(double raio, double angulo) {
        if (!verificar()) return;
        // curva esquerda: roda direita (OUT_C) e exterior
        double raioExt = raio + DBW / 2;
        double raioInt = raio - DBW / 2;
        int velExt = velocidade;
        int velInt = (int) Math.round(velocidade * raioInt / raioExt);
        int graus = arcParaGrausRoda(raioExt, angulo);
        log("[EV3] curveLeft(" + raio + ", " + angulo + ") -> " + graus + " graus roda ext");
        acionarCurva(InterpretadorEV3.OUT_C, velExt, InterpretadorEV3.OUT_B, velInt, graus);
    }

    @Override
    public void curvarDireita(double raio, double angulo) {
        if (!verificar()) return;
        // curva direita: roda esquerda (OUT_B) e exterior
        double raioExt = raio + DBW / 2;
        double raioInt = raio - DBW / 2;
        int velExt = velocidade;
        int velInt = (int) Math.round(velocidade * raioInt / raioExt);
        int graus = arcParaGrausRoda(raioExt, angulo);
        log("[EV3] curveRight(" + raio + ", " + angulo + ") -> " + graus + " graus roda ext");
        acionarCurva(InterpretadorEV3.OUT_B, velExt, InterpretadorEV3.OUT_C, velInt, graus);
    }

    @Override
    public void parar(boolean travar) {
        if (!verificar()) return;
        log("[EV3] Parar");
        if (travar)
            ev3.Off(InterpretadorEV3.OUT_BC);
        else
            ev3.Float(InterpretadorEV3.OUT_BC);
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
        ev3.OnFwd(motorExt, velExt);
        if (velInt > 0)
            ev3.OnFwd(motorInt, velInt);
        else if (velInt < 0)
            ev3.OnRev(motorInt, -velInt);
        else
            ev3.Off(motorInt);
        esperarRotacao(motorExt, graus);
        ev3.Off(InterpretadorEV3.OUT_BC);
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

    // Espera ate a roda rodar o numero de graus alvo (abordagem do slide com RotationCount)
    private void esperarRotacao(int port, int grausAlvo) {
        int id = ev3.RotationCount(port);
        while (Math.abs(ev3.RotationCount(port) - id) < grausAlvo) {
            dormir(10);
        }
    }

    private boolean verificar() {
        if (!ligado) {
            log("[EV3] ERRO: robot nao esta ligado");
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
