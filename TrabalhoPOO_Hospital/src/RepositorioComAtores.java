
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.AskPattern;

public class RepositorioComAtores implements Repositorio {
    private final ActorSystem<ComandoRepositorio> sistemaAtores;
    private final Duration timeout = Duration.ofSeconds(15);

    public RepositorioComAtores(List<Leito> listaInicial) {
        this.sistemaAtores = ActorSystem.create(
                RepositorioActor.create(listaInicial), "SistemaHospitalarPekko");
    }

    @Override
    public boolean adicionar(Leito leito) {
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
    public void salvarEmArquivo(String caminhoArquivo) {
        CompletionStage<Boolean> resposta = AskPattern.ask(
                sistemaAtores,
                replyTo -> new ComandoRepositorio.Salvar(caminhoArquivo, replyTo),
                Duration.ofSeconds(60),
                sistemaAtores.scheduler());
        resposta.toCompletableFuture().join();
    }

    public void encerrar() {
        sistemaAtores.terminate();
    }
}
