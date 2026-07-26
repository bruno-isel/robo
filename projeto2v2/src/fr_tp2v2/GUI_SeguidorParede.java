package fr_tp2v2;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * GUI do TP2 Parte II - Robot Autónomo Seguidor de Parede.
 * Mesmo estilo/padrão de GUI_TP1/GUI_TP2 (null layout, SwingWorker, consola).
 */
public class GUI_SeguidorParede extends JFrame {

    private static final long serialVersionUID = 1L;

    private JPanel contentPane;
    private JTextField textField_Robot;
    private JCheckBox chckbxDebug;
    private JTextArea textArea;
    private JScrollPane scrollPane;
    private JRadioButton rdbtnOnoff;
    private JButton btnSeguir;

    private IRobotParede robot;
    private volatile boolean seguindo = false;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                GUI_SeguidorParede frame = new GUI_SeguidorParede();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public GUI_SeguidorParede() {
        setTitle("TP2 Parte II - Seguidor de Parede");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 420, 420);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        JLabel lbRobot = new JLabel("Robot");
        lbRobot.setFont(new Font("Times New Roman", Font.PLAIN, 14));
        lbRobot.setBounds(20, 15, 46, 16);
        contentPane.add(lbRobot);

        textField_Robot = new JTextField("EVA");
        textField_Robot.setBounds(70, 14, 80, 20);
        contentPane.add(textField_Robot);

        rdbtnOnoff = new JRadioButton("On/Off");
        rdbtnOnoff.setBounds(180, 13, 100, 20);
        rdbtnOnoff.addActionListener(e -> toggleConexao());
        contentPane.add(rdbtnOnoff);

        btnSeguir = new JButton("Iniciar Seguimento");
        btnSeguir.setOpaque(true);
        btnSeguir.setBackground(new Color(0, 160, 80));
        btnSeguir.setForeground(Color.WHITE);
        btnSeguir.setFont(new Font("Times New Roman", Font.BOLD, 14));
        btnSeguir.setBounds(20, 50, 250, 40);
        btnSeguir.setEnabled(false);
        btnSeguir.addActionListener(e -> toggleSeguimento());
        contentPane.add(btnSeguir);

        chckbxDebug = new JCheckBox("Debug");
        chckbxDebug.setFont(new Font("Times New Roman", Font.PLAIN, 14));
        chckbxDebug.setSelected(true);
        chckbxDebug.setBounds(20, 100, 100, 23);
        contentPane.add(chckbxDebug);

        JLabel lbConsola = new JLabel("Consola");
        lbConsola.setFont(new Font("Times New Roman", Font.PLAIN, 14));
        lbConsola.setBounds(20, 130, 70, 16);
        contentPane.add(lbConsola);

        scrollPane = new JScrollPane();
        scrollPane.setBounds(20, 155, 370, 220);
        contentPane.add(scrollPane);

        textArea = new JTextArea();
        textArea.setEditable(false);
        scrollPane.setViewportView(textArea);

        // robot criado após textArea estar pronto (o consumer referencia myPrintSempre)
        robot = new myRobotParede(this::myPrintSempre);
    }

    private void toggleConexao() {
        String nome = textField_Robot.getText().trim();
        if (rdbtnOnoff.isSelected()) {
            // Reverte o toggle visual imediato do clique: só fica "ligado"
            // depois de robot.ligar() confirmar a ligação.
            rdbtnOnoff.setSelected(false);
            rdbtnOnoff.setEnabled(false);
            myPrintSempre("A ligar ao robot: " + nome + "...");
            new SwingWorker<Boolean, Void>() {
                @Override protected Boolean doInBackground() { return robot.ligar(nome); }
                @Override protected void done() {
                    try {
                        boolean ok = get();
                        rdbtnOnoff.setSelected(ok);
                        myPrintSempre(ok ? "Ligado a: " + nome : "Falha na ligação a: " + nome);
                        btnSeguir.setEnabled(ok);
                    } catch (Exception ex) {
                        myPrintSempre("Erro: " + ex.getMessage());
                        rdbtnOnoff.setSelected(false);
                    } finally {
                        rdbtnOnoff.setEnabled(true);
                    }
                }
            }.execute();
        } else {
            seguindo = false;
            robot.desligar();
            myPrintSempre("Desligado");
            btnSeguir.setEnabled(false);
            btnSeguir.setText("Iniciar Seguimento");
        }
    }

    private void toggleSeguimento() {
        if (!seguindo) {
            if (!robot.isLigado()) {
                myPrintSempre("Robot não está ligado. Use o botão On/Off.");
                return;
            }
            seguindo = true;
            btnSeguir.setText("Parar Seguimento");
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() {
                    loopSeguimento();
                    return null;
                }
                @Override protected void done() {
                    btnSeguir.setText("Iniciar Seguimento");
                }
            }.execute();
        } else {
            seguindo = false; // o loop deteta no proximo ciclo e sai sozinho
        }
    }

    private void loopSeguimento() {
        myPrintSempre("A iniciar seguimento de parede...");
        while (seguindo && robot.isLigado()) {
            if (robot.tocouObstaculo()) {
                myPrintSempre("[Seguidor] Obstáculo detetado - a recuperar");
                robot.parar();
                robot.recuar(SeguidorParedeLogica.DISTANCIA_RECUO);
                robot.curvarEsquerda(SeguidorParedeLogica.RAIO_RECUPERACAO, 90);
                continue;
            }
            int dist = robot.lerDistanciaSonar();
            SeguidorParedeLogica.Decisao d = SeguidorParedeLogica.decidir(dist);
            myPrint(String.format("[Seguidor] distância=%d cm -> %s", dist, d));
            robot.andarDiferencial(SeguidorParedeLogica.velEsquerda(d), SeguidorParedeLogica.velDireita(d));
            dormir(100);
        }
        robot.parar();
        myPrintSempre("Seguimento parado.");
    }

    /** Escreve na consola apenas quando Debug está ativo. */
    private void myPrint(String msg) {
        if (chckbxDebug.isSelected()) myPrintSempre(msg);
    }

    /** Escreve sempre na consola, independentemente do Debug. Thread-safe. */
    private void myPrintSempre(String msg) {
        SwingUtilities.invokeLater(() -> {
            textArea.append(msg + "\n");
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }

    private static void dormir(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
