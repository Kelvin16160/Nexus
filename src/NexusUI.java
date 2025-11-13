import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

public class NexusUI extends JFrame {

    private final ConsultaInteligenteService service;
    private final ImagemDao imagemDao;
    private final DefaultListModel<Nota> notasModel = new DefaultListModel<>();
    private final DefaultListModel<ImagemNota> imagensConsultaModel = new DefaultListModel<>();

    private JTextField txtPergunta;
    private JTextArea txtResposta;
    private JList<Nota> listaNotas;
    private JList<ImagemNota> listaImagensConsulta;
    private JLabel lblPreviewImagem;
    private JButton btnConsultar;
    private JButton btnReset;
    private JButton btnImagens;

    public NexusUI() {
        NotaDao notaDao = new NotaDao();
        OpenAIClient openAIClient = new OpenAIClient();
        this.imagemDao = new ImagemDao();
        this.service = new ConsultaInteligenteService(notaDao, openAIClient, imagemDao);

        initComponents();
        carregarNotas("");
    }

    private void initComponents() {
        setTitle("Nexus – Base de Conhecimento Inteligente");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Janela maior
        setSize(1200, 750);
        setLocationRelativeTo(null);

        // ============ TOPO ============

        JPanel topo = new JPanel(new BorderLayout(8, 8));

        txtPergunta = new JTextField();
        btnConsultar = new JButton("Consultar IA");
        btnReset = new JButton("Reset");

        JPanel painelDireitaTopo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        painelDireitaTopo.add(btnReset);
        painelDireitaTopo.add(btnConsultar);

        topo.add(new JLabel("Pergunta:"), BorderLayout.WEST);
        topo.add(txtPergunta, BorderLayout.CENTER);
        topo.add(painelDireitaTopo, BorderLayout.EAST);

        // ============ LISTA DE NOTAS (ESQUERDA) ============

        listaNotas = new JList<>(notasModel);
        JScrollPane scrollNotas = new JScrollPane(listaNotas);
        scrollNotas.setBorder(BorderFactory.createTitledBorder("Notas"));

        // ============ PAINEL DIREITO (RESPOSTA + IMAGENS) ============

        // Texto de resposta / conteúdo
        txtResposta = new JTextArea();
        txtResposta.setLineWrap(true);
        txtResposta.setWrapStyleWord(true);
        txtResposta.setEditable(false);
        JScrollPane scrollResposta = new JScrollPane(txtResposta);
        scrollResposta.setBorder(BorderFactory.createTitledBorder("Resposta / Texto da nota"));

        // Preview de imagem (bem maior)
        lblPreviewImagem = new JLabel("Pré-visualização da imagem", SwingConstants.CENTER);
        lblPreviewImagem.setPreferredSize(new Dimension(800, 400));
        lblPreviewImagem.setOpaque(true);
        lblPreviewImagem.setBackground(Color.WHITE);
        lblPreviewImagem.setBorder(BorderFactory.createTitledBorder("Imagem selecionada"));

        // Lista de imagens (altura menor)
        listaImagensConsulta = new JList<>(imagensConsultaModel);
        JScrollPane scrollImagens = new JScrollPane(listaImagensConsulta);
        scrollImagens.setBorder(BorderFactory.createTitledBorder("Imagens relacionadas"));
        scrollImagens.setPreferredSize(new Dimension(200, 120));

        // Painel de imagens (preview + lista)
        JPanel painelImagens = new JPanel(new BorderLayout(5, 5));
        painelImagens.add(lblPreviewImagem, BorderLayout.CENTER);
        painelImagens.add(scrollImagens, BorderLayout.SOUTH);

        // Painel direito completo (texto em cima, imagens embaixo)
        JPanel painelDireito = new JPanel(new BorderLayout(5, 5));
        painelDireito.add(scrollResposta, BorderLayout.CENTER);
        painelDireito.add(painelImagens, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollNotas, painelDireito);
        split.setResizeWeight(0.3);

        // ============ RODAPÉ ============

        JButton btnAdicionarNota = new JButton("Adicionar Nota");
        JButton btnEditarNota = new JButton("Editar Nota");
        JButton btnExcluirNota = new JButton("Excluir Nota");
        btnImagens = new JButton("Imagens...");

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rodape.add(btnAdicionarNota);
        rodape.add(btnEditarNota);
        rodape.add(btnExcluirNota);
        rodape.add(btnImagens);

        // ============ LAYOUT PRINCIPAL ============

        getContentPane().setLayout(new BorderLayout(8, 8));
        getContentPane().add(topo, BorderLayout.NORTH);
        getContentPane().add(split, BorderLayout.CENTER);
        getContentPane().add(rodape, BorderLayout.SOUTH);

        // ============ AÇÕES ============

        btnConsultar.addActionListener(this::onConsultar);
        btnReset.addActionListener(this::onReset);
        btnAdicionarNota.addActionListener(this::onAdicionarNota);
        btnEditarNota.addActionListener(this::onEditarNota);
        btnExcluirNota.addActionListener(this::onExcluirNota);
        btnImagens.addActionListener(this::onImagens);

        // Selecionar nota → mostra texto + imagens dessa nota
        listaNotas.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Nota selecionada = listaNotas.getSelectedValue();
                if (selecionada != null) {
                    txtResposta.setText(selecionada.getTexto());
                    carregarImagensDaNota(selecionada);
                }
            }
        });

        // Selecionar imagem → mostrar preview
        listaImagensConsulta.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ImagemNota img = listaImagensConsulta.getSelectedValue();
                if (img != null) {
                    exibirImagemNaPreview(img);
                }
            }
        });

        // Duplo clique na imagem → abre imagem em janela grande com scroll
        listaImagensConsulta.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    abrirImagemEmDialogSelecionada();
                }
            }
        });
    }

    // ============ CONSULTA ============

    private void onConsultar(ActionEvent e) {
        String pergunta = txtPergunta.getText().trim();
        if (pergunta.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Digite uma pergunta.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        txtResposta.setText("Consultando IA, aguarde...");
        imagensConsultaModel.clear();
        limparPreviewImagem();
        carregarNotas(pergunta); // filtra notas relacionadas
        btnConsultar.setEnabled(false);

        new Thread(() -> {
            try {
                String resposta = service.consultar(pergunta);
                SwingUtilities.invokeLater(() -> {
                    txtResposta.setText(resposta);
                    carregarImagensDaConsulta(pergunta);
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                            "Erro ao consultar IA:\n" + ex.getMessage(),
                            "Erro", JOptionPane.ERROR_MESSAGE);
                    txtResposta.setText("");
                    imagensConsultaModel.clear();
                    limparPreviewImagem();
                });
            } finally {
                SwingUtilities.invokeLater(() -> btnConsultar.setEnabled(true));
            }
        }).start();
    }

    private void onReset(ActionEvent e) {
        txtPergunta.setText("");
        txtResposta.setText("");
        listaNotas.clearSelection();
        imagensConsultaModel.clear();
        limparPreviewImagem();
        carregarNotas("");
    }

    // ============ CARREGAR LISTAS ============

    private void carregarNotas(String termo) {
        notasModel.clear();
        List<Nota> notas = service.listarNotasParaExibicao(termo);
        for (Nota n : notas) {
            notasModel.addElement(n);
        }
    }

    // Imagens de todas as notas relacionadas à consulta
    private void carregarImagensDaConsulta(String termo) {
        imagensConsultaModel.clear();
        limparPreviewImagem();

        List<Nota> notas = service.listarNotasParaExibicao(termo);
        for (Nota n : notas) {
            List<ImagemNota> imgs = imagemDao.listarPorNota(n.getId());
            for (ImagemNota img : imgs) {
                imagensConsultaModel.addElement(img);
            }
        }

        if (!imagensConsultaModel.isEmpty()) {
            listaImagensConsulta.setSelectedIndex(0);
            exibirImagemNaPreview(imagensConsultaModel.get(0));
        }
    }

    // Imagens só da nota selecionada
    private void carregarImagensDaNota(Nota nota) {
        imagensConsultaModel.clear();
        limparPreviewImagem();

        if (nota == null) return;

        List<ImagemNota> imgs = imagemDao.listarPorNota(nota.getId());
        for (ImagemNota img : imgs) {
            imagensConsultaModel.addElement(img);
        }

        if (!imagensConsultaModel.isEmpty()) {
            listaImagensConsulta.setSelectedIndex(0);
            exibirImagemNaPreview(imagensConsultaModel.get(0));
        }
    }

    // ============ VISUALIZAÇÃO DE IMAGENS ============

    private void exibirImagemNaPreview(ImagemNota img) {
        try {
            File f = resolverArquivoImagem(img.getCaminho());
            if (!f.exists()) {
                lblPreviewImagem.setText("Imagem não encontrada:\n" + f.getAbsolutePath());
                lblPreviewImagem.setIcon(null);
                return;
            }

            ImageIcon original = new ImageIcon(f.getAbsolutePath());
            if (original.getIconWidth() <= 0 || original.getIconHeight() <= 0) {
                lblPreviewImagem.setText("Não foi possível carregar a imagem.");
                lblPreviewImagem.setIcon(null);
                return;
            }

            int maxW = lblPreviewImagem.getWidth() > 0 ? lblPreviewImagem.getWidth() : 800;
            int maxH = lblPreviewImagem.getHeight() > 0 ? lblPreviewImagem.getHeight() : 400;

            double scaleX = (double) maxW / original.getIconWidth();
            double scaleY = (double) maxH / original.getIconHeight();
            double scale = Math.min(scaleX, scaleY);

            int newW = (int) (original.getIconWidth() * scale);
            int newH = (int) (original.getIconHeight() * scale);

            Image scaled = original.getImage().getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
            lblPreviewImagem.setIcon(new ImageIcon(scaled));
            lblPreviewImagem.setText(null);
        } catch (Exception ex) {
            ex.printStackTrace();
            lblPreviewImagem.setText("Erro ao exibir imagem.");
            lblPreviewImagem.setIcon(null);
        }
    }

    private void limparPreviewImagem() {
        lblPreviewImagem.setText("Pré-visualização da imagem");
        lblPreviewImagem.setIcon(null);
    }

    private File resolverArquivoImagem(String caminho) throws Exception {
        File f = new File(caminho);
        if (!f.isAbsolute()) {
            f = new File(new File(".").getCanonicalFile(), caminho);
        }
        return f;
    }

    // Abre a imagem selecionada em uma janela grande com scroll
    private void abrirImagemEmDialogSelecionada() {
        ImagemNota img = listaImagensConsulta.getSelectedValue();
        if (img == null) return;

        try {
            File f = resolverArquivoImagem(img.getCaminho());
            if (!f.exists()) {
                JOptionPane.showMessageDialog(this,
                        "Arquivo de imagem não encontrado:\n" + f.getAbsolutePath(),
                        "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ImageIcon icon = new ImageIcon(f.getAbsolutePath());
            JLabel lbl = new JLabel(icon);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);

            JScrollPane scroll = new JScrollPane(lbl);

            JDialog dlg = new JDialog(this, "Visualizar imagem", true);
            dlg.getContentPane().add(scroll);
            dlg.setSize(1000, 700);
            dlg.setLocationRelativeTo(this);
            dlg.setVisible(true);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao abrir imagem:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ============ CRUD DE NOTAS ============

    private void onAdicionarNota(ActionEvent e) {
        JTextField txtTitulo = new JTextField();
        JTextArea txtTexto = new JTextArea(10, 30);
        txtTexto.setLineWrap(true);
        txtTexto.setWrapStyleWord(true);

        JScrollPane scrollTexto = new JScrollPane(txtTexto);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel("Título:"), BorderLayout.NORTH);
        panel.add(txtTitulo, BorderLayout.CENTER);
        panel.add(scrollTexto, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Nova Nota",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String titulo = txtTitulo.getText().trim();
            String texto = txtTexto.getText().trim();

            if (titulo.isEmpty() || texto.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Título e texto são obrigatórios.",
                        "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Nota nova = service.salvarNota(titulo, texto);
            carregarNotas("");

            int resp = JOptionPane.showConfirmDialog(
                    this,
                    "Deseja anexar imagens para esta nota agora?",
                    "Anexar imagens",
                    JOptionPane.YES_NO_OPTION
            );

            if (resp == JOptionPane.YES_OPTION) {
                adicionarImagensParaNota(nova);
                carregarImagensDaNota(nova);
            }
        }
    }

    private void adicionarImagensParaNota(Nota nota) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecionar imagens");
        chooser.setMultiSelectionEnabled(true);

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File[] arquivos = chooser.getSelectedFiles();
        if (arquivos == null || arquivos.length == 0) return;

        for (File arquivo : arquivos) {
            if (arquivo == null || !arquivo.exists()) continue;

            String descricao = JOptionPane.showInputDialog(
                    this,
                    "Descrição para a imagem:\n" + arquivo.getName(),
                    "Descrição da imagem",
                    JOptionPane.PLAIN_MESSAGE
            );

            try {
                String caminhoRelativo = imagemDao.copiarParaPastaImagens(arquivo, nota.getId());

                ImagemNota img = new ImagemNota();
                img.setNotaId(nota.getId());
                img.setCaminho(caminhoRelativo);
                img.setDescricao(descricao != null ? descricao.trim() : null);

                imagemDao.salvar(img);

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Erro ao anexar imagem " + arquivo.getName() + ":\n" + ex.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onEditarNota(ActionEvent e) {
        Nota selecionada = listaNotas.getSelectedValue();
        if (selecionada == null) {
            JOptionPane.showMessageDialog(this,
                    "Selecione uma nota para editar.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField txtTitulo = new JTextField(selecionada.getTitulo());
        JTextArea txtTexto = new JTextArea(selecionada.getTexto(), 10, 30);
        txtTexto.setLineWrap(true);
        txtTexto.setWrapStyleWord(true);

        JScrollPane scrollTexto = new JScrollPane(txtTexto);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel("Título:"), BorderLayout.NORTH);
        panel.add(txtTitulo, BorderLayout.CENTER);
        panel.add(scrollTexto, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Editar Nota",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String novoTitulo = txtTitulo.getText().trim();
            String novoTexto = txtTexto.getText().trim();

            if (novoTitulo.isEmpty() || novoTexto.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Título e texto são obrigatórios.",
                        "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            service.atualizarNota(selecionada.getId(), novoTitulo, novoTexto);
            carregarNotas("");

            Nota notaAtualizada = new Nota();
            notaAtualizada.setId(selecionada.getId());
            notaAtualizada.setTitulo(novoTitulo);
            notaAtualizada.setTexto(novoTexto);

            int resp = JOptionPane.showConfirmDialog(
                    this,
                    "Deseja anexar novas imagens para esta nota agora?",
                    "Anexar imagens",
                    JOptionPane.YES_NO_OPTION
            );

            if (resp == JOptionPane.YES_OPTION) {
                adicionarImagensParaNota(notaAtualizada);
                carregarImagensDaNota(notaAtualizada);
            } else {
                carregarImagensDaNota(notaAtualizada);
            }
        }
    }

    private void onExcluirNota(ActionEvent e) {
        Nota selecionada = listaNotas.getSelectedValue();
        if (selecionada == null) {
            JOptionPane.showMessageDialog(this,
                    "Selecione uma nota para excluir.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int resp = JOptionPane.showConfirmDialog(
                this,
                "Tem certeza que deseja excluir a nota:\n\"" + selecionada.getTitulo() + "\"?",
                "Confirmar exclusão",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (resp == JOptionPane.YES_OPTION) {
            service.excluirNota(selecionada.getId());
            carregarNotas("");
            txtResposta.setText("");
            imagensConsultaModel.clear();
            limparPreviewImagem();
        }
    }

    private void onImagens(ActionEvent e) {
        Nota selecionada = listaNotas.getSelectedValue();
        if (selecionada == null) {
            JOptionPane.showMessageDialog(this,
                    "Selecione uma nota para gerenciar imagens.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ImagemManagerDialog dlg = new ImagemManagerDialog(this, selecionada);
        dlg.setVisible(true);
        carregarImagensDaNota(selecionada);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            new NexusUI().setVisible(true);
        });
    }
}
