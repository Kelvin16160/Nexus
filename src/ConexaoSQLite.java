import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexaoSQLite {

    // Banco portátil: data/nexus.db
    private static final String DB_FILE = "data/nexus.db";

    private static String getUrlBanco() {
        try {
            Path dbPath = Paths.get(DB_FILE).toAbsolutePath();
            if (dbPath.getParent() != null && !Files.exists(dbPath.getParent())) {
                Files.createDirectories(dbPath.getParent());
            }
            return "jdbc:sqlite:" + dbPath.toString();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao resolver caminho do banco SQLite", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        String url = getUrlBanco();
        return DriverManager.getConnection(url);
    }
}
