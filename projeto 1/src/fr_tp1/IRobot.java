package fr_tp1;

public interface IRobot {
    boolean ligar(String nome);
    void desligar();
    void reta(int distancia);
    void curvarEsquerda(double raio, int angulo);
    void curvarDireita(double raio, int angulo);
    void parar(boolean travar);
    void setVelocidade(int vel);
    boolean isLigado();
}
