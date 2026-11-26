import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

public class RepositorioActor extends AbstractBehavior<ComandoRepositorio> {
    private final Map<Integer, Leito> dados;

    public static Behavior<ComandoRepositorio> create(List<Leito> listaInicial) {
        return Behaviors.setup(context -> new RepositorioActor(context, listaInicial));
    }

    private RepositorioActor(ActorContext<ComandoRepositorio> context, List<Leito> listaInicial) {
        super(context);
        this.dados = new HashMap<>(listaInicial.size());
        for (Leito leito : listaInicial) {
            this.dados.put(leito.getidLeito(), leito);
        }
    }

    @Override
    public Receive<ComandoRepositorio> createReceive() {
        return newReceiveBuilder()
                .onMessage(ComandoRepositorio.Adicionar.class, this::onAdicionar)
                .onMessage(ComandoRepositorio.Remover.class, this::onRemover)
                .onMessage(ComandoRepositorio.Buscar.class, this::onBuscar)
                .onMessage(ComandoRepositorio.Tamanho.class, this::onTamanho)
                .onMessage(ComandoRepositorio.Salvar.class, this::onSalvar)
                .build();
    }

    private Behavior<ComandoRepositorio> onAdicionar(ComandoRepositorio.Adicionar cmd) {
        if (dados.containsKey(cmd.leito.getidLeito())) {
            cmd.replyTo.tell(false);
        } else {
            dados.put(cmd.leito.getidLeito(), cmd.leito);
            cmd.replyTo.tell(true);
        }
        return this;
    }

    private Behavior<ComandoRepositorio> onRemover(ComandoRepositorio.Remover cmd) {
        boolean removido = (dados.remove(cmd.id) != null);
        cmd.replyTo.tell(removido);
        return this;
    }

    private Behavior<ComandoRepositorio> onBuscar(ComandoRepositorio.Buscar cmd) {
        cmd.replyTo.tell(Optional.ofNullable(dados.get(cmd.id)));
        return this;
    }

    private Behavior<ComandoRepositorio> onTamanho(ComandoRepositorio.Tamanho cmd) {
        cmd.replyTo.tell(dados.size());
        return this;
    }

    private Behavior<ComandoRepositorio> onSalvar(ComandoRepositorio.Salvar cmd) {
        try (BufferedWriter escrita = new BufferedWriter(new FileWriter(cmd.caminhoArquivo))) {
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
            cmd.replyTo.tell(true);
        } catch (IOException e) {
            System.err.println("Erro ao salvar arquivo no ator: " + e.getMessage());
            cmd.replyTo.tell(false);
        }
        return this;
    }
}
