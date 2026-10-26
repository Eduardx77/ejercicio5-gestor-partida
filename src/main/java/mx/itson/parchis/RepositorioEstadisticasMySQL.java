package mx.itson.parchis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class RepositorioEstadisticasMySQL implements RepositorioEstadisticas {

    private static final String URL_BD =
            "jdbc:mysql://localhost:3306/parchis?createDatabaseIfNotExist=true&connectTimeout=2000";
    private static final String USUARIO_BD = "root";
    private static final String CLAVE_BD = "";

    @Override
    public void inicializar() throws RepositorioEstadisticasException {
        String sql = "CREATE TABLE IF NOT EXISTS estadisticas ("
                + "color VARCHAR(10) PRIMARY KEY, "
                + "jugadas INT NOT NULL DEFAULT 0, "
                + "ganadas INT NOT NULL DEFAULT 0)";
        try (Connection conexion = conectar(); Statement sentencia = conexion.createStatement()) {
            sentencia.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RepositorioEstadisticasException("No se pudo inicializar la tabla de estadísticas.", e);
        }
    }

    @Override
    public List<EstadisticaJugador> cargar() throws RepositorioEstadisticasException {
        List<EstadisticaJugador> estadisticas = new ArrayList<>();
        String sql = "SELECT color, jugadas, ganadas FROM estadisticas ORDER BY color DESC";
        try (Connection conexion = conectar();
                Statement sentencia = conexion.createStatement();
                ResultSet resultado = sentencia.executeQuery(sql)) {
            while (resultado.next()) {
                ColorJugador color = ColorJugador.valueOf(resultado.getString("color").toUpperCase());
                estadisticas.add(new EstadisticaJugador(
                        color, resultado.getInt("jugadas"), resultado.getInt("ganadas")));
            }
        } catch (SQLException e) {
            throw new RepositorioEstadisticasException("No se pudieron consultar las estadísticas.", e);
        }
        return estadisticas;
    }

    @Override
    public void guardar(Jugador ganador) throws RepositorioEstadisticasException {
        String sql = "INSERT INTO estadisticas (color, jugadas, ganadas) VALUES (?, 1, ?) "
                + "ON DUPLICATE KEY UPDATE jugadas = jugadas + 1, ganadas = ganadas + ?";
        try (Connection conexion = conectar();
                PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            for (ColorJugador color : ColorJugador.values()) {
                int gano = color == ganador.getColor() ? 1 : 0;
                sentencia.setString(1, color.getNombre());
                sentencia.setInt(2, gano);
                sentencia.setInt(3, gano);
                sentencia.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RepositorioEstadisticasException("No se pudieron guardar las estadísticas.", e);
        }
    }

    private Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL_BD, USUARIO_BD, CLAVE_BD);
    }
}
