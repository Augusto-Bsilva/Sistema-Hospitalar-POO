import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementação sequencial (sem controle de concorrência) da interface {@link Repositorio}.
 * Serve como linha de base (baseline) da Etapa 3 para comparação de desempenho contra
 * as versões concorrentes ({@link RepositorioComLock} e {@link RepositorioComAtores}).
 */
public class RepositorioSequencial implements Repositorio {
    private final Map<Integer, Leito> dados;

    public RepositorioSequencial(List<Leito> listaInicial) {
        this.dados = new HashMap<>(listaInicial.size());
        for (Leito leito : listaInicial) {
            this.dados.put(leito.getidLeito(), leito);
        }
    }

    @Override
    public boolean adicionar(Leito leito) {
        if (leito == null) {
            throw new IllegalArgumentException("Leito nao pode ser nulo.");
        }
        if (dados.containsKey(leito.getidLeito())) {
            return false;
        }
        dados.put(leito.getidLeito(), leito);
        return true;
    }

    @Override
    public boolean remover(int id) {
        return dados.remove(id) != null;
    }

    @Override
    public Leito buscar(int id) {
        return dados.get(id);
    }

    @Override
    public int tamanho() {
        return dados.size();
    }

    @Override
    public void salvarEmArquivo(String caminhoArquivo, Formatador formatador) {
        if (formatador == null) {
            throw new IllegalArgumentException("Formatador nao pode ser nulo.");
        }
        File pai = new File(caminhoArquivo).getParentFile();
        if (pai != null && !pai.exists()) {
            pai.mkdirs();
        }
        try (BufferedWriter escrita = new BufferedWriter(new FileWriter(caminhoArquivo))) {
            for (Leito l : dados.values()) {
                escrita.write(formatador.formatarLinha(l));
                escrita.newLine();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao salvar arquivo: " + caminhoArquivo, e);
        }
    }
}
