package fr_tp2;

public interface IRobot {
    boolean ligar(String nome);
    void desligar();
    void reta(double distancia);
    void recuar(double distancia);
    void curvarEsquerda(double raio, double angulo);
    void curvarDireita(double raio, double angulo);
    void parar(boolean travar);
    void setVelocidade(int vel);
    boolean isLigado();
}
