package fr_tp1;

/**
 * Simulador de terminal do robot EV3.
 *
 * Sistema de coordenadas (conforme slides do professor):
 *   - Xi: eixo vertical (para cima) — direção inicial do robot
 *   - Yi: eixo horizontal (para a esquerda)
 *   - φ=0°  → robot aponta para Xi (cima)
 *   - φ=90° → robot aponta para Yi (esquerda)
 *   - curveLeft  → φ aumenta (anti-horário)
 *   - curveRight → φ diminui (horário)
 */
public class SimuladorRobot implements IRobot {

    private boolean ligado = false;
    private String nome = "";
    private int velocidade = 40;

    // Estado cinemático — coordenadas do professor (Xi, Yi, φ)
    private double xi = 0.0;
    private double yi = 0.0;
    private double phi = 0.0; // graus: 0=Xi(cima), 90=Yi(esquerda)

    @Override
    public boolean ligar(String nome) {
        this.nome = nome;
        this.ligado = true;
        xi = 0; yi = 0; phi = 0;
        log("Ligado ao robot: " + nome);
        log("Posição inicial: Xi=0.00  Yi=0.00  φ=0.00°  (a apontar para Xi)");
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
        double θ = Math.toRadians(phi);
        double α = Math.toRadians(angulo);
        xi += raio * (Math.sin(θ + α) - Math.sin(θ));
        yi += raio * (Math.cos(θ) - Math.cos(θ + α));
        phi = normalizarPhi(phi + angulo);
        log("curveLeft(" + fmt(raio) + ", " + fmt(angulo) + ")  →  " + pos());
    }

    @Override
    public void curvarDireita(double raio, double angulo) {
        if (!verificar()) return;
        double θ = Math.toRadians(phi);
        double α = Math.toRadians(angulo);
        // Centro do arco: perpendicular à direita do robot
        // c = (xi + r*sin(θ), yi - r*cos(θ))
        xi += raio * (Math.sin(θ) - Math.sin(θ - α));
        yi += raio * (Math.cos(θ - α) - Math.cos(θ));
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
        return "Xi=" + fmt(xi) + "  Yi=" + fmt(yi) + "  φ=" + fmt(phi) + "°";
    }

    private boolean verificar() {
        if (!ligado) {
            log("[ERRO] Robot não está ligado");
            return false;
        }
        return true;
    }

    // Normaliza φ para (-180°, 180°] — igual à convenção dos slides do professor
    private static double normalizarPhi(double p) {
        p = ((p % 360) + 360) % 360; // primeiro para [0°, 360°)
        return p > 180 ? p - 360 : p; // depois para (-180°, 180°]
    }

    private static String fmt(double v) {
        return String.format("%.2f", v);
    }

    private static void log(String msg) {
        System.out.println("[SIMULADOR] " + msg);
    }
}
