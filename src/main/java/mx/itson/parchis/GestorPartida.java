package mx.itson.parchis;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.Objects;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * Parchís para dos jugadores, rojo y azul, en una sola computadora.
 *
 * Presenta el tablero y coordina la partida con sus adaptadores de persistencia y comunicación.
 */
public class GestorPartida extends JPanel implements MouseListener, ActionListener {

    // Tablero: cuadrícula de 19 x 19 celdas con 68 casillas en el recorrido
    private static final int CELDAS = 19;
    private static final int TOTAL_CASILLAS = 68;
    private final Partida partida;
    private final RepositorioEstadisticas repositorioEstadisticas;
    private final NotificadorJugadas notificadorJugadas;
    private boolean hayBaseDeDatos = false;
    private boolean avisoSinRed = false;
    private final Random azar = new Random();

    private JLabel etiquetaTurno;
    private JLabel etiquetaDado;
    private JLabel etiquetaEstadisticas;
    private JButton botonDado;
    private JButton botonNueva;
    private JTextArea bitacora;

    // Medidas del tablero en pantalla; se calculan al dibujar
    private int tamCelda;
    private int origenX;
    private int origenY;

    public GestorPartida() {
        this(new Partida(), new RepositorioEstadisticasMySQL(), new NotificadorSocket());
    }

