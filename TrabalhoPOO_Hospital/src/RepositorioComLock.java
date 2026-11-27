import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
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
        if (leito == null) {
            throw new IllegalArgumentException("Leito nao pode ser nulo.");
        }
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
    public void salvarEmArquivo(String caminhoArquivo, Formatador formatador) {
        if (formatador == null) {
            throw new IllegalArgumentException("Formatador nao pode ser nulo.");
        }
        File pai = new File(caminhoArquivo).getParentFile();
        if (pai != null && !pai.exists()) {
            pai.mkdirs();
        }
        lock.readLock().lock();
        try (BufferedWriter escrita = new BufferedWriter(new FileWriter(caminhoArquivo))) {
            for (Leito l : dados.values()) {
                escrita.write(formatador.formatarLinha(l));
                escrita.newLine();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao salvar arquivo: " + caminhoArquivo, e);
        } finally {
            lock.readLock().unlock();
        }
    }
}
