import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotaDao {

    public NotaDao() {
        inicializarBanco();
    }

    private void inicializarBanco() {
        String sqlNotas = """
                CREATE TABLE IF NOT EXISTS notas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    titulo TEXT NOT NULL,
                    texto  TEXT NOT NULL
                )
                """;

        String sqlImagens = """
                CREATE TABLE IF NOT EXISTS imagens (
                    id      INTEGER PRIMARY KEY AUTOINCREMENT,
                    nota_id INTEGER NOT NULL,
                    caminho TEXT    NOT NULL,
                    descricao TEXT,
                    FOREIGN KEY (nota_id) REFERENCES notas(id)
                )
                """;

        try (Connection conn = ConexaoSQLite.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sqlNotas);
            st.execute(sqlImagens);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Agora retorna a própria nota com o ID preenchido
    public Nota salvar(Nota nota) {
        String sql = "INSERT INTO notas (titulo, texto) VALUES (?, ?)";

        try (Connection conn = ConexaoSQLite.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, nota.getTitulo());
            ps.setString(2, nota.getTexto());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    nota.setId(rs.getInt(1));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return nota;
    }

    public void atualizar(Nota nota) {
        String sql = "UPDATE notas SET titulo = ?, texto = ? WHERE id = ?";

        try (Connection conn = ConexaoSQLite.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nota.getTitulo());
            ps.setString(2, nota.getTexto());
            ps.setInt(3, nota.getId());
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void excluir(int id) {
        // Apaga imagens vinculadas à nota e depois a nota
        String sqlImgs = "DELETE FROM imagens WHERE nota_id = ?";
        String sqlNota = "DELETE FROM notas WHERE id = ?";

        try (Connection conn = ConexaoSQLite.getConnection()) {

            try (PreparedStatement psi = conn.prepareStatement(sqlImgs)) {
                psi.setInt(1, id);
                psi.executeUpdate();
            }

            try (PreparedStatement psn = conn.prepareStatement(sqlNota)) {
                psn.setInt(1, id);
                psn.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Nota> buscarPorTermo(String termo) {
        List<Nota> lista = new ArrayList<>();
        String sql = "SELECT id, titulo, texto FROM notas " +
                "WHERE titulo LIKE ? OR texto LIKE ? " +
                "ORDER BY id DESC LIMIT 50";

        String like = "%" + termo + "%";

        try (Connection conn = ConexaoSQLite.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, like);
            ps.setString(2, like);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Nota n = new Nota();
                    n.setId(rs.getInt("id"));
                    n.setTitulo(rs.getString("titulo"));
                    n.setTexto(rs.getString("texto"));
                    lista.add(n);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }

    public List<Nota> listarTodas() {
        List<Nota> lista = new ArrayList<>();
        String sql = "SELECT id, titulo, texto FROM notas ORDER BY id DESC";

        try (Connection conn = ConexaoSQLite.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Nota n = new Nota();
                n.setId(rs.getInt("id"));
                n.setTitulo(rs.getString("titulo"));
                n.setTexto(rs.getString("texto"));
                lista.add(n);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }
}
