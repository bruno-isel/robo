package fr_tp1;

import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import java.awt.Font;
import javax.swing.JTextField;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.JButton;
import java.awt.Color;
import javax.swing.JCheckBox;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;

public class GUI_TP1 extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField textField_Angulo, textField_Distancia, textField_Raio, textField_Robot;
	private JCheckBox chckbxDebug_1;
	private DadosGUI_TP1 dados;

	JLabel Label_Robot, label_Consola_1, label_Angulo, Label_Raio, label_Distancia;
	JScrollPane scrollPane;
	JRadioButton rdbtnOnoff;
	JButton JButtton_Esquerda, JButtton_Parar, JButtton_Direita, JButtton_Frente, JButtton_Retaguarda;
	JTextArea textArea;

	// Usar SimuladorRobot por defeito; trocar por new myRobotLego() para o robot real
	private IRobot robot = new SimuladorRobot();

	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			try {
				GUI_TP1 frame = new GUI_TP1();
				frame.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public GUI_TP1() {
		dados = new DadosGUI_TP1();

		setTitle("Trabalho 1 por Bruno Rodrigues");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 580);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		Label_Robot = new JLabel("    Robot");
		Label_Robot.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		Label_Robot.setBounds(110, 10, 71, 16);
		contentPane.add(Label_Robot);

		textField_Robot = new JTextField();
		textField_Robot.setBounds(185, 11, 80, 16);
		contentPane.add(textField_Robot);
		textField_Robot.setColumns(10);

		label_Consola_1 = new JLabel("Consola");
		label_Consola_1.setHorizontalAlignment(SwingConstants.CENTER);
		label_Consola_1.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		label_Consola_1.setBounds(190, 351, 70, 16);
		contentPane.add(label_Consola_1);

		chckbxDebug_1 = new JCheckBox("Debug");
		chckbxDebug_1.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		chckbxDebug_1.setBounds(6, 346, 97, 23);
		contentPane.add(chckbxDebug_1);

		label_Angulo = new JLabel("    Ângulo");
		label_Angulo.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		label_Angulo.setBounds(120, 42, 70, 16);
		contentPane.add(label_Angulo);

		Label_Raio = new JLabel("    Raio");
		Label_Raio.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		Label_Raio.setBounds(10, 42, 46, 16);
		contentPane.add(Label_Raio);

		textField_Raio = new JTextField();
		textField_Raio.setColumns(10);
		textField_Raio.setBounds(69, 43, 50, 16);
		contentPane.add(textField_Raio);

		label_Distancia = new JLabel("    Distância");
		label_Distancia.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		label_Distancia.setBounds(276, 45, 70, 14);
		contentPane.add(label_Distancia);

		textField_Distancia = new JTextField();
		textField_Distancia.setColumns(10);
		textField_Distancia.setBounds(359, 41, 50, 20);
		contentPane.add(textField_Distancia);

		rdbtnOnoff = new JRadioButton("On/Off");
		rdbtnOnoff.setBounds(276, 10, 109, 16);
		rdbtnOnoff.addActionListener(e -> toggleConexao());
		contentPane.add(rdbtnOnoff);

		textField_Angulo = new JTextField();
		textField_Angulo.setColumns(10);
		textField_Angulo.setBounds(200, 43, 50, 16);
		contentPane.add(textField_Angulo);

		JButtton_Esquerda = new JButton("Esquerda");
		JButtton_Esquerda.setOpaque(true);
		JButtton_Esquerda.setBackground(new Color(0, 0, 255));
		JButtton_Esquerda.setForeground(new Color(192, 192, 192));
		JButtton_Esquerda.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		JButtton_Esquerda.setBounds(80, 170, 90, 45);
		JButtton_Esquerda.addActionListener(e -> executarComando(() -> {
			double raio   = parseCampoDouble(textField_Raio, dados.getRaio());
			double angulo = parseCampoDouble(textField_Angulo, dados.getAngulo());
			robot.curvarEsquerda(raio, angulo);
			myPrint("Esquerda  raio=" + raio + " ângulo=" + angulo + "°");
		}));
		contentPane.add(JButtton_Esquerda);

		JButtton_Parar = new JButton("Parar");
		JButtton_Parar.setOpaque(true);
		JButtton_Parar.setBackground(new Color(255, 0, 0));
		JButtton_Parar.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		JButtton_Parar.setForeground(new Color(192, 192, 192));
		JButtton_Parar.setBounds(170, 170, 90, 45);
		JButtton_Parar.addActionListener(e -> executarComando(() -> {
			robot.parar(true);
			myPrint("Parar");
		}));
		contentPane.add(JButtton_Parar);

		JButtton_Direita = new JButton("Direita");
		JButtton_Direita.setOpaque(true);
		JButtton_Direita.setBackground(new Color(255, 255, 0));
		JButtton_Direita.setForeground(new Color(192, 192, 192));
		JButtton_Direita.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		JButtton_Direita.setBounds(259, 170, 90, 45);
		JButtton_Direita.addActionListener(e -> executarComando(() -> {
			double raio   = parseCampoDouble(textField_Raio, dados.getRaio());
			double angulo = parseCampoDouble(textField_Angulo, dados.getAngulo());
			robot.curvarDireita(raio, angulo);
			myPrint("Direita  raio=" + raio + " ângulo=" + angulo + "°");
		}));
		contentPane.add(JButtton_Direita);

		JButtton_Frente = new JButton("Frente");
		JButtton_Frente.setOpaque(true);
		JButtton_Frente.setBackground(new Color(128, 255, 0));
		JButtton_Frente.setForeground(new Color(192, 192, 192));
		JButtton_Frente.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		JButtton_Frente.setBounds(170, 126, 90, 45);
		JButtton_Frente.addActionListener(e -> executarComando(() -> {
			double dist = parseCampoDouble(textField_Distancia, dados.getDistancia());
			robot.reta(dist);
			myPrint("Frente  distância=" + dist + " cm");
		}));
		contentPane.add(JButtton_Frente);

		JButtton_Retaguarda = new JButton("Retaguarda");
		JButtton_Retaguarda.setOpaque(true);
		JButtton_Retaguarda.setBackground(new Color(255, 0, 255));
		JButtton_Retaguarda.setForeground(new Color(192, 192, 192));
		JButtton_Retaguarda.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		JButtton_Retaguarda.setBounds(170, 216, 90, 45);
		JButtton_Retaguarda.addActionListener(e -> executarComando(() -> {
			double dist = parseCampoDouble(textField_Distancia, dados.getDistancia());
			robot.recuar(dist);
			myPrint("Retaguarda  distância=" + dist + " cm");
		}));
		contentPane.add(JButtton_Retaguarda);

		scrollPane = new JScrollPane();
		scrollPane.setBounds(16, 376, 408, 150);
		contentPane.add(scrollPane);

		textArea = new JTextArea();
		textArea.setEditable(false);
		scrollPane.setViewportView(textArea);

		aplicarDadosNoFormulario();
	}

	private void toggleConexao() {
		String nomeRobot = textField_Robot.getText().trim();
		if (rdbtnOnoff.isSelected()) {
			myPrintSempre("A ligar ao robot: " + nomeRobot + "...");
			new SwingWorker<Boolean, Void>() {
				@Override protected Boolean doInBackground() {
					return robot.ligar(nomeRobot);
				}
				@Override protected void done() {
					try {
						boolean ok = get();
						rdbtnOnoff.setSelected(ok);
						myPrintSempre(ok ? "Ligado a: " + nomeRobot : "Falha na ligação a: " + nomeRobot);
						setBotoesMovimento(ok);
					} catch (Exception ex) {
						myPrintSempre("Erro: " + ex.getMessage());
						rdbtnOnoff.setSelected(false);
					}
				}
			}.execute();
		} else {
			robot.desligar();
			myPrintSempre("Desligado");
			setBotoesMovimento(false);
		}
	}

	// Executa um comando do robot numa SwingWorker para não bloquear a EDT
	private void executarComando(Runnable cmd) {
		if (!robot.isLigado()) {
			myPrintSempre("Robot não está ligado. Use o botão On/Off.");
			return;
		}
		setBotoesMovimento(false);
		new SwingWorker<Void, Void>() {
			@Override protected Void doInBackground() {
				cmd.run();
				return null;
			}
			@Override protected void done() {
				setBotoesMovimento(true);
			}
		}.execute();
	}

	private void setBotoesMovimento(boolean ativo) {
		JButtton_Frente.setEnabled(ativo);
		JButtton_Retaguarda.setEnabled(ativo);
		JButtton_Esquerda.setEnabled(ativo);
		JButtton_Direita.setEnabled(ativo);
		JButtton_Parar.setEnabled(ativo);
	}

	private void aplicarDadosNoFormulario() {
		textField_Robot.setText(dados.getRobotName());
		textField_Raio.setText(String.valueOf(dados.getRaio()));
		textField_Angulo.setText(String.valueOf(dados.getAngulo()));
		textField_Distancia.setText(String.valueOf(dados.getDistancia()));
		chckbxDebug_1.setSelected(dados.isDebug());
		rdbtnOnoff.setSelected(dados.isOnOff());
		setBotoesMovimento(false);
	}

	/** Escreve na consola apenas quando Debug está ativo. */
	private void myPrint(String msg) {
		if (!chckbxDebug_1.isSelected()) return;
		myPrintSempre(msg);
	}

	/** Escreve sempre na consola, independentemente do Debug. */
	private void myPrintSempre(String msg) {
		textArea.append(msg + "\n");
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}

	private double parseCampoDouble(JTextField campo, double valorPadrao) {
		try { return Double.parseDouble(campo.getText().trim()); }
		catch (NumberFormatException e) { return valorPadrao; }
	}
}
