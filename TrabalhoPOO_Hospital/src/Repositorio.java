public interface Repositorio {
    boolean adicionar(Leito leito);

    boolean remover(int id);

    Leito buscar(int id);

    int tamanho();

    void salvarEmArquivo(String caminhoArquivo, Formatador formatador);

    default void salvarEmArquivo(String caminhoArquivo) {
        salvarEmArquivo(caminhoArquivo, new FormatadorCsv());
    }
}
