package mx.itson.parchis;

import java.awt.Color;

public enum ColorJugador {
    ROJO("rojo", 1, new Color(0xC23B2E)),
    AZUL("azul", 18, new Color(0x2563A8));

    private final String nombre;
    private final int casillaSalida;
    private final Color color;

    ColorJugador(String nombre, int casillaSalida, Color color) {
        this.nombre = nombre;
        this.casillaSalida = casillaSalida;
        this.color = color;
    }

    public String getNombre() {
        return nombre;
    }

    public int getCasillaSalida() {
        return casillaSalida;
    }

    public Color getColor() {
        return color;
    }

    public ColorJugador rival() {
        return this == ROJO ? AZUL : ROJO;
    }
}
