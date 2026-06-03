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
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class GUI_TP1 extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField textField_Angulo,textField_Distancia, textField_Raio,textField_Robot;
	private JCheckBox chckbxDebug_1;
	private DadosGUI_TP1 dados;
	
	
	JLabel Label_Robot , label_Consola_1 ,label_Angulo , Label_Raio,label_Distancia;
	JScrollPane scrollPane;
	JRadioButton rdbtnOnoff;
	JButton JButtton_Esquerda,JButtton_Parar ,JButtton_Direita, JButtton_Frente , JButtton_Retaguarda;
	JTextArea textArea;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUI_TP1 frame = new GUI_TP1();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public GUI_TP1() {
		dados = new DadosGUI_TP1();
		
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
		textField_Robot.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
			}
		});
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
		JButtton_Esquerda.addActionListener(e -> myPrint("Esquerda, raio: " + textField_Raio.getText().trim() + ", ângulo: " + textField_Angulo.getText().trim()));
		contentPane.add(JButtton_Esquerda);
		
		JButtton_Parar = new JButton("Parar");
		JButtton_Parar.setOpaque(true);
		JButtton_Parar.setBackground(new Color(255, 0, 0));
		JButtton_Parar.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		JButtton_Parar.setForeground(new Color(192, 192, 192));
		JButtton_Parar.setBounds(170, 170, 90, 45);
		JButtton_Parar.addActionListener(e -> myPrint("Parar"));
		contentPane.add(JButtton_Parar);
		
		 JButtton_Direita = new JButton("Esquerda");
		JButtton_Direita.setOpaque(true);
		JButtton_Direita.setBackground(new Color(255, 255, 0));
		JButtton_Direita.setForeground(new Color(192, 192, 192));
		JButtton_Direita.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		JButtton_Direita.setBounds(259, 170, 90, 45);
		JButtton_Direita.addActionListener(e -> myPrint("Direita, raio: " + textField_Raio.getText().trim() + ", ângulo: " + textField_Angulo.getText().trim()));
		contentPane.add(JButtton_Direita);
		
		JButtton_Frente = new JButton("Frente");
		JButtton_Frente.addActionListener(e -> myPrint("Frente, distância: " + textField_Distancia.getText().trim()));
		JButtton_Frente.setOpaque(true);
		JButtton_Frente.setBackground(new Color(128, 255, 0));
		JButtton_Frente.setForeground(new Color(192, 192, 192));
		JButtton_Frente.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		JButtton_Frente.setBounds(170, 126, 90, 45);
		contentPane.add(JButtton_Frente);
		
		JButtton_Retaguarda = new JButton("Retaguarda");
		JButtton_Retaguarda.setOpaque(true);
		JButtton_Retaguarda.setBackground(new Color(255, 0, 255));
		JButtton_Retaguarda.setForeground(new Color(192, 192, 192));
		JButtton_Retaguarda.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		JButtton_Retaguarda.setBounds(170, 216, 90, 45);
		JButtton_Retaguarda.addActionListener(e -> myPrint("Retaguarda, distância: " + textField_Distancia.getText().trim()));
		contentPane.add(JButtton_Retaguarda);
		
		scrollPane = new JScrollPane();
		scrollPane.setBounds(16, 376, 408, 150);
		contentPane.add(scrollPane);
		
		textArea = new JTextArea();
		scrollPane.setViewportView(textArea);
		
		aplicarDadosNoFormulario();
	}
	
	private void aplicarDadosNoFormulario() {
		textField_Robot.setText(dados.getRobotName());
		textField_Raio.setText(String.valueOf(dados.getRaio()));
		textField_Angulo.setText(String.valueOf(dados.getAngulo()));
		textField_Distancia.setText(String.valueOf(dados.getDistancia()));
		chckbxDebug_1.setSelected(dados.isDebug());
		rdbtnOnoff.setSelected(dados.isOnOff());
	}

	/** Escreve na consola de debug (só aparece se a checkbox Debug estiver marcada). */
	private void myPrint(String mensagem) {
		if (!chckbxDebug_1.isSelected())
			return;
		textArea.append(mensagem + "\n");
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}
}