    public GestorPartida(Partida partida, RepositorioEstadisticas repositorioEstadisticas,
            NotificadorJugadas notificadorJugadas) {
        this.partida = Objects.requireNonNull(partida, "partida");
        this.repositorioEstadisticas = Objects.requireNonNull(repositorioEstadisticas, "repositorioEstadisticas");
        this.notificadorJugadas = Objects.requireNonNull(notificadorJugadas, "notificadorJugadas");
        setPreferredSize(new Dimension(620, 620));
        setBackground(new Color(0xF3F4EF));
        addMouseListener(this);
        crearTablaEstadisticas();
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("espectador")) {
            EscuchaJugadasSocket.escuchar();
            return;
        }
        SwingUtilities.invokeLater(() -> {
            GestorPartida juego = new GestorPartida();
            JFrame ventana = new JFrame("Parchís");
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ventana.add(juego, BorderLayout.CENTER);
            ventana.add(juego.crearPanelLateral(), BorderLayout.EAST);
            ventana.pack();
            ventana.setLocationRelativeTo(null);
            juego.cargarEstadisticas();
            juego.agregarBitacora("Empieza " + juego.jugadorEnTurno().getNombre()
                    + " (" + juego.jugadorEnTurno().getColor().getNombre() + ").");
            juego.actualizarEtiquetas();
            ventana.setVisible(true);
        });
    }

    /** Arma el panel de la derecha: turno, dado, botones, bitácora y estadísticas. */
    public JPanel crearPanelLateral() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setPreferredSize(new Dimension(300, 620));

        JPanel arriba = new JPanel(new GridLayout(0, 1, 0, 8));
        JLabel titulo = new JLabel("Parchís");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        etiquetaTurno = new JLabel(" ");
        etiquetaTurno.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        etiquetaDado = new JLabel("Dado: -");
        etiquetaDado.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
        botonDado = new JButton("Lanzar dado");
        botonDado.addActionListener(this);
        botonNueva = new JButton("Nueva partida");
        botonNueva.addActionListener(this);
        arriba.add(titulo);
        arriba.add(etiquetaTurno);
        arriba.add(etiquetaDado);
        arriba.add(botonDado);
        arriba.add(botonNueva);
        panel.add(arriba, BorderLayout.NORTH);

        bitacora = new JTextArea();
        bitacora.setEditable(false);
        bitacora.setLineWrap(true);
        bitacora.setWrapStyleWord(true);
        bitacora.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        panel.add(new JScrollPane(bitacora), BorderLayout.CENTER);

        etiquetaEstadisticas = new JLabel(" ");
        etiquetaEstadisticas.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        panel.add(etiquetaEstadisticas, BorderLayout.SOUTH);
        return panel;
    }

    // ------------------------------------------------------------------ jugadas

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == botonDado) {
            lanzarDado();
        } else if (e.getSource() == botonNueva) {
            nuevaPartida();
        }
    }

    private void lanzarDado() {
        if (partida.estaTerminada()) {
            JOptionPane.showMessageDialog(this, "La partida terminó. Empieza una nueva.");
            return;
        }
        if (partida.yaTiro()) {
            JOptionPane.showMessageDialog(this, "Ya tiraste: elige una ficha.");
            return;
        }
        procesarDado(azar.nextInt(6) + 1);
    }

    /** Aplica el valor del dado al jugador en turno. */
    void procesarDado(int valor) {
        Jugador jugador = partida.getJugadorEnTurno();
        Partida.ResultadoTirada resultado = partida.lanzarDado(valor);
        agregarBitacora(jugador.getNombre() + " sacó " + partida.getDado() + ".");
        enviarAlRival("DADO;" + jugador.getColor().getNombre() + ";" + partida.getDado());
        if (!resultado.puedeMover()) {
            agregarBitacora("No puede mover ninguna ficha.");
            if (resultado.vuelveATirar()) {
                agregarBitacora("Sacó 6: vuelve a tirar.");
            } else {
                registrarCambioTurno();
            }
        }
        actualizarEtiquetas();
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (partida.estaTerminada() || !partida.yaTiro()) {
            return;
        }
        Jugador jugador = partida.getJugadorEnTurno();
        int[] fichas = jugador.getFichas();
        for (int i = 0; i < fichas.length; i++) {
            int[] centro = centroFicha(jugador.getColor(), i, fichas[i]);
            int dx = e.getX() - centro[0];
            int dy = e.getY() - centro[1];
            int radio = tamCelda / 2;
            if (dx * dx + dy * dy <= radio * radio) {
                if (fichas[i] == Partida.META) {
                    JOptionPane.showMessageDialog(this, "Esa ficha ya llegó a la meta.");
                    return;
                }
                if (fichas[i] == -1 && partida.getDado() != 5) {
                    JOptionPane.showMessageDialog(this, "Para sacar una ficha de la base necesitas un 5.");
                    return;
                }
                if (fichas[i] >= 0 && fichas[i] + partida.getDado() > Partida.META) {
                    JOptionPane.showMessageDialog(this,
                            "Esa ficha necesita exactamente un " + (Partida.META - fichas[i])
                            + " para llegar a la meta.");
                    return;
                }
                if (!partida.puedeMoverFicha(i)) {
                    return;
                }
                moverFicha(jugador, i);
                return;
            }
        }
    }

    private void moverFicha(Jugador jugador, int numero) {
        Partida.ResultadoMovimiento resultado = partida.moverFicha(numero);
        int posicion = resultado.posicionNueva();
        if (resultado.posicionAnterior() == -1) {
            agregarBitacora(jugador.getNombre() + " sacó la ficha " + (numero + 1) + " de su base.");
        } else {
            agregarBitacora(jugador.getNombre() + " avanzó " + partida.getDado()
                    + (partida.getDado() == 1 ? " casilla" : " casillas")
                    + " con la ficha " + (numero + 1) + ".");
        }
        Jugador rival = partida.getRivalDe(jugador);
        for (int fichaCapturada : resultado.fichasCapturadas()) {
            agregarBitacora("¡Capturó la ficha " + (fichaCapturada + 1) + " de "
                    + rival.getNombre() + "! Regresa a su base.");
            enviarAlRival("CAPTURA;" + rival.getColor().getNombre() + ";" + fichaCapturada);
        }
        enviarAlRival("MOVIMIENTO;" + jugador.getColor().getNombre() + ";" + numero + ";" + posicion);

        if (posicion == Partida.META) {
            agregarBitacora("La ficha " + (numero + 1) + " de " + jugador.getNombre() + " llegó a la meta.");
        }
        if (resultado.gano()) {
            terminarPartida(jugador);
            return;
        }
        if (resultado.vuelveATirar()) {
            agregarBitacora("Sacó 6: vuelve a tirar.");
        } else {
            registrarCambioTurno();
        }
        actualizarEtiquetas();
        repaint();
    }

    private Jugador jugadorEnTurno() {
        return partida.getJugadorEnTurno();
    }

    private void registrarCambioTurno() {
        Jugador jugador = jugadorEnTurno();
        agregarBitacora("Turno de " + jugador.getNombre() + " ("
                + jugador.getColor().getNombre() + ").");
    }

    private void terminarPartida(Jugador ganador) {
        agregarBitacora("¡" + ganador.getNombre() + " ganó la partida!");
        enviarAlRival("FIN;" + ganador.getColor().getNombre());
        guardarEstadisticas(ganador);
        cargarEstadisticas();
        actualizarEtiquetas();
        repaint();
        JOptionPane.showMessageDialog(this, "¡Ganó " + ganador.getNombre() + " ("
                + ganador.getColor().getNombre() + ")!");
    }

    private void nuevaPartida() {
        partida.nuevaPartida();
        bitacora.setText("");
        agregarBitacora("Nueva partida. Empieza " + jugadorEnTurno().getNombre() + ".");
        actualizarEtiquetas();
        repaint();
    }

    private void actualizarEtiquetas() {
        Jugador jugador = jugadorEnTurno();
        if (partida.estaTerminada()) {
            etiquetaTurno.setText("Partida terminada");
            etiquetaTurno.setForeground(Color.DARK_GRAY);
        } else {
            etiquetaTurno.setText("Turno: " + jugador.getNombre() + " ("
                    + jugador.getColor().getNombre() + ")");
            etiquetaTurno.setForeground(jugador.getColor().getColor());
        }
        etiquetaDado.setText("Dado: " + (partida.getDado() == 0
                ? "-" : String.valueOf(partida.getDado()))
                + (partida.yaTiro() ? " · elige una ficha" : ""));
        botonDado.setEnabled(!partida.estaTerminada() && !partida.yaTiro());
    }

    private void agregarBitacora(String texto) {
        bitacora.append(texto + "\n");
        bitacora.setCaretPosition(bitacora.getDocument().getLength());
    }

    // ------------------------------------------------------------------ red

    /** Avisa la jugada a la computadora del rival. Si no contesta, se sigue jugando aquí. */
    private void enviarAlRival(String mensaje) {
        try {
            notificadorJugadas.enviar(mensaje);
        } catch (IOException e) {
            if (!avisoSinRed) {
                avisoSinRed = true;
                agregarBitacora("Sin conexión con " + ConfiguracionRed.HOST + ":"
                        + ConfiguracionRed.PUERTO
                        + ". Se sigue jugando en esta computadora.");
            }
        }
    }

    private void crearTablaEstadisticas() {
        try {
            repositorioEstadisticas.inicializar();
            hayBaseDeDatos = true;
        } catch (RepositorioEstadisticasException e) {
            hayBaseDeDatos = false;
            System.out.println("Sin base de datos: " + e.getMessage());
        }
    }

    private void cargarEstadisticas() {
        if (!hayBaseDeDatos) {
            etiquetaEstadisticas.setText("<html>Estadísticas: sin base de datos</html>");
            return;
        }
        String texto = "<html><b>Estadísticas</b>";
        try {
            for (RepositorioEstadisticas.EstadisticaJugador estadistica
                    : repositorioEstadisticas.cargar()) {
                texto += "<br>" + estadistica.color().getNombre() + ": " + estadistica.ganadas()
                        + " ganadas de " + estadistica.jugadas();
            }
        } catch (RepositorioEstadisticasException e) {
            texto += "<br>No se pudieron leer: " + e.getMessage();
        }
        etiquetaEstadisticas.setText(texto + "</html>");
    }

    private void guardarEstadisticas(Jugador ganador) {
        if (!hayBaseDeDatos) {
            agregarBitacora("Sin base de datos: no se guardaron las estadísticas.");
            return;
        }
        try {
            repositorioEstadisticas.guardar(ganador);
        } catch (RepositorioEstadisticasException e) {
            agregarBitacora("No se guardaron las estadísticas: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        tamCelda = (Math.min(getWidth(), getHeight()) - 20) / CELDAS;
        origenX = (getWidth() - tamCelda * CELDAS) / 2;
        origenY = (getHeight() - tamCelda * CELDAS) / 2;

        // Bases: rojo arriba a la izquierda y azul arriba a la derecha; las de abajo no se usan
        dibujarBase(g2, 0, 0, ColorJugador.ROJO.getColor());
        dibujarBase(g2, 0, 11, ColorJugador.AZUL.getColor());
        dibujarBase(g2, 11, 0, new Color(0xD9DDD3));
        dibujarBase(g2, 11, 11, new Color(0xD9DDD3));

        // Recorrido, con la salida de cada jugador en su color
        for (int i = 0; i < TOTAL_CASILLAS; i++) {
            int[] celda = coordenadaCasilla(i);
            Color relleno = Color.WHITE;
            if (i == ColorJugador.ROJO.getCasillaSalida()) {
                relleno = aclarar(ColorJugador.ROJO.getColor());
            } else if (i == ColorJugador.AZUL.getCasillaSalida()) {
                relleno = aclarar(ColorJugador.AZUL.getColor());
            }
            pintarCelda(g2, celda[0], celda[1], relleno);
        }

        // Pasillos hacia la meta
        for (int i = 0; i < 7; i++) {
            int[] celdaRoja = coordenadaPasillo(ColorJugador.ROJO, i);
            int[] celdaAzul = coordenadaPasillo(ColorJugador.AZUL, i);
            pintarCelda(g2, celdaRoja[0], celdaRoja[1], aclarar(ColorJugador.ROJO.getColor()));
            pintarCelda(g2, celdaAzul[0], celdaAzul[1], aclarar(ColorJugador.AZUL.getColor()));
            // Los pasillos de los colores que no juegan
            pintarCelda(g2, 9, 17 - i, new Color(0xE7EAE3));
            pintarCelda(g2, 17 - i, 9, new Color(0xE7EAE3));
        }

        // Meta: el cuadro de 3 x 3 del centro
        g2.setColor(new Color(0x14233A));
        g2.fillRect(origenX + 8 * tamCelda, origenY + 8 * tamCelda, 3 * tamCelda, 3 * tamCelda);
        g2.setColor(new Color(0xA9B6C6));
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(8, tamCelda * 3 / 10)));
        FontMetrics medidas = g2.getFontMetrics();
        g2.drawString("META", origenX + 10 * tamCelda + tamCelda / 2 - medidas.stringWidth("META") / 2,
                origenY + 10 * tamCelda + tamCelda / 2 + medidas.getAscent() / 2 - 1);

        // Fichas
        dibujarFichas(g2, partida.getJugadorRojo());
        dibujarFichas(g2, partida.getJugadorAzul());
    }

    private void dibujarBase(Graphics2D g2, int fila, int columna, Color color) {
        g2.setColor(color);
        g2.fillRect(origenX + columna * tamCelda, origenY + fila * tamCelda, 8 * tamCelda, 8 * tamCelda);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(origenX + (columna + 1) * tamCelda, origenY + (fila + 1) * tamCelda,
                6 * tamCelda, 6 * tamCelda, tamCelda, tamCelda);
    }

    private void pintarCelda(Graphics2D g2, int fila, int columna, Color relleno) {
        int x = origenX + columna * tamCelda;
        int y = origenY + fila * tamCelda;
        g2.setColor(relleno);
        g2.fillRect(x, y, tamCelda, tamCelda);
        g2.setColor(new Color(0xB9C0B5));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRect(x, y, tamCelda, tamCelda);
    }

    private void dibujarFichas(Graphics2D g2, Jugador jugador) {
        int[] fichas = jugador.getFichas();
        boolean esSuTurno = jugador == partida.getJugadorEnTurno();
        int radio = tamCelda * 2 / 5;
        for (int i = 0; i < fichas.length; i++) {
            int[] centro = centroFicha(jugador.getColor(), i, fichas[i]);

            // Resaltar las fichas que se pueden mover
            boolean movible = esSuTurno && partida.puedeMoverFicha(i);
            if (movible) {
                g2.setColor(new Color(0x2A7A50));
                g2.setStroke(new BasicStroke(3f));
                g2.drawOval(centro[0] - radio - 4, centro[1] - radio - 4, (radio + 4) * 2, (radio + 4) * 2);
            }

            g2.setColor(jugador.getColor().getColor());
            g2.fillOval(centro[0] - radio, centro[1] - radio, radio * 2, radio * 2);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(centro[0] - radio, centro[1] - radio, radio * 2, radio * 2);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(9, radio)));
            String numero = String.valueOf(i + 1);
            FontMetrics medidas = g2.getFontMetrics();
            g2.drawString(numero, centro[0] - medidas.stringWidth(numero) / 2,
                    centro[1] + medidas.getAscent() / 2 - 1);
        }
    }

    private static Color aclarar(Color color) {
        return new Color(
                (color.getRed() + 2 * 255) / 3,
                (color.getGreen() + 2 * 255) / 3,
                (color.getBlue() + 2 * 255) / 3);
    }

    // ------------------------------------------------------------------ geometría del tablero

    /** Fila y columna de la casilla {@code i} del recorrido (0 a 67), en el sentido de las manecillas del reloj. */
    private int[] coordenadaCasilla(int i) {
        if (i <= 7) return new int[] {8, i};
        if (i <= 15) return new int[] {15 - i, 8};
        if (i == 16) return new int[] {0, 9};
        if (i <= 24) return new int[] {i - 17, 10};
        if (i <= 32) return new int[] {8, i - 14};
        if (i == 33) return new int[] {9, 18};
        if (i <= 41) return new int[] {10, 52 - i};
        if (i <= 49) return new int[] {i - 31, 10};
        if (i == 50) return new int[] {18, 9};
        if (i <= 58) return new int[] {69 - i, 8};
        if (i <= 66) return new int[] {10, 66 - i};
        return new int[] {9, 0};
    }

    /** Fila y columna de la casilla {@code i} (0 a 6) del pasillo de un color. */
    private int[] coordenadaPasillo(ColorJugador color, int i) {
        return color == ColorJugador.ROJO ? new int[] {9, 1 + i} : new int[] {1 + i, 9};
    }

    /** Centro en pixeles de una ficha según su color, su número y su posición. */
    private int[] centroFicha(ColorJugador color, int numero, int posicion) {
        double fila;
        double columna;
        if (posicion == -1) {
            // En la base: cuatro lugares dentro del cuadro de su color
            double[][] lugares = {{2.6, 2.6}, {2.6, 5.4}, {5.4, 2.6}, {5.4, 5.4}};
            int columnaBase = color == ColorJugador.ROJO ? 0 : 11;
            fila = lugares[numero][0];
            columna = columnaBase + lugares[numero][1];
        } else if (posicion == Partida.META) {
            // En la meta: cada color ocupa su lado del cuadro central
            int[][] lugares = color == ColorJugador.ROJO
                    ? new int[][] {{9, 8}, {10, 8}, {10, 9}, {9, 9}}
                    : new int[][] {{8, 9}, {8, 10}, {9, 10}, {8, 8}};
            fila = lugares[numero][0] + 0.5;
            columna = lugares[numero][1] + 0.5;
        } else {
            int[] celda = posicion <= 66
                    ? coordenadaCasilla((color.getCasillaSalida() + posicion) % TOTAL_CASILLAS)
                    : coordenadaPasillo(color, posicion - 67);
            // Un pequeño desplazamiento para que dos fichas en la misma casilla se distingan
            double desplazamiento = (numero - 1.5) * 0.12;
            fila = celda[0] + 0.5 + desplazamiento;
            columna = celda[1] + 0.5 + desplazamiento;
        }
        return new int[] {(int) Math.round(origenX + columna * tamCelda), (int) Math.round(origenY + fila * tamCelda)};
    }

    // ------------------------------------------------------------------ eventos del ratón sin usar

    @Override
    public void mouseClicked(MouseEvent e) { }

    @Override
    public void mouseReleased(MouseEvent e) { }

    @Override
    public void mouseEntered(MouseEvent e) { }

    @Override
    public void mouseExited(MouseEvent e) { }
}
