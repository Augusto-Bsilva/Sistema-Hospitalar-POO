public interface Repositorio {
    Leito adicionar(Leito l);    
    Leito buscarPorId(int id);
    void remover(int id);
    void salvar();
}
