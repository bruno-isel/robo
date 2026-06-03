package fr_tp1;

import interpretador.InterpretadorEV3;

/**
 * Implementação real do robot EV3 usando InterpretadorEV3.
 *
 * Parâmetros físicos do robot (configuração padrão EV3):
 *   - Diâmetro das rodas: 5.6 cm  →  circunferência ≈ 17.59 cm
 *   - Distância entre rodas (dbw): 9.5 cm
 *   - Motor esquerdo: porta B  /  Motor direito: porta C
 *   - Velocidade base: 40% (≈ 14 cm/s)
 */
public class myRobotLego implements IRobot {

    private static final double WHEEL_DIAM   = 5.6;
    private static final double DBW          = 9.5;
    private static final double WHEEL_CIRC   = Math.PI * WHEEL_DIAM;
    private static final double MAX_CM_PER_S = 720.0 * WHEEL_CIRC / 360.0;

    private final InterpretadorEV3 ev3;
    private boolean ligado = false;
    private int velocidade = 40;

    public myRobotLego() {
        ev3 = new InterpretadorEV3();
    }

    @Override
    public boolean ligar(String nome) {
        ligado = ev3.OpenEV3(nome);
        if (ligado) {
            System.out.println("[EV3] Ligado a: " + nome);
            ev3.ResetAll();
        } else {
            System.out.println("[EV3] Falha na ligação a: " + nome);
        }
        return ligado;
    }

    @Override
    public void desligar() {
        if (ligado) {
            ev3.Off(InterpretadorEV3.OUT_BC);
            ev3.CloseEV3();
            ligado = false;
            System.out.println("[EV3] Desligado");
        }
    }

    @Override
    public void reta(double distancia) {
        if (!verificar()) return;
        long ms = tempoMs(distancia, velocidade);
        System.out.println("[EV3] straight(" + distancia + ") → " + ms + " ms");
        ev3.OnFwd(InterpretadorEV3.OUT_BC, velocidade);
        dormir(ms);
        ev3.Off(InterpretadorEV3.OUT_BC);
    }

    @Override
    public void recuar(double distancia) {
        if (!verificar()) return;
        long ms = tempoMs(distancia, velocidade);
        System.out.println("[EV3] Recuar(" + distancia + ") → " + ms + " ms");
        ev3.OnRev(InterpretadorEV3.OUT_BC, velocidade);
        dormir(ms);
        ev3.Off(InterpretadorEV3.OUT_BC);
    }

    @Override
    public void curvarEsquerda(double raio, double angulo) {
        if (!verificar()) return;
        double raioExt = raio + DBW / 2;
        double raioInt = raio - DBW / 2;
        int velExt = velocidade;
        int velInt = (int) Math.round(velocidade * raioInt / raioExt);
        long ms = tempoMs(raioExt * Math.toRadians(angulo), velExt);
        System.out.println("[EV3] curveLeft(" + raio + ", " + angulo + ") → " + ms + " ms");
        acionarCurva(InterpretadorEV3.OUT_C, velExt, InterpretadorEV3.OUT_B, velInt, ms);
    }

    @Override
    public void curvarDireita(double raio, double angulo) {
        if (!verificar()) return;
        double raioExt = raio + DBW / 2;
        double raioInt = raio - DBW / 2;
        int velExt = velocidade;
        int velInt = (int) Math.round(velocidade * raioInt / raioExt);
        long ms = tempoMs(raioExt * Math.toRadians(angulo), velExt);
        System.out.println("[EV3] curveRight(" + raio + ", " + angulo + ") → " + ms + " ms");
        acionarCurva(InterpretadorEV3.OUT_B, velExt, InterpretadorEV3.OUT_C, velInt, ms);
    }

    @Override
    public void parar(boolean travar) {
        if (!verificar()) return;
        System.out.println("[EV3] Parar");
        if (travar)
            ev3.Off(InterpretadorEV3.OUT_BC);
        else
            ev3.Float(InterpretadorEV3.OUT_BC);
    }

    @Override
    public void setVelocidade(int vel) {
        this.velocidade = Math.max(20, Math.min(80, vel));
        System.out.println("[EV3] Velocidade: " + this.velocidade + "%");
    }

    @Override
    public boolean isLigado() {
        return ligado;
    }

    private void acionarCurva(int motorExt, int velExt, int motorInt, int velInt, long ms) {
        ev3.OnFwd(motorExt, velExt);
        if (velInt > 0)
            ev3.OnFwd(motorInt, velInt);
        else if (velInt < 0)
            ev3.OnRev(motorInt, -velInt);
        else
            ev3.Off(motorInt);
        dormir(ms);
        ev3.Off(InterpretadorEV3.OUT_BC);
    }

    private long tempoMs(double distancia, int vel) {
        double cmPerSec = (vel / 100.0) * MAX_CM_PER_S;
        return (long) (distancia / cmPerSec * 1000);
    }

    private boolean verificar() {
        if (!ligado) {
            System.out.println("[EV3] ERRO: robot não está ligado");
            return false;
        }
        return true;
    }

    private static void dormir(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
