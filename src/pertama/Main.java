package pertama;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import javax.imageio.ImageIO;
import javax.swing.*;

public class Main extends Component implements Runnable {

    private String ip = "localhost";
    private int port = 12345;
    private JFrame frame = new JFrame();
    private final int L = 506;
    private final int P = 527;
    private Thread thread;
    private Painter painter= new Painter();
    private Socket socket;
    private DataOutputStream dos;
    private DataInputStream dis;

    private ServerSocket serverSocket;

    private BufferedImage board;
    private BufferedImage redX;
    private BufferedImage blueX;
    private BufferedImage redCircle;
    private BufferedImage blueCircle;

    private String[] spaces = new String[9];

    private boolean yourTurn = false;
    private boolean circle = true;
    private boolean accepted = false;
    private boolean unableToCommunicateWithOpponent = false;
    private boolean won = false;
    private boolean enemyWon = false;
    private boolean tie = false;

    private int lengthOfSpace = 160;
    private int errors = 0;
    private int firstSpot = -1;
    private int secondSpot = -1;
    Scanner scanner = new Scanner(System.in);

    private Font font = new Font("Verdana", Font.BOLD, 14);
    private Font smallerFont = new Font("Verdana", Font.BOLD, 20);
    private Font largerFont = new Font("Verdana", Font.BOLD, 50);

    private String waitingString = "Menunggu Player Lainnya";
    private String unableToCommunicateWithOpponentString = "Unable to communicate with opponent.";
    private String wonString = "Menang";
    private String enemyWonString = "Kalah";
    private String tieString = "DRAW";

    private int[][] wins = new int[][] { { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 }, { 0, 3, 6 }, { 1, 4, 7 }, { 2, 5, 8 }, { 0, 4, 8 }, { 2, 4, 6 } };


    @SuppressWarnings("unused")
    public static void main(String[] args) {
        System.out.println("Masuk di main");
        Main game = new Main();
    }

