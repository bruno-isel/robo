package fr_tp2v2;

/**
 * Lógica pura de decisão do seguimento de parede (sem estado, sem IO).
 *
 * Controlo bang-bang simples (sonar do lado direito do robot):
 *   - distância > DIST_MAX  → longe demais, aproxima-se da parede (curva à direita)
 *   - distância < DIST_MIN  → perto demais, afasta-se da parede (curva à esquerda)
 *   - caso contrário        → dentro da gama, segue reto
 */
public class SeguidorParedeLogica {

    public static final int DIST_MIN   = 40;
    public static final int DIST_MAX   = 60;
    public static final int DIST_IDEAL = 50;

    public static final int VEL_BASE  = 40;
    public static final int VEL_LENTA = 20;

    /** Raio (cm) da curva de 90º na manobra de recuperação - afinar no robot real. */
    public static final double RAIO_RECUPERACAO = 15.0;

    /** Distância (cm) a recuar na manobra de recuperação, conforme o guião. */
    public static final double DISTANCIA_RECUO = 70.0;

    public enum Decisao { APROXIMAR, AFASTAR, SEGUIR_RETO }

    public static Decisao decidir(int distanciaCm) {
        if (distanciaCm > DIST_MAX) return Decisao.APROXIMAR;
        if (distanciaCm < DIST_MIN) return Decisao.AFASTAR;
        return Decisao.SEGUIR_RETO;
    }

    /** Velocidade da roda esquerda para a decisão dada. */
    public static int velEsquerda(Decisao d) {
        return d == Decisao.AFASTAR ? VEL_LENTA : VEL_BASE;
    }

    /** Velocidade da roda direita para a decisão dada. */
    public static int velDireita(Decisao d) {
        return d == Decisao.APROXIMAR ? VEL_LENTA : VEL_BASE;
    }
}
