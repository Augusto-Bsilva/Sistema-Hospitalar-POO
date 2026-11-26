import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SimuladorConcorrente {
    public static void executarSimulacao(Repositorio repo, int numClientes, int operacoesPorCliente) {
        int tamanhoInicial = repo.tamanho();
        // Gera IDs novos acima de 2 milhões para nunca colidir com os IDs iniciais (1 a 1.000.000)
        AtomicInteger geradorNovoId = new AtomicInteger(2_000_000);
        AtomicInteger totalAdicionados = new AtomicInteger(0);
        AtomicInteger totalRemovidos = new AtomicInteger(0);
        AtomicInteger totalBuscas = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(numClientes);

        System.out.printf("\n=== INICIANDO SIMULACAO (%d clientes x %d ops = %d ops) ===%n",
                numClientes, operacoesPorCliente, numClientes * operacoesPorCliente);

        long inicio = System.nanoTime();

        for (int c = 0; c < numClientes; c++) {
            executor.submit(() -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                List<String> sintomasPadrao = Arrays.asList("Febre", "Tosse");

                for (int i = 0; i < operacoesPorCliente; i++) {
                    int sorteio = random.nextInt(100);

                    if (sorteio < 60) {
                        // 60% Buscas
                        int idBusca = random.nextInt(1, tamanhoInicial + 1);
                        repo.buscar(idBusca);
                        totalBuscas.incrementAndGet();

                    } else if (sorteio < 80) {
                        // 20% Adições
                        int novoId = geradorNovoId.incrementAndGet();
                        Leito novoLeito = new LeitoUTI(
                                novoId, "Paciente Simulado", 350.0, 5, LocalDate.now(), "UTI", sintomasPadrao);
                        if (repo.adicionar(novoLeito)) {
                            totalAdicionados.incrementAndGet();
                        }

                    } else {
                        // 20% Remoções
                        int idRemover = random.nextInt(1, tamanhoInicial + 1);
                        if (repo.remover(idRemover)) {
                            totalRemovidos.incrementAndGet();
                        }
                    }
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Simulação interrompida.");
        }

        double tempoSegundos = (System.nanoTime() - inicio) / 1_000_000_000.0;
        int tamanhoFinal = repo.tamanho();
        int tamanhoEsperado = tamanhoInicial + totalAdicionados.get() - totalRemovidos.get();
        boolean consistente = (tamanhoFinal == tamanhoEsperado);

        System.out.println("--- RESULTADO DA SIMULACAO ---");
        System.out.printf("Tempo total: %.3f segundos%n", tempoSegundos);
        System.out.printf("Buscas realizadas: %d | Adicoes efetivas: %d | Remocoes efetivas: %d%n",
                totalBuscas.get(), totalAdicionados.get(), totalRemovidos.get());
        System.out.printf("Registros -> Inicial: %d | Esperado: %d | Final Real: %d%n",
                tamanhoInicial, tamanhoEsperado, tamanhoFinal);
        System.out.println("Consistencia validada? " + (consistente ? "SIM (OK)" : "NAO (FALHA DE CONCORRENCIA!)"));
    }
}
