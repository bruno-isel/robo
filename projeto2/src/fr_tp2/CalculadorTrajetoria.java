package fr_tp2;

import java.util.ArrayList;
import java.util.List;

/**
 * Calcula automaticamente a trajetória para um ponto objetivo (xf, yf, φf).
 *
 * Sistema de coordenadas (conforme slides do professor):
 *   Xi = eixo vertical (cima) — direção inicial do robot
 *   Yi = eixo horizontal (esquerda)
 *   φ  = ângulo a partir de Xi, positivo para a esquerda
 *
 * Três tipos de trajetória (todos começam com curva para a esquerda):
 *   T1: curvaEsq(r, α1) + reta(d) + curvaEsq(r, α2)   [φf > 0, dois arcos esq]
 *   T2: curvaEsq(r, α)  + curvaDireita(r, α−φf)        [yc2 < yc1]
 *   T3: curvaEsq(r, α)  + curvaDireita(r, α−φf)        [yc2 ≥ yc1, φf negativo]
 *
 * Parâmetros físicos do robot:
 *   DBW = 9.5 cm, V_ROBOT = 40%, V_MIN = 20%, V_MAX = 80%
 */
public class CalculadorTrajetoria {

    public static final double DBW     = 9.5;
    public static final int    V_ROBOT = 40;
    public static final int    V_MIN   = 20;
    public static final int    V_MAX   = 80;

    // ── tipos de passo ─────────────────────────────────────────────────────
    public enum TipoMovimento { CURVA_ESQ, CURVA_DIR, RETA }

    public static class Passo {
        public final TipoMovimento tipo;
        public final double raio;
        public final double angulo;
        public final double distancia;

        Passo(TipoMovimento tipo, double raio, double angulo, double distancia) {
            this.tipo      = tipo;
            this.raio      = raio;
            this.angulo    = angulo;
            this.distancia = distancia;
        }

        @Override
        public String toString() {
            switch (tipo) {
                case CURVA_ESQ: return String.format("curvaEsquerda(%.2f cm, %.2f°)", raio, angulo);
                case CURVA_DIR: return String.format("curvaDireita(%.2f cm, %.2f°)",  raio, angulo);
                case RETA:      return String.format("reta(%.2f cm)", distancia);
                default: return "?";
            }
        }
    }

    public static class Resultado {
        public final int         trajetoria; // 1, 2 ou 3
        public final double      raio;
        public final List<Passo> passos;

        Resultado(int trajetoria, double raio, List<Passo> passos) {
            this.trajetoria = trajetoria;
            this.raio       = raio;
            this.passos     = passos;
        }

