package mx.itson.parchis;

import java.util.Arrays;
import java.util.Objects;

/**
 * Un jugador y el estado de sus cuatro fichas. El color define sus propiedades de juego.
 */
public final class Jugador {

    private final String nombre;
    private final ColorJugador color;
    private final int[] fichas = {-1, -1, -1, -1};

    public Jugador(String nombre, ColorJugador color) {
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.color = Objects.requireNonNull(color, "color");
    }

    public String getNombre() {
        return nombre;
    }

    public ColorJugador getColor() {
        return color;
    }

    public int getCasillaSalida() {
        return color.getCasillaSalida();
    }

    public int getPosicionFicha(int numero) {
        return fichas[numero];
    }

    void setPosicionFicha(int numero, int posicion) {
        fichas[numero] = posicion;
    }

    public int[] getFichas() {
        return Arrays.copyOf(fichas, fichas.length);
    }

    public void reiniciar() {
        Arrays.fill(fichas, -1);
    }
}
