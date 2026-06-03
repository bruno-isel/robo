package fr_tp1;

import java.util.ArrayList;

public class DadosGUI_TP1 {

    private boolean onOff, debug;
    private String robotName;
    private double raio, angulo, distancia;
    private ArrayList<String> consola;

    public DadosGUI_TP1() {
        onOff     = false;
        debug     = true;
        robotName = "Bruno";
        raio      = 20.0;
        angulo    = 50.0;
        distancia = 50.0;
        consola   = new ArrayList<>();
    }

    public boolean isOnOff()                    { return onOff; }
    public void setOnOff(boolean onOff)         { this.onOff = onOff; }
    public boolean isDebug()                    { return debug; }
    public void setDebug(boolean debug)         { this.debug = debug; }
    public String getRobotName()                { return robotName; }
    public void setRobotName(String n)          { this.robotName = n; }
    public double getRaio()                     { return raio; }
    public void setRaio(double raio)            { this.raio = raio; }
    public double getAngulo()                   { return angulo; }
    public void setAngulo(double angulo)        { this.angulo = angulo; }
    public double getDistancia()                { return distancia; }
    public void setDistancia(double distancia)  { this.distancia = distancia; }
    public ArrayList<String> getConsola()       { return consola; }
    public void setConsola(ArrayList<String> c) { this.consola = c; }
}
