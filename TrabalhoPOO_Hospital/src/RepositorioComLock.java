import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RepositorioComLock implements Repositorio {
    private final Map<Integer, Leito> dados;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public RepositorioComLock(List<Leito> listaInicial) {
        this.dados = new HashMap<>(listaInicial.size());
        for (Leito leito : listaInicial) {
            this.dados.put(leito.getidLeito(), leito);
        }
    }

    @Override
    public boolean adicionar(Leito leito) {
        lock.writeLock().lock();
        try {
            if (dados.containsKey(leito.getidLeito())) {
                return false; 
            }
            dados.put(leito.getidLeito(), leito);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remover(int id) {
        lock.writeLock().lock();
        try {
            return dados.remove(id) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Leito buscar(int id) {
        lock.readLock().lock();
        try {
            return dados.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int tamanho() {
        lock.readLock().lock();
        try {
            return dados.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void salvarEmArquivo(String caminhoArquivo) {
        lock.readLock().lock();
        try (BufferedWriter escrita = new BufferedWriter(new FileWriter(caminhoArquivo))) {
            for (Leito l : dados.values()) {
                String sintomasFormatados = String.join(",", l.getSintomas());
                String linha = String.format(Locale.US, "%d;%s;%.2f;%d;%s;%s;%s",
                        l.getidLeito(),
                        l.getNomePaciente(),
                        l.getCustoBase(),
                        l.getDiasInternacao(),
                        l.getDataEntrada().toString(),
                        sintomasFormatados,
                        l.getTipo());
                escrita.write(linha);
                escrita.newLine();
            }
        } catch (IOException e) {
            System.err.println("Erro ao salvar arquivo: " + e.getMessage());
        } finally {
            lock.readLock().unlock();
        }
    }
}
