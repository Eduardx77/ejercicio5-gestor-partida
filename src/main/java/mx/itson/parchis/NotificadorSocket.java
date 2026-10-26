package mx.itson.parchis;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class NotificadorSocket implements NotificadorJugadas {

    @Override
    public void enviar(String mensaje) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ConfiguracionRed.HOST, ConfiguracionRed.PUERTO), 300);
            PrintWriter salida = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            salida.println(mensaje);
            if (salida.checkError()) {
                throw new IOException("No se pudo enviar el mensaje al espectador.");
            }
        }
    }
}
