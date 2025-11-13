import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

public class ImagemManagerDialog extends JDialog {

    private final ImagemDao imagemDao;
    private final Nota nota;
    private final DefaultListModel<ImagemNota> imagensModel = new DefaultListModel<>();
    private JList<ImagemNota> listaImagens;

    public ImagemManagerDialog(Frame owner, Nota nota) {
        super(owner, "Imagens da nota: " + nota.getTitulo(), true);
        this.nota = nota;
        this.imagemDao = new ImagemDao();

        initComponents();
        carregarImagens();
    }

    private void initComponents() {
        setSize(600, 400);
        setLocationRelativeTo(getOwner());

        listaImagens = new JList<>(imagensModel);
        JScrollPane scroll = new JScrollPane(listaImagens);
        scroll.setBorder(BorderFactory.createTitledBorder("Imagens anexadas"));

        JButton btnAdicionar = new JButton("Adicionar...");
        JButton btnAbrir = new JButton("Abrir");
        JButton btnRemover = new JButton("Remover");

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botoes.add(btnAdicionar);
        botoes.add(btnAbrir);
        botoes.add(btnRemover);

        getContentPane().setLayout(new BorderLayout(5, 5));
        getContentPane().add(scroll, BorderLayout.CENTER);
        getContentPane().add(botoes, BorderLayout.SOUTH);

        btnAdicionar.addActionListener(this::onAdicionar);
        btnAbrir.addActionListener(this::onAbrir);
        btnRemover.addActionListener(this::onRemover);
    }

    private void carregarImagens() {
        imagensModel.clear();
        List<ImagemNota> lista = imagemDao.listarPorNota(nota.getId());
        for (ImagemNota img : lista) {
            imagensModel.addElement(img);
        }
    }

    private void onAdicionar(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecionar imagem");

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File arquivo = chooser.getSelectedFile();
        if (arquivo == null || !arquivo.exists()) return;

        String descricao = JOptionPane.showInputDialog(
                this,
                "Descrição da imagem (opcional):",
                "Descrição",
                JOptionPane.PLAIN_MESSAGE
        );

        try {
            String caminhoRelativo = imagemDao.copiarParaPastaImagens(arquivo, nota.getId());

            ImagemNota img = new ImagemNota();
            img.setNotaId(nota.getId());
            img.setCaminho(caminhoRelativo);
            img.setDescricao(descricao != null ? descricao.trim() : null);

            imagemDao.salvar(img);
            carregarImagens();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao copiar/registrar imagem:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onAbrir(ActionEvent e) {
        ImagemNota img = listaImagens.getSelectedValue();
        if (img == null) {
            JOptionPane.showMessageDialog(this,
                    "Selecione uma imagem para abrir.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            File f = new File(img.getCaminho());
            if (!f.isAbsolute()) {
                f = new File(new File(".").getCanonicalFile(), img.getCaminho());
            }
            if (!f.exists()) {
                JOptionPane.showMessageDialog(this,
                        "Arquivo de imagem não encontrado:\n" + f.getAbsolutePath(),
                        "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Desktop.getDesktop().open(f);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao abrir imagem:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onRemover(ActionEvent e) {
        ImagemNota img = listaImagens.getSelectedValue();
        if (img == null) {
            JOptionPane.showMessageDialog(this,
                    "Selecione uma imagem para remover.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int resp = JOptionPane.showConfirmDialog(
                this,
                "Remover a imagem selecionada?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (resp != JOptionPane.YES_OPTION) return;

        imagemDao.excluir(img.getId());

        // opcional: remover arquivo físico
        try {
            File f = new File(img.getCaminho());
            if (!f.isAbsolute()) {
                f = new File(new File(".").getCanonicalFile(), img.getCaminho());
            }
            if (f.exists()) {
                // se não quiser apagar o arquivo real, comente esta linha:
                f.delete();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        carregarImagens();
    }
}
