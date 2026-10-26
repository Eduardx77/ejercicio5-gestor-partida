package mx.itson.parchis;

import java.util.List;

public interface RepositorioEstadisticas {

    void inicializar() throws RepositorioEstadisticasException;

    List<EstadisticaJugador> cargar() throws RepositorioEstadisticasException;

    void guardar(Jugador ganador) throws RepositorioEstadisticasException;

    record EstadisticaJugador(ColorJugador color, int jugadas, int ganadas) {
    }
}
