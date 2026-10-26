package mx.itson.parchis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class EscuchaJugadasSocket {

    private EscuchaJugadasSocket() {
    }

    public static void escuchar() {
        System.out.println("Esperando jugadas en el puerto " + ConfiguracionRed.PUERTO + "...");
        try (ServerSocket servidor = new ServerSocket(ConfiguracionRed.PUERTO)) {
            while (true) {
                try (Socket cliente = servidor.accept();
                        BufferedReader entrada = new BufferedReader(
                                new InputStreamReader(cliente.getInputStream(), StandardCharsets.UTF_8))) {
                    String linea;
                    while ((linea = entrada.readLine()) != null) {
                        System.out.println("Llegó: " + linea);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("No se pudo abrir el puerto " + ConfiguracionRed.PUERTO + ": " + e.getMessage());
        }
    }
}
