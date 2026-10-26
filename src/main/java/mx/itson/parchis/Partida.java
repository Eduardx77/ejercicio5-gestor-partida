package mx.itson.parchis;

import java.util.ArrayList;
import java.util.List;

/**
 * Estado y reglas de una partida de Parchís para dos jugadores.
 */
public final class Partida {

    public static final int META = 74;
    private static final int TOTAL_CASILLAS = 68;

    private final Jugador rojo = new Jugador("Jugador 1", ColorJugador.ROJO);
    private final Jugador azul = new Jugador("Jugador 2", ColorJugador.AZUL);
    private Jugador jugadorEnTurno = rojo;
    private int dado;
    private boolean yaTiro;
    private int seisSeguidos;
    private boolean terminada;

    public Jugador getJugadorRojo() {
        return rojo;
    }

    public Jugador getJugadorAzul() {
        return azul;
    }

    public Jugador getJugadorEnTurno() {
        return jugadorEnTurno;
    }

    public Jugador getRivalDe(Jugador jugador) {
        return jugador.getColor() == ColorJugador.ROJO ? azul : rojo;
    }

    public int getDado() {
        return dado;
    }

    public boolean yaTiro() {
        return yaTiro;
    }

    public boolean estaTerminada() {
        return terminada;
    }

    public boolean puedeMoverFicha(int numero) {
        if (numero < 0 || numero >= 4 || !yaTiro || terminada) {
            return false;
        }
        int posicion = jugadorEnTurno.getPosicionFicha(numero);
        return (posicion == -1 && dado == 5)
                || (posicion >= 0 && posicion < META && posicion + dado <= META);
    }

    public boolean puedeMoverAlgunaFicha() {
        for (int numero = 0; numero < 4; numero++) {
            if (puedeMoverFicha(numero)) {
                return true;
            }
        }
        return false;
    }

    public ResultadoTirada lanzarDado(int valor) {
        if (terminada || yaTiro) {
            throw new IllegalStateException("No se puede lanzar el dado en este momento.");
        }
        if (valor < 1 || valor > 6) {
            throw new IllegalArgumentException("El dado debe tener un valor entre 1 y 6.");
        }
        dado = valor;
        yaTiro = true;
        seisSeguidos = dado == 6 ? seisSeguidos + 1 : 0;
        if (puedeMoverAlgunaFicha()) {
            return new ResultadoTirada(true, false, false);
        }

        yaTiro = false;
        boolean repetir = dado == 6 && seisSeguidos < 3;
        if (!repetir) {
            cambiarTurno();
        }
        return new ResultadoTirada(false, !repetir, repetir);
    }

    public ResultadoMovimiento moverFicha(int numero) {
        if (!puedeMoverFicha(numero)) {
            throw new IllegalStateException("La ficha seleccionada no se puede mover.");
        }

        int posicionAnterior = jugadorEnTurno.getPosicionFicha(numero);
        int nuevaPosicion = posicionAnterior == -1 ? 0 : posicionAnterior + dado;
        jugadorEnTurno.setPosicionFicha(numero, nuevaPosicion);

        Jugador jugadorQueMovio = jugadorEnTurno;
        Jugador rival = getRivalDe(jugadorQueMovio);
        List<Integer> fichasCapturadas = new ArrayList<>();
        if (nuevaPosicion <= 66) {
            int casilla = (jugadorQueMovio.getCasillaSalida() + nuevaPosicion) % TOTAL_CASILLAS;
            for (int i = 0; i < 4; i++) {
                int posicionRival = rival.getPosicionFicha(i);
                if (posicionRival >= 0 && posicionRival <= 66
                        && (rival.getCasillaSalida() + posicionRival) % TOTAL_CASILLAS == casilla) {
                    rival.setPosicionFicha(i, -1);
                    fichasCapturadas.add(i);
                }
            }
        }

        boolean gano = true;
        for (int posicion : jugadorQueMovio.getFichas()) {
            if (posicion != META) {
                gano = false;
                break;
            }
        }
        if (gano) {
            terminada = true;
            yaTiro = false;
            return new ResultadoMovimiento(jugadorQueMovio, posicionAnterior, nuevaPosicion,
                    fichasCapturadas, true, false);
        }

        yaTiro = false;
        boolean repetir = dado == 6 && seisSeguidos < 3;
        if (!repetir) {
            cambiarTurno();
        }
        return new ResultadoMovimiento(jugadorQueMovio, posicionAnterior, nuevaPosicion,
                fichasCapturadas, false, repetir);
    }

    public void nuevaPartida() {
        rojo.reiniciar();
        azul.reiniciar();
        jugadorEnTurno = rojo;
        dado = 0;
        yaTiro = false;
        seisSeguidos = 0;
        terminada = false;
    }

    private void cambiarTurno() {
        jugadorEnTurno = getRivalDe(jugadorEnTurno);
        seisSeguidos = 0;
    }

    public record ResultadoTirada(boolean puedeMover, boolean cambioTurno, boolean vuelveATirar) {
    }

    public record ResultadoMovimiento(
            Jugador jugador,
            int posicionAnterior,
            int posicionNueva,
            List<Integer> fichasCapturadas,
            boolean gano,
            boolean vuelveATirar) {
        public ResultadoMovimiento {
            fichasCapturadas = List.copyOf(fichasCapturadas);
        }
    }
}
