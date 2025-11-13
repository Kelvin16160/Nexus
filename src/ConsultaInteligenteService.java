import java.util.List;

public class ConsultaInteligenteService {

    private final NotaDao notaDao;
    private final OpenAIClient openAIClient;
    private final ImagemDao imagemDao;

    public ConsultaInteligenteService(NotaDao notaDao, OpenAIClient openAIClient, ImagemDao imagemDao) {
        this.notaDao = notaDao;
        this.openAIClient = openAIClient;
        this.imagemDao = imagemDao;
    }

    public String consultar(String perguntaUsuario) throws Exception {
        List<Nota> notas = notaDao.buscarPorTermo(perguntaUsuario);

        StringBuilder contexto = new StringBuilder();
        contexto.append("Pergunta do usuário: ").append(perguntaUsuario).append("\n\n");
        contexto.append("Base de conhecimento (notas cadastradas):\n\n");

        if (notas.isEmpty()) {
            contexto.append("Nenhuma nota relevante encontrada. Mesmo assim, tente orientar o usuário dizendo que não há registros.\n");
        } else {
            for (Nota n : notas) {
                contexto.append("Título: ").append(n.getTitulo()).append("\n");
                contexto.append("Texto:\n").append(n.getTexto()).append("\n");

                // Imagens dessa nota
                List<ImagemNota> imagens = imagemDao.listarPorNota(n.getId());
                if (!imagens.isEmpty()) {
                    contexto.append("Imagens ligadas a esta nota:\n");
                    for (ImagemNota img : imagens) {
                        contexto.append(" - Descrição: ")
                                .append(img.getDescricao() != null && !img.getDescricao().isBlank()
                                        ? img.getDescricao()
                                        : "(sem descrição)")
                                .append(" | Caminho local: ")
                                .append(img.getCaminho())
                                .append("\n");
                    }
                }
                contexto.append("\n");
            }
        }

        String promptFinal = """
                Você é um assistente treinado em cima dessas notas e descrições de imagens.
                Use APENAS essas informações para responder.
                Quando for útil, mencione as imagens pelas descrições (por exemplo: "na imagem de X..." ),
                mas não invente imagens que não foram listadas.
                Explique de forma clara, organizada, em tópicos se fizer sentido.
                Se faltar informação, deixe isso explícito.
                
                %s
                """.formatted(contexto.toString());

        return openAIClient.perguntar(promptFinal);
    }

    public List<Nota> listarNotasParaExibicao(String termo) {
        if (termo == null || termo.isBlank()) {
            return notaDao.listarTodas();
        }
        return notaDao.buscarPorTermo(termo);
    }

    // Salva nota e retorna com ID preenchido
    public Nota salvarNota(String titulo, String texto) {
        Nota n = new Nota();
        n.setTitulo(titulo);
        n.setTexto(texto);
        return notaDao.salvar(n);
    }

    public void atualizarNota(int id, String titulo, String texto) {
        Nota n = new Nota();
        n.setId(id);
        n.setTitulo(titulo);
        n.setTexto(texto);
        notaDao.atualizar(n);
    }

    public void excluirNota(int id) {
        notaDao.excluir(id);
    }
}
