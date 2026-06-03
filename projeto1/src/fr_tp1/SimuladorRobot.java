package fr_tp1;

/**
 * Simulador de terminal do robot EV3.
 * Rastreia a posição e rumo do robot e imprime cada ação no terminal.
 * Usa coordenadas cartesianas (x=Este, y=Norte) e rumo em graus (0=Este, 90=Norte).
 */
public class SimuladorRobot implements IRobot {

    private boolean ligado = false;
    private String nome = "";
    private int velocidade = 40;

    // Estado cinemático do robot
    private double x = 0.0;
    private double y = 0.0;
    private double rumo = 0.0; // graus, 0=Este, 90=Norte

    @Override
    public boolean ligar(String nome) {
        this.nome = nome;
        this.ligado = true;
        x = 0; y = 0; rumo = 0;
        log("Ligado ao robot: " + nome);
        log("Posição inicial: (0.00, 0.00) rumo: 0.00°");
        return true;
    }

    @Override
    public void desligar() {
        ligado = false;
        log("Desligado do robot: " + nome);
    }

    @Override
    public void reta(int distancia) {
        if (!verificar()) return;
        double rad = Math.toRadians(rumo);
        x += distancia * Math.cos(rad);
        y += distancia * Math.sin(rad);
        log("Reta " + distancia + " cm  →  pos: (" + fmt(x) + ", " + fmt(y) + ")  rumo: " + fmt(rumo) + "°");
    }

    @Override
    public void recuar(int distancia) {
        if (!verificar()) return;
        double rad = Math.toRadians(rumo);
        x -= distancia * Math.cos(rad);
        y -= distancia * Math.sin(rad);
        log("Recuar " + distancia + " cm  →  pos: (" + fmt(x) + ", " + fmt(y) + ")  rumo: " + fmt(rumo) + "°");
    }

    @Override
    public void curvarEsquerda(double raio, int angulo) {
        if (!verificar()) return;
        double θ = Math.toRadians(rumo);
        double α = Math.toRadians(angulo);
        // Centro do arco à esquerda do robot
        x += raio * (Math.sin(θ + α) - Math.sin(θ));
        y += raio * (Math.cos(θ) - Math.cos(θ + α));
        rumo += angulo;
        log("Curvar esquerda raio=" + fmt(raio) + " ângulo=" + angulo + "°  →  pos: (" + fmt(x) + ", " + fmt(y) + ")  rumo: " + fmt(rumo) + "°");
    }

    @Override
    public void curvarDireita(double raio, int angulo) {
        if (!verificar()) return;
        double θ = Math.toRadians(rumo);
        double α = Math.toRadians(angulo);
        // Centro do arco à direita do robot
        x += raio * (Math.sin(θ) - Math.sin(θ - α));
        y += raio * (Math.cos(θ - α) - Math.cos(θ));
        rumo -= angulo;
        log("Curvar direita  raio=" + fmt(raio) + " ângulo=" + angulo + "°  →  pos: (" + fmt(x) + ", " + fmt(y) + ")  rumo: " + fmt(rumo) + "°");
    }

    @Override
    public void parar(boolean travar) {
        if (!verificar()) return;
        log("Parar" + (travar ? " (travagem)" : " (livre)") + "  →  pos: (" + fmt(x) + ", " + fmt(y) + ")");
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

    private boolean verificar() {
        if (!ligado) {
            log("[ERRO] Robot não está ligado");
            return false;
        }
        return true;
    }

    private static String fmt(double v) {
        return String.format("%.2f", v);
    }

    private static void log(String msg) {
        System.out.println("[SIMULADOR] " + msg);
    }
}
