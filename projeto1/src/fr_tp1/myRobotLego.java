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

    // Usados para interromper um movimento em curso a partir do botao Parar,
    // sem chamar ev3 a partir de duas threads ao mesmo tempo (exclusao mutua
    // do InterpretadorEV3): quem chama ev3.Off/Float e sempre a thread do
    // movimento em curso; parar() so define as flags quando ha movimento ativo.
    private volatile boolean pedidoParar = false;
    private volatile boolean travarAoParar = true;
    private volatile boolean emMovimento = false;

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
        iniciarMovimento();
        int graus = distanciaParaGraus(distancia);
        log("[EV3] straight(" + distancia + " cm) -> " + graus + " graus de roda");
        ev3.OnFwd(InterpretadorEV3.OUT_B, velocidade, InterpretadorEV3.OUT_C, velocidade);
        esperarRotacao(InterpretadorEV3.OUT_B, graus);
        terminarMovimento();
    }

    @Override
    public void recuar(double distancia) {
        if (!verificar()) return;
        iniciarMovimento();
        int graus = distanciaParaGraus(distancia);
        log("[EV3] Recuar(" + distancia + " cm) -> " + graus + " graus de roda");
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
        log("[EV3] curveLeft(" + raio + ", " + angulo + ") -> " + graus + " graus roda ext");
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
        log("[EV3] curveRight(" + raio + ", " + angulo + ") -> " + graus + " graus roda ext");
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