    public Main() {
        System.out.println("=========================");
        System.out.println("IP yang tersedia = " + ip);
        System.out.println("PORT yang tersedia = " + port);
        System.out.println("=========================");
        System.out.println("Harap masukkan IP anda : ");
        ip = scanner.nextLine();
        System.out.println("Harap masukkan port anda : ");
        port = scanner.nextInt();

        while (port != 12345) {
            System.out.println("Harap Masukkan Port yang telah tersedia");
            port = scanner.nextInt();
        }

        try {
            board = ImageIO.read(getClass().getResourceAsStream("../gambar/pembatas.png"));
            redX = ImageIO.read(getClass().getResourceAsStream("../gambar/X pink.png"));
            redCircle = ImageIO.read(getClass().getResourceAsStream("../gambar/lingkaran pink.png"));
            blueX = ImageIO.read(getClass().getResourceAsStream("../gambar/X ungu.png"));
            blueCircle = ImageIO.read(getClass().getResourceAsStream("../gambar/lingkaran ungu.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        painter.setPreferredSize(new Dimension(L, P));

        if (!connect()) initializeServer();

        frame.setTitle("Game TicTacToe Clasic");
        frame.setContentPane(painter);
        frame.setSize(L, P);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);

        thread = new Thread(this, "TicTacToe");
        thread.start();
    }

    public void run() {
        System.out.println("Masuk di run");
        while (true) {
            tick();
            painter.repaint();
            System.out.println(painter);

            if (!circle && !accepted) {
                System.out.println("Masuk di run if ");
                System.out.println(circle);
                System.out.println(accepted);
                listenForServerRequest();
            }

        }
    }

    private void render(Graphics g) {
        System.out.println("Masuk di render");
        g.drawImage(board, 0, 0, null);
        if (unableToCommunicateWithOpponent) {
            g.setColor(Color.RED);
            g.setFont(smallerFont);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int stringWidth = g2.getFontMetrics().stringWidth(unableToCommunicateWithOpponentString);
            g.drawString(unableToCommunicateWithOpponentString, L / 2 - stringWidth / 2, P / 2);
            return;
        }

        if (accepted) {
            System.out.println("di render terus isi acept ="+accepted);
            for (int i = 0; i < spaces.length; i++) {
                System.out.println(i);
                System.out.println(spaces);
                if (spaces[i] != null) {
                    if (spaces[i].equals("X")) {
                        if (circle) {
                            g.drawImage(redX, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
                        } else {
                            g.drawImage(blueX, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
                        }
                    } else if (spaces[i].equals("O")) {
                        if (circle) {
                            g.drawImage(blueCircle, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
                        } else {
                            g.drawImage(redCircle, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
                        }
                    }
                }
            }
            if (won || enemyWon) {
                System.out.println("masuk di render won"+won +"enemywon"+enemyWon);
                Graphics2D g2 = (Graphics2D) g;
                g2.setStroke(new BasicStroke(10));
                g.setColor(Color.BLACK);
                g.drawLine(firstSpot % 3 * lengthOfSpace + 10 * firstSpot % 3 + lengthOfSpace / 2, (int) (firstSpot / 3) * lengthOfSpace + 10 * (int) (firstSpot / 3) + lengthOfSpace / 2, secondSpot % 3 * lengthOfSpace + 10 * secondSpot % 3 + lengthOfSpace / 2, (int) (secondSpot / 3) * lengthOfSpace + 10 * (int) (secondSpot / 3) + lengthOfSpace / 2);

                g.setColor(Color.RED);
                g.setFont(smallerFont);
                if (won) {
                    System.out.println("masuk di won");
                    int stringWidth = g2.getFontMetrics().stringWidth(wonString);
                    g.drawString(wonString, L / 2 - stringWidth / 2, P / 2);
                } else if (enemyWon) {
                    int stringWidth = g2.getFontMetrics().stringWidth(enemyWonString);
                    g.drawString(enemyWonString, L / 2 - stringWidth / 2, P / 2);
                }
            }
            if (tie) {
                Graphics2D g2 = (Graphics2D) g;
                g.setColor(Color.BLACK);
                g.setFont(largerFont);
                int stringWidth = g2.getFontMetrics().stringWidth(tieString);
                System.out.println("String widh "+stringWidth);
                g.drawString(tieString, L / 2 - stringWidth / 2, P / 2);
            }
        } else {
            g.setColor(Color.RED);
            g.setFont(font);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int stringWidth = g2.getFontMetrics().stringWidth(waitingString);
            g.drawString(waitingString, L / 2 - stringWidth / 2, P / 2);
        }

    }

    private void tick() {
        System.out.println("Masuk di tick");
        if (errors >= 10) unableToCommunicateWithOpponent = true;
        System.out.println("error ="+errors+" your turn = "+yourTurn+" unablecomunication = "+unableToCommunicateWithOpponent);

        if (!yourTurn && !unableToCommunicateWithOpponent) {
            try {
                int space = dis.readInt();
                if (circle) spaces[space] = "X";
                else spaces[space] = "O";
                checkForEnemyWin();
                checkForTie();
                yourTurn = true;
            } catch (IOException e) {
                e.printStackTrace();
                errors++;
            }
        }
    }

    private void cekpemenang() {
        System.out.println("Masuk di cek menang");
        for (int i = 0; i < wins.length; i++) {
            if (circle) {
                if (spaces[wins[i][0]] == "O" && spaces[wins[i][1]] == "O" && spaces[wins[i][2]] == "O") {
                    firstSpot = wins[i][0];
                    secondSpot = wins[i][2];
                    won = true;
                }
            } else {
                if (spaces[wins[i][0]] == "X" && spaces[wins[i][1]] == "X" && spaces[wins[i][2]] == "X") {
                    firstSpot = wins[i][0];
                    secondSpot = wins[i][2];
                    won = true;
                }
            }
        }
    }

    private void checkForEnemyWin() {
        System.out.println("Masuk di cek musuh win");
        for (int i = 0; i < wins.length; i++) {
            if (circle) {
                if (spaces[wins[i][0]] == "X" && spaces[wins[i][1]] == "X" && spaces[wins[i][2]] == "X") {
                    firstSpot = wins[i][0];
                    secondSpot = wins[i][2];
                    enemyWon = true;
                }
            } else {
                if (spaces[wins[i][0]] == "O" && spaces[wins[i][1]] == "O" && spaces[wins[i][2]] == "O") {
                    firstSpot = wins[i][0];
                    secondSpot = wins[i][2];
                    enemyWon = true;
                }
            }
        }
    }

    private void checkForTie() {
        System.out.println("Masuk di cek for tie");
        for (int i = 0; i < spaces.length; i++) {
            if (spaces[i] == null) {
                return;
            }
        }
        tie = true;
    }

    private void listenForServerRequest() {
        System.out.println("Masuk di listen server Request");
        Socket socket = null;
        try {
            socket = serverSocket.accept();
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            accepted = true;
            System.out.println("CLIENT HAS REQUESTED TO JOIN, AND WE HAVE ACCEPTED");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean connect() {
        System.out.println("Masuk di Connect");
        try {
            socket = new Socket(ip, port);
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            System.out.println(dos);
            System.out.println(dis);
            accepted = true;
        } catch (IOException e) {
            System.out.println("Dapat Mengakses: " + ip + ":" + port + " | Memulai Server");
            return false;
        }
        System.out.println("Sukses.");
        return true;
    }

    private void initializeServer() {
        System.out.println("Masuk di initialize Server");
        try {
            serverSocket = new ServerSocket(port, 8, InetAddress.getByName(ip));
        } catch (Exception e) {
            e.printStackTrace();
        }
        yourTurn = true;
        circle = false;
    }




    private class Painter extends JPanel implements MouseListener {
        private static final long serialVersionUID = 1L;

        public Painter() {
            System.out.println("Masuk di painter");
            setFocusable(true);
            requestFocus();
            setBackground(Color.white);
            addMouseListener(this);
        }

        @Override
        public void paintComponent(Graphics g) {
            System.out.println("Masuk di paintcomponen");
            super.paintComponent(g);
            render(g);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            System.out.println("Masuk di Mouseklik");
            if (accepted) {
                if (yourTurn && !unableToCommunicateWithOpponent && !won && !enemyWon) {
                    int x = e.getX() / lengthOfSpace;
                    int y = e.getY() / lengthOfSpace;
                    y *= 3;
                    int position = x + y;

                    if (spaces[position] == null) {
                        if (!circle) spaces[position] = "X";
                        else spaces[position] = "O";
                        yourTurn = false;
                        repaint();
                        Toolkit.getDefaultToolkit().sync();

                        try {
                            dos.writeInt(position);
                            dos.flush();
                        } catch (IOException e1) {
                            errors++;
                            e1.printStackTrace();
                        }

                        System.out.println("Data telah terkirim");
                        cekpemenang();
                        checkForTie();

                    }
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }

    }

}
