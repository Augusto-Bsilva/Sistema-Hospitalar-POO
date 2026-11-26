public interface Repositorio {
    boolean adicionar(Leito leito);
    boolean remover(int id);
    Leito buscar(int id);
    int tamanho();
    void salvarEmArquivo(String caminhoArquivo);
}
