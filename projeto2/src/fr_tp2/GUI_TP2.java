package fr_tp2;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

public class GUI_TP2 extends JFrame {

    private static final long serialVersionUID = 1L;

    // ── componentes TP1 ──────────────────────────────────────────────────────
    private JPanel      contentPane;
    private JTextField  textField_Robot, textField_Raio, textField_Angulo, textField_Distancia;
    private JCheckBox   chckbxDebug;
    private JTextArea   textArea;
    private JScrollPane scrollPane;
    private JRadioButton rdbtnOnoff;
    private JButton JButtton_Frente, JButtton_Retaguarda,
                    JButtton_Esquerda, JButtton_Direita, JButtton_Parar;

    // ── componentes TP2 — ponto objetivo ────────────────────────────────────
    private JTextField textField_Xf, textField_Yf, textField_PhiF;
    private JButton    btnCalcular, btnExecutar;

    // ── modelo ───────────────────────────────────────────────────────────────
    private DadosGUI_TP1 dados;
    private IRobot        robot;

    // última trajetória calculada (usada pelo botão Executar)
    private CalculadorTrajetoria.Resultado ultimaTrajetoria = null;

    // ── arranque ─────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                GUI_TP2 frame = new GUI_TP2();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public GUI_TP2() {
        dados = new DadosGUI_TP1();

