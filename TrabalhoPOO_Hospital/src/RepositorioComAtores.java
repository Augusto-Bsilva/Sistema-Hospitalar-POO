import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.AskPattern;

public class RepositorioComAtores implements Repositorio {
    private static final AtomicInteger CONTADOR_SISTEMAS = new AtomicInteger(0);

    private final ActorSystem<ComandoRepositorio> sistemaAtores;
    private final Duration timeout = Duration.ofSeconds(15);

    public RepositorioComAtores(List<Leito> listaInicial) {
        String nomeSistema = "SistemaHospitalarPekko-" + CONTADOR_SISTEMAS.incrementAndGet();
        this.sistemaAtores = ActorSystem.create(
                RepositorioActor.create(listaInicial), nomeSistema);
    }

    @Override
    public boolean adicionar(Leito leito) {
        if (leito == null) {
            throw new IllegalArgumentException("Leito nao pode ser nulo.");
        }
        CompletionStage<Boolean> resposta = AskPattern.ask(
                sistemaAtores,
                replyTo -> new ComandoRepositorio.Adicionar(leito, replyTo),
                timeout,
                sistemaAtores.scheduler());
        return resposta.toCompletableFuture().join();
    }

    @Override
    public boolean remover(int id) {
        CompletionStage<Boolean> resposta = AskPattern.ask(
                sistemaAtores,
                replyTo -> new ComandoRepositorio.Remover(id, replyTo),
                timeout,
                sistemaAtores.scheduler());
        return resposta.toCompletableFuture().join();
    }

    @Override
    public Leito buscar(int id) {
        CompletionStage<Optional<Leito>> resposta = AskPattern.ask(
                sistemaAtores,
                replyTo -> new ComandoRepositorio.Buscar(id, replyTo),
                timeout,
                sistemaAtores.scheduler());
        return resposta.toCompletableFuture().join().orElse(null);
    }

    @Override
    public int tamanho() {
        CompletionStage<Integer> resposta = AskPattern.ask(
                sistemaAtores,
                ComandoRepositorio.Tamanho::new,
                timeout,
                sistemaAtores.scheduler());
        return resposta.toCompletableFuture().join();
    }

    @Override
    public void salvarEmArquivo(String caminhoArquivo, Formatador formatador) {
        if (formatador == null) {
            throw new IllegalArgumentException("Formatador nao pode ser nulo.");
        }
        CompletionStage<Boolean> resposta = AskPattern.ask(
                sistemaAtores,
                replyTo -> new ComandoRepositorio.Salvar(caminhoArquivo, formatador, replyTo),
                Duration.ofSeconds(60),
                sistemaAtores.scheduler());
        Boolean ok = resposta.toCompletableFuture().join();
        if (!Boolean.TRUE.equals(ok)) {
            throw new RuntimeException("Falha ao salvar arquivo via ator: " + caminhoArquivo);
        }
    }

    public void encerrar() {
        sistemaAtores.terminate();
        sistemaAtores.getWhenTerminated().toCompletableFuture().join();
    }
}
