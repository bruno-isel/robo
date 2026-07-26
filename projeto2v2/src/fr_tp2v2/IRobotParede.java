package fr_tp2v2;

/**
 * Interface do robot seguidor de parede (TP2, Parte II).
 * Isolada de fr_tp2.IRobot de propósito: comportamento autónomo continuo,
 * não os comandos pontuais da Parte I.
 */
public interface IRobotParede {

    boolean ligar(String nome);
    void desligar();
    boolean isLigado();

    /** Comando contínuo, não bloqueia: define a velocidade de cada roda diretamente. */
    void andarDiferencial(int velEsquerda, int velDireita);

    /** Comandos pontuais, bloqueiam até terminar (usados na manobra de recuperação). */
    void recuar(double distanciaCm);
    void curvarEsquerda(double raioCm, double anguloGraus);
    void parar();

    /** Distância à parede lida pelo sonar, em cm. */
    int lerDistanciaSonar();

    /** true se o sensor de toque detetou um obstáculo. */
    boolean tocouObstaculo();
}