        setTitle("Trabalho 2 por Bruno Rodrigues");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 470, 690);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        // ── linha 1: Robot + On/Off ──────────────────────────────────────────
        JLabel lbRobot = new JLabel("Robot");
        lbRobot.setFont(new Font("Times New Roman", Font.PLAIN, 14));
        lbRobot.setBounds(110, 10, 46, 16);
        contentPane.add(lbRobot);

        textField_Robot = new JTextField(dados.getRobotName());
        textField_Robot.setBounds(160, 9, 80, 18);
        contentPane.add(textField_Robot);

        rdbtnOnoff = new JRadioButton("On/Off");
        rdbtnOnoff.setBounds(276, 8, 109, 18);
        rdbtnOnoff.addActionListener(e -> toggleConexao());
        contentPane.add(rdbtnOnoff);

        // ── linha 2: Raio / Ângulo / Distância ──────────────────────────────
        JLabel lbRaio = new JLabel("Raio");
        lbRaio.setFont(new Font("Times New Roman", Font.PLAIN, 14));
        lbRaio.setBounds(10, 38, 40, 16);
        contentPane.add(lbRaio);

        textField_Raio = new JTextField(String.valueOf(dados.getRaio()));
        textField_Raio.setBounds(55, 37, 55, 16);
        contentPane.add(textField_Raio);

        JLabel lbAngulo = new JLabel("Ângulo");
        lbAngulo.setFont(new Font("Times New Roman", Font.PLAIN, 14));
        lbAngulo.setBounds(125, 38, 55, 16);
        contentPane.add(lbAngulo);

        textField_Angulo = new JTextField(String.valueOf(dados.getAngulo()));
        textField_Angulo.setBounds(183, 37, 55, 16);
        contentPane.add(textField_Angulo);

        JLabel lbDist = new JLabel("Distância");
        lbDist.setFont(new Font("Times New Roman", Font.PLAIN, 14));
        lbDist.setBounds(255, 38, 68, 16);
        contentPane.add(lbDist);

        textField_Distancia = new JTextField(String.valueOf(dados.getDistancia()));
        textField_Distancia.setBounds(328, 37, 55, 16);
        contentPane.add(textField_Distancia);

        // ── botões de movimento (TP1) ────────────────────────────────────────
        JButtton_Frente = botao("Frente", new Color(128, 255, 0));
        JButtton_Frente.setBounds(170, 65, 90, 45);
        JButtton_Frente.addActionListener(e -> {
            double dist = parseDouble(textField_Distancia, dados.getDistancia());
            executarComando(() -> { robot.reta(dist); myPrint("Frente  dist=" + dist + " cm"); });
        });
        contentPane.add(JButtton_Frente);

        JButtton_Esquerda = botao("Esquerda", new Color(0, 0, 255));
        JButtton_Esquerda.setBounds(80, 112, 90, 45);
        JButtton_Esquerda.addActionListener(e -> {
            double r = parseDouble(textField_Raio,   dados.getRaio());
            double a = parseDouble(textField_Angulo, dados.getAngulo());
            executarComando(() -> { robot.curvarEsquerda(r, a); myPrint("Esquerda r=" + r + " a=" + a + "°"); });
        });
        contentPane.add(JButtton_Esquerda);

        JButtton_Parar = botao("Parar", new Color(255, 0, 0));
        JButtton_Parar.setBounds(170, 112, 90, 45);
        JButtton_Parar.addActionListener(e ->
            executarComando(() -> { robot.parar(true); myPrint("Parar"); }));
        contentPane.add(JButtton_Parar);

        JButtton_Direita = botao("Direita", new Color(255, 220, 0));
        JButtton_Direita.setBounds(259, 112, 90, 45);
        JButtton_Direita.addActionListener(e -> {
            double r = parseDouble(textField_Raio,   dados.getRaio());
            double a = parseDouble(textField_Angulo, dados.getAngulo());
            executarComando(() -> { robot.curvarDireita(r, a); myPrint("Direita r=" + r + " a=" + a + "°"); });
        });
        contentPane.add(JButtton_Direita);

        JButtton_Retaguarda = botao("Retaguarda", new Color(255, 0, 255));
        JButtton_Retaguarda.setBounds(170, 159, 90, 45);
        JButtton_Retaguarda.addActionListener(e -> {
            double dist = parseDouble(textField_Distancia, dados.getDistancia());
            executarComando(() -> { robot.recuar(dist); myPrint("Retaguarda  dist=" + dist + " cm"); });
        });
        contentPane.add(JButtton_Retaguarda);

        // ── painel Ponto Objetivo (TP2) ──────────────────────────────────────
        JPanel panelObjetivo = new JPanel(null);
        panelObjetivo.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Ponto Objetivo  (Xf, Yf, φf)",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Times New Roman", Font.BOLD, 13)));
        panelObjetivo.setBounds(10, 213, 440, 110);
        contentPane.add(panelObjetivo);

        JLabel lbXf = new JLabel("Xf (cm)");
        lbXf.setFont(new Font("Times New Roman", Font.PLAIN, 13));
        lbXf.setBounds(10, 26, 60, 16);
        panelObjetivo.add(lbXf);

        textField_Xf = new JTextField("70");
        textField_Xf.setBounds(72, 24, 55, 18);
        panelObjetivo.add(textField_Xf);

        JLabel lbYf = new JLabel("Yf (cm)");
        lbYf.setFont(new Font("Times New Roman", Font.PLAIN, 13));
        lbYf.setBounds(148, 26, 55, 16);
        panelObjetivo.add(lbYf);

        textField_Yf = new JTextField("40");
        textField_Yf.setBounds(205, 24, 55, 18);
        panelObjetivo.add(textField_Yf);

        JLabel lbPhi = new JLabel("φf  (°)");
        lbPhi.setFont(new Font("Times New Roman", Font.PLAIN, 13));
        lbPhi.setBounds(280, 26, 50, 16);
        panelObjetivo.add(lbPhi);

        textField_PhiF = new JTextField("70");
        textField_PhiF.setBounds(330, 24, 55, 18);
        panelObjetivo.add(textField_PhiF);

        btnCalcular = new JButton("Calcular Trajetória");
        btnCalcular.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        btnCalcular.setBackground(new Color(255, 140, 0));
        btnCalcular.setForeground(Color.WHITE);
        btnCalcular.setOpaque(true);
        btnCalcular.setFont(new Font("Times New Roman", Font.BOLD, 13));
        btnCalcular.setBounds(10, 55, 185, 38);
        btnCalcular.addActionListener(e -> calcularTrajetoria());
        panelObjetivo.add(btnCalcular);

        btnExecutar = new JButton("Executar");
        btnExecutar.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        btnExecutar.setBackground(new Color(0, 160, 80));
        btnExecutar.setForeground(Color.WHITE);
        btnExecutar.setOpaque(true);
        btnExecutar.setFont(new Font("Times New Roman", Font.BOLD, 13));
        btnExecutar.setBounds(205, 55, 120, 38);
        btnExecutar.setEnabled(false);
        btnExecutar.addActionListener(e -> executarTrajetoria());
        panelObjetivo.add(btnExecutar);

        // ── Debug + Consola ──────────────────────────────────────────────────
        chckbxDebug = new JCheckBox("Debug");
        chckbxDebug.setFont(new Font("Times New Roman", Font.PLAIN, 14));
        chckbxDebug.setSelected(dados.isDebug());
        chckbxDebug.setBounds(6, 323, 97, 23);
        contentPane.add(chckbxDebug);

        JLabel lbConsola = new JLabel("Consola");
        lbConsola.setHorizontalAlignment(SwingConstants.CENTER);
        lbConsola.setFont(new Font("Times New Roman", Font.PLAIN, 14));
        lbConsola.setBounds(190, 323, 70, 16);
        contentPane.add(lbConsola);

        scrollPane = new JScrollPane();
        scrollPane.setBounds(6, 348, 440, 290);
        contentPane.add(scrollPane);

        textArea = new JTextArea();
        textArea.setEditable(false);
        scrollPane.setViewportView(textArea);

        // robot criado após textArea estar pronto (o consumer referencia myPrintSempre)
        // Trocar por new myRobotLego() quando o robot físico estiver disponível
        robot = new SimuladorRobot(this::myPrintSempre);

        setBotoesMovimento(false);
    }

    // ── ligação Bluetooth ────────────────────────────────────────────────────
    private void toggleConexao() {
        String nome = textField_Robot.getText().trim();
        if (rdbtnOnoff.isSelected()) {
            myPrintSempre("A ligar ao robot: " + nome + "...");
            new SwingWorker<Boolean, Void>() {
                @Override protected Boolean doInBackground() { return robot.ligar(nome); }
                @Override protected void done() {
                    try {
                        boolean ok = get();
                        rdbtnOnoff.setSelected(ok);
                        myPrintSempre(ok ? "Ligado a: " + nome : "Falha na ligação a: " + nome);
                        setBotoesMovimento(ok);
                        btnExecutar.setEnabled(ok && ultimaTrajetoria != null);
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
            btnExecutar.setEnabled(false);
        }
    }

    // ── calcular trajetória ──────────────────────────────────────────────────
    private void calcularTrajetoria() {
        double xf   = parseDouble(textField_Xf,   70.0);
        double yf   = parseDouble(textField_Yf,   40.0);
        double phiF = parseDouble(textField_PhiF, 70.0);

        myPrintSempre("─────────────────────────────────────");
        myPrintSempre(String.format("Ponto objetivo: Xf=%.2f  Yf=%.2f  φf=%.2f°", xf, yf, phiF));

        CalculadorTrajetoria.Resultado res = CalculadorTrajetoria.calcular(xf, yf, phiF);

        if (res == null) {
            myPrintSempre("Ponto não atingível com as trajetórias suportadas.\n");
            ultimaTrajetoria = null;
            btnExecutar.setEnabled(false);
            return;
        }

        ultimaTrajetoria = res;
        myPrintSempre(res.toConsola());
        btnExecutar.setEnabled(robot.isLigado());
    }

    // ── executar trajetória calculada ────────────────────────────────────────
    private void executarTrajetoria() {
        if (ultimaTrajetoria == null || !robot.isLigado()) return;
        myPrintSempre("A executar trajetória " + ultimaTrajetoria.trajetoria + "...");
        setBotoesMovimento(false);
        btnExecutar.setEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                for (CalculadorTrajetoria.Passo p : ultimaTrajetoria.passos) {
                    switch (p.tipo) {
                        case CURVA_ESQ: robot.curvarEsquerda(p.raio, p.angulo); break;
                        case CURVA_DIR: robot.curvarDireita(p.raio, p.angulo);  break;
                        case RETA:      robot.reta(p.distancia);                 break;
                    }
                }
                return null;
            }
            @Override
            protected void done() {
                myPrintSempre("Trajetória concluída.");
                setBotoesMovimento(robot.isLigado());
                btnExecutar.setEnabled(robot.isLigado() && ultimaTrajetoria != null);
            }
        }.execute();
    }

    // ── execução de comando simples ──────────────────────────────────────────
    private void executarComando(Runnable cmd) {
        if (!robot.isLigado()) {
            myPrintSempre("Robot não está ligado. Use o botão On/Off.");
            return;
        }
        setBotoesMovimento(false);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() { cmd.run(); return null; }
            @Override protected void done()           { setBotoesMovimento(true); }
        }.execute();
    }

    private void setBotoesMovimento(boolean ativo) {
        JButtton_Frente.setEnabled(ativo);
        JButtton_Retaguarda.setEnabled(ativo);
        JButtton_Esquerda.setEnabled(ativo);
        JButtton_Direita.setEnabled(ativo);
        JButtton_Parar.setEnabled(ativo);
    }

    // ── consola thread-safe ──────────────────────────────────────────────────
    private void myPrint(String msg) {
        if (chckbxDebug.isSelected()) myPrintSempre(msg);
    }

    private void myPrintSempre(String msg) {
        SwingUtilities.invokeLater(() -> {
            textArea.append(msg.endsWith("\n") ? msg : msg + "\n");
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }

    // ── utilitários ──────────────────────────────────────────────────────────
    private double parseDouble(JTextField campo, double padrao) {
        try { return Double.parseDouble(campo.getText().trim()); }
        catch (NumberFormatException e) { return padrao; }
    }

    private JButton botao(String texto, Color cor) {
        JButton b = new JButton(texto);
        b.setOpaque(true);
        b.setBackground(cor);
        b.setForeground(new Color(192, 192, 192));
        b.setFont(new Font("Times New Roman", Font.PLAIN, 14));
        return b;
    }
}
