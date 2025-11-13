import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ImagemDao {

    public ImagemDao() {
        inicializarTabela();
    }

    private void inicializarTabela() {
        String sql = """
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
            st.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<ImagemNota> listarPorNota(int notaId) {
        List<ImagemNota> lista = new ArrayList<>();
        String sql = "SELECT id, nota_id, caminho, descricao FROM imagens WHERE nota_id = ? ORDER BY id";

        try (Connection conn = ConexaoSQLite.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, notaId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ImagemNota img = new ImagemNota();
                    img.setId(rs.getInt("id"));
                    img.setNotaId(rs.getInt("nota_id"));
                    img.setCaminho(rs.getString("caminho"));
                    img.setDescricao(rs.getString("descricao"));
                    lista.add(img);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }

    public void salvar(ImagemNota img) {
        String sql = "INSERT INTO imagens (nota_id, caminho, descricao) VALUES (?, ?, ?)";

        try (Connection conn = ConexaoSQLite.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, img.getNotaId());
            ps.setString(2, img.getCaminho());
            ps.setString(3, img.getDescricao());
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void excluir(int id) {
        String sql = "DELETE FROM imagens WHERE id = ?";

        try (Connection conn = ConexaoSQLite.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // utilitário: copia o arquivo escolhido para data/images e devolve o caminho relativo
    public String copiarParaPastaImagens(java.io.File origem, int notaId) throws Exception {
        Path imagesDir = Paths.get("data", "images").toAbsolutePath();
        if (!Files.exists(imagesDir)) {
            Files.createDirectories(imagesDir);
        }

        String nomeOriginal = origem.getName();
        String ext = "";
        int idx = nomeOriginal.lastIndexOf('.');
        if (idx != -1) {
            ext = nomeOriginal.substring(idx);
        }

        String novoNome = "nota-" + notaId + "-" + System.currentTimeMillis() + ext;
        Path destino = imagesDir.resolve(novoNome);

        Files.copy(origem.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);

        // caminho que vamos guardar no banco (relativo pra ficar portátil)
        return "data/images/" + novoNome;
    }
}
