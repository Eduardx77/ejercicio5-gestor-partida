package mx.itson.parchis;

public class RepositorioEstadisticasException extends Exception {

    private static final long serialVersionUID = 1L;

    public RepositorioEstadisticasException(String message, Throwable cause) {
        super(message + ": " + cause.getMessage(), cause);
    }
}
