package fr_tp1;

import java.util.function.Consumer;

/**
 * Simulador de terminal do robot EV3.
 *
 * Sistema de coordenadas (conforme slides do professor):
 *   - Xi: eixo vertical (para cima) — direção inicial do robot
 *   - Yi: eixo horizontal (para a esquerda)
 *   - phi=0   -> robot aponta para Xi (cima)
 *   - phi=90  -> robot aponta para Yi (esquerda)
 *   - curveLeft  -> phi aumenta (anti-horario)
 *   - curveRight -> phi diminui (horario)
 */
public class SimuladorRobot implements IRobot {

    private final Consumer<String> logger;
    private boolean ligado = false;
    private String nome = "";
    private int velocidade = 40;

    public SimuladorRobot(Consumer<String> logger) {
        this.logger = logger;
    }

    // Estado cinematico - coordenadas do professor (Xi, Yi, phi)
    private double xi = 0.0;
    private double yi = 0.0;
    private double phi = 0.0; // graus: 0=Xi(cima), 90=Yi(esquerda) - phi em graus

    @Override
    public boolean ligar(String nome) {
        this.nome = nome;
        this.ligado = true;
        xi = 0; yi = 0; phi = 0;
        log("Ligado ao robot: " + nome);
        log("Posicao inicial: Xi=0.00  Yi=0.00  phi=0.00 graus  (a apontar para Xi)");
        return true;
    }

    @Override
    public void desligar() {
        ligado = false;
        log("Desligado do robot: " + nome);
    }

    @Override
    public void reta(double distancia) {
        if (!verificar()) return;
        double rad = Math.toRadians(phi);
        xi += distancia * Math.cos(rad);
        yi += distancia * Math.sin(rad);
        log("straight(" + fmt(distancia) + ")  →  " + pos());
    }

    @Override
    public void recuar(double distancia) {
        if (!verificar()) return;
        double rad = Math.toRadians(phi);
        xi -= distancia * Math.cos(rad);
        yi -= distancia * Math.sin(rad);
        log("Recuar(" + fmt(distancia) + ")   →  " + pos());
    }

    @Override
    public void curvarEsquerda(double raio, double angulo) {
        if (!verificar()) return;
        double phiRad = Math.toRadians(phi);
        double anguloRad = Math.toRadians(angulo);
        xi += raio * (Math.sin(phiRad + anguloRad) - Math.sin(phiRad));
        yi += raio * (Math.cos(phiRad) - Math.cos(phiRad + anguloRad));
        phi = normalizarPhi(phi + angulo);
        log("curveLeft(" + fmt(raio) + ", " + fmt(angulo) + ")  →  " + pos());
    }

    @Override
    public void curvarDireita(double raio, double angulo) {
        if (!verificar()) return;
        double phiRad = Math.toRadians(phi);
        double anguloRad = Math.toRadians(angulo);
        // Centro do arco: perpendicular à direita do robot
        xi += raio * (Math.sin(phiRad) - Math.sin(phiRad - anguloRad));
        yi += raio * (Math.cos(phiRad - anguloRad) - Math.cos(phiRad));
        phi = normalizarPhi(phi - angulo);
        log("curveRight(" + fmt(raio) + ", " + fmt(angulo) + ")  →  " + pos());
    }

    @Override
    public void parar(boolean travar) {
        if (!verificar()) return;
        log("Parar" + (travar ? " (travagem)" : " (livre)") + "  →  " + pos());
    }

    @Override
    public void setVelocidade(int vel) {
        this.velocidade = vel;
        log("Velocidade: " + vel + "%");
    }

    @Override
    public boolean isLigado() {
        return ligado;
    }

    private String pos() {
        return "Xi=" + fmt(xi) + "  Yi=" + fmt(yi) + "  phi=" + fmt(phi) + " graus";
    }

    private boolean verificar() {
        if (!ligado) {
            log("[ERRO] Robot não está ligado");
            return false;
        }
        return true;
    }

    // Normaliza phi para (-180, 180] - igual a convencao dos slides do professor
    private static double normalizarPhi(double p) {
        p = ((p % 360) + 360) % 360; // primeiro para [0, 360)
        return p > 180 ? p - 360 : p; // depois para (-180, 180]
    }

    private static String fmt(double v) {
        return String.format("%.2f", v);
    }

    private void log(String msg) {
        logger.accept("[SIMULADOR] " + msg);
    }
}
