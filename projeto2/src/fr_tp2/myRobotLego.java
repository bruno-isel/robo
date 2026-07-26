package fr_tp2;

import interpretador.InterpretadorEV3;
import java.util.function.Consumer;

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
    private final Consumer<String> guiLog;
    private boolean ligado = false;
    private int velocidade = 40;

    // Usados para interromper um movimento em curso a partir do botao Parar.
    // Ao contrario do TP1 (que faz polling a RotationCount), aqui o movimento
    // bloqueia num Thread.sleep(ms); por isso a paragem interrompe a propria
    // thread do movimento (Thread.interrupt), que e quem chama ev3.Off/Float -
    // nunca duas threads a falar com o ev3 ao mesmo tempo (exclusao mutua).
    private volatile Thread threadMovimento = null;
    private volatile boolean travarAoParar = true;

    public myRobotLego() {
        this(null);
    }

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
        long ms = tempoMs(distancia, velocidade);
        log(String.format("[EV3] straight(%.2f) → %d ms", distancia, ms));
        ev3.OnFwd(InterpretadorEV3.OUT_B, velocidade, InterpretadorEV3.OUT_C, velocidade);
        dormir(ms);
        terminarMovimento();
    }

    @Override
    public void recuar(double distancia) {
        if (!verificar()) return;
        iniciarMovimento();
        long ms = tempoMs(distancia, velocidade);
        log(String.format("[EV3] Recuar(%.2f) → %d ms", distancia, ms));
        ev3.OnRev(InterpretadorEV3.OUT_B, velocidade, InterpretadorEV3.OUT_C, velocidade);
        dormir(ms);
        terminarMovimento();
    }

    @Override
    public void curvarEsquerda(double raio, double angulo) {
        if (!verificar()) return;
        iniciarMovimento();
        double raioExt = raio + DBW / 2;
        double raioInt = raio - DBW / 2;
        int velExt = velocidade;
        int velInt = (int) Math.round(velocidade * raioInt / raioExt);
        long ms = tempoMs(raioExt * Math.toRadians(angulo), velExt);
        log(String.format("[EV3] curveLeft(%.2f, %.2f) → %d ms", raio, angulo, ms));
        acionarCurva(InterpretadorEV3.OUT_C, velExt, InterpretadorEV3.OUT_B, velInt, ms);
    }

    @Override
    public void curvarDireita(double raio, double angulo) {
        if (!verificar()) return;
        iniciarMovimento();
        double raioExt = raio + DBW / 2;
        double raioInt = raio - DBW / 2;
        int velExt = velocidade;
        int velInt = (int) Math.round(velocidade * raioInt / raioExt);
        long ms = tempoMs(raioExt * Math.toRadians(angulo), velExt);
        log(String.format("[EV3] curveRight(%.2f, %.2f) → %d ms", raio, angulo, ms));
        acionarCurva(InterpretadorEV3.OUT_B, velExt, InterpretadorEV3.OUT_C, velInt, ms);
    }

    @Override
    public void parar(boolean travar) {
        if (!verificar()) return;
        log("[EV3] Parar");
        Thread t = threadMovimento;
        if (t != null) {
            // Movimento em curso: so pede a interrupcao da thread que la esta;
            // e essa thread (em dormir/terminarMovimento) que fala com o ev3.
            travarAoParar = travar;
            t.interrupt();
        } else {
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

    // Marca inicio de um movimento: guarda a thread atual para poder ser
    // interrompida pelo Parar, e assume travagem por defeito no fim.
    private void iniciarMovimento() {
        travarAoParar = true;
        Thread.interrupted(); // limpa qualquer interrupcao pendente de um pedido anterior
        threadMovimento = Thread.currentThread();
    }

    // Fim natural ou interrompido do movimento: para os motores respeitando
    // o modo pedido por parar() (travar/Off ou deixar andar livre/Float).
    private void terminarMovimento() {
        if (travarAoParar) ev3.Off(InterpretadorEV3.OUT_BC);
        else ev3.Float(InterpretadorEV3.OUT_BC);
        threadMovimento = null;
    }

    private long tempoMs(double distancia, int vel) {
        double cmPerSec = (vel / 100.0) * MAX_CM_PER_S;
        return (long) (distancia / cmPerSec * 1000);
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

    // Nao repoe o interrupt status no catch: as threads das SwingWorker vem
    // de uma pool reutilizada, e deixar o status marcado "vazaria" para a
    // proxima tarefa (nao relacionada) que caia na mesma thread da pool.
    private static void dormir(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { /* pedido de paragem: sai mais cedo */ }
    }
}
