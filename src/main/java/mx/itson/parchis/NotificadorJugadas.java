package mx.itson.parchis;

import java.io.IOException;

public interface NotificadorJugadas {

    void enviar(String mensaje) throws IOException;
}
