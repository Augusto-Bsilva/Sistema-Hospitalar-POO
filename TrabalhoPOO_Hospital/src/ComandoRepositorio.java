import java.util.Optional;
import org.apache.pekko.actor.typed.ActorRef;

public interface ComandoRepositorio {
    final class Adicionar implements ComandoRepositorio {
        public final Leito leito;
        public final ActorRef<Boolean> replyTo;

        public Adicionar(Leito leito, ActorRef<Boolean> replyTo) {
            this.leito = leito;
            this.replyTo = replyTo;
        }
    }

    final class Remover implements ComandoRepositorio {
        public final int id;
        public final ActorRef<Boolean> replyTo;

        public Remover(int id, ActorRef<Boolean> replyTo) {
            this.id = id;
            this.replyTo = replyTo;
        }
    }

    final class Buscar implements ComandoRepositorio {
        public final int id;
        public final ActorRef<Optional<Leito>> replyTo;

        public Buscar(int id, ActorRef<Optional<Leito>> replyTo) {
            this.id = id;
            this.replyTo = replyTo;
        }
    }

    final class Tamanho implements ComandoRepositorio {
        public final ActorRef<Integer> replyTo;

        public Tamanho(ActorRef<Integer> replyTo) {
            this.replyTo = replyTo;
        }
    }

    final class Salvar implements ComandoRepositorio {
        public final String caminhoArquivo;
        public final ActorRef<Boolean> replyTo;

        public Salvar(String caminhoArquivo, ActorRef<Boolean> replyTo) {
            this.caminhoArquivo = caminhoArquivo;
            this.replyTo = replyTo;
        }
    }
}
