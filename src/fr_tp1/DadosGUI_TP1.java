package fr_tp1;

import java.util.ArrayList;

public class DadosGUI_TP1 {
	
	private boolean onOff,debug;
	private String robotName;
	private int raio,angulo,distancia;
	private ArrayList<String> consola;	

	public DadosGUI_TP1() {

        onOff = false;
        debug = true ;
        robotName = "Bruno";
        raio= 20;
        angulo= 50;
        distancia = 50;
        consola= new ArrayList<String>();
    }

	
	
	public boolean isOnOff() {
		return onOff;
	}
	public void setOnOff(boolean onOff) {
		this.onOff = onOff;
	}
	public boolean isDebug() {
		return debug;
	}
	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	public String getRobotName() {
		return robotName;
	}
	public void setRobotName(String robotName) {
		this.robotName = robotName;
	}
	public int getRaio() {
		return raio;
	}
	public void setRaio(int raio) {
		this.raio = raio;
	}
	public int getAngulo() {
		return angulo;
	}
	public void setAngulo(int angulo) {
		this.angulo = angulo;
	}
	public int getDistancia() {
		return distancia;
	}
	public void setDistancia(int distancia) {
		this.distancia = distancia;
	}
	public ArrayList<String> getConsola() {
		return consola;
	}
	public void setConsola(ArrayList<String> consola) {
		this.consola = consola;
	}
	
}