        /** Texto multi-linha para apresentar na consola da GUI. */
        public String toConsola() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("── Trajetória %d  (r=%.2f cm) ──%n", trajetoria, raio));
            for (int i = 0; i < passos.size(); i++)
                sb.append(String.format("  Passo %d: %s%n", i + 1, passos.get(i)));
            return sb.toString();
        }
    }

    // ── API pública ─────────────────────────────────────────────────────────

    /**
     * Calcula a trajetória para (xf, yf, phiGraus).
     * Tenta T1 primeiro; se não for válida, usa T2 ou T3.
     * Retorna null se o ponto não for atingível.
     */
    public static Resultado calcular(double xf, double yf, double phiGraus) {
        if (xf <= 0) return null; // ponto atrás do robot

        Resultado r1 = tentarT1(xf, yf, phiGraus);
        if (r1 != null) return r1;

        return calcularT2T3(xf, yf, phiGraus);
    }

    // ── Trajetória 1: curvaEsq + reta + curvaEsq ────────────────────────────

    private static Resultado tentarT1(double xf, double yf, double phiGraus) {
        if (phiGraus <= 0) return null; // T1 requer φf positivo

        double phi  = Math.toRadians(phiGraus);
        double cosP = Math.cos(phi);
        double sinP = Math.sin(phi);

        // coeficientes da fórmula resolvente (raio teórico)
        double a = 2 + 2 * cosP;
        double b = 2 * yf * (1 - cosP) + 2 * xf * sinP;
        double c = -(xf * xf + yf * yf);

        double disc = b * b - 4 * a * c;
        if (disc < 0) return null;
        double rT = (-b + Math.sqrt(disc)) / (2 * a);
        if (rT <= DBW / 2 + 1e-6) return null; // não atingível fisicamente

        double rP = calcRaioPratico(rT);

        // centros dos dois arcos com raio prático
        double xc2 = xf - rP * sinP;
        double yc2 = yf + rP * cosP;
        double yc1 = rP; // c1 = (0, rP)

        double dx  = xc2;
        double dy  = yc2 - yc1;
        double d12 = Math.sqrt(dx * dx + dy * dy);
        if (d12 < 1e-9) return null;

        double cosA1 = Math.max(-1.0, Math.min(1.0, xc2 / d12));
        double alpha1 = Math.toDegrees(Math.acos(cosA1));
        double alpha2 = phiGraus - alpha1;

        if (alpha1 < 1e-3 || alpha2 < 1e-3) return null;

        List<Passo> passos = new ArrayList<>();
        passos.add(new Passo(TipoMovimento.CURVA_ESQ, rP, alpha1, 0));
        passos.add(new Passo(TipoMovimento.RETA,      0,  0,      d12));
        passos.add(new Passo(TipoMovimento.CURVA_ESQ, rP, alpha2, 0));

        return new Resultado(1, rP, passos);
    }

    // ── Trajetórias 2 e 3: curvaEsq + curvaDireita ──────────────────────────

    private static Resultado calcularT2T3(double xf, double yf, double phiGraus) {
        double phi  = Math.toRadians(phiGraus);
        double cosP = Math.cos(phi);
        double sinP = Math.sin(phi);

        double a = 2 - 2 * cosP;
        double c = -(xf * xf + yf * yf);
        double r;

        if (Math.abs(a) < 1e-9) {
            // φf ≈ 0°: equação linear  b·r = −c
            double bLin = 4 * yf;
            if (Math.abs(bLin) < 1e-9) return null;
            r = -c / bLin;
        } else {
            double b    = 2 * yf * (1 + cosP) - 2 * xf * sinP;
            double disc = b * b - 4 * a * c;
            if (disc < 0) return null;
            r = (-b + Math.sqrt(disc)) / (2 * a);
        }
        if (r <= 0) return null;

        // centro do arco final (curva direita), usando r teórico
        double xc2 = xf + r * sinP;
        double yc2 = yf - r * cosP;
        double yc1 = r; // c1 = (0, r)

        double sinAlpha = Math.max(-1.0, Math.min(1.0, xc2 / (2 * r)));
        double baseAlpha = Math.toDegrees(Math.asin(sinAlpha));

        double alpha;
        int    traj;
        if (yc2 < yc1) {
            // T2: yc2 < yc1
            alpha = baseAlpha;
            traj  = 2;
        } else {
            // T3: yc2 >= yc1 (φf negativo)
            alpha = 180.0 - baseAlpha;
            traj  = 3;
        }

        double alphaDireita = alpha - phiGraus;
        if (alphaDireita < 1e-3) return null;

        List<Passo> passos = new ArrayList<>();
        passos.add(new Passo(TipoMovimento.CURVA_ESQ, r, alpha,        0));
        passos.add(new Passo(TipoMovimento.CURVA_DIR, r, alphaDireita, 0));

        return new Resultado(traj, r, passos);
    }

    // ── Raio teórico → raio prático ─────────────────────────────────────────

    /**
     * Converte raio teórico em raio prático (slide 04 — Trajetória 1 prática).
     *   f      = (rT + dbw/2) / (rT − dbw/2)
     *   vSlow  = floor(2·vRobot / (f+1))
     *   vFast  = 2·vRobot − vSlow
     *   fP     = vFast / vSlow
     *   rP     = (dbw/2)·(fP+1)/(fP−1)
     */
    static double calcRaioPratico(double rT) {
        if (rT <= DBW / 2) return rT;
        double f     = (rT + DBW / 2) / (rT - DBW / 2);
        int    vSlow = (int) Math.floor(2.0 * V_ROBOT / (f + 1));
        vSlow = Math.max(V_MIN, Math.min(V_MAX - 1, vSlow));
        int    vFast = 2 * V_ROBOT - vSlow;
        if (vSlow == 0) return rT;
        double fP = (double) vFast / vSlow;
        if (fP <= 1.0) return rT;
        return (DBW / 2) * (fP + 1) / (fP - 1);
    }
}
