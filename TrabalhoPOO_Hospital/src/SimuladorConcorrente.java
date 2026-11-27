import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SimuladorConcorrente {

    private static final String[] TIPOS_LEITO = {"ENFERMARIA", "UTI", "ISOLAMENTO", "NEONATAL"};
    private static final AtomicInteger geradorNovoId = new AtomicInteger(2_000_000);

    public static final class ResultadoSimulacao {
        public final double tempoSegundos;
        public final int totalBuscas;
        public final int totalAdicionados;
        public final int totalRemovidos;
        public final int tamanhoInicial;
        public final int tamanhoEsperado;
        public final int tamanhoFinal;
        public final boolean contagemConsistente;
        public final boolean idsConsistentes;

        public ResultadoSimulacao(double tempoSegundos, int totalBuscas, int totalAdicionados,
                                  int totalRemovidos, int tamanhoInicial, int tamanhoEsperado,
                                  int tamanhoFinal, boolean contagemConsistente, boolean idsConsistentes) {
            this.tempoSegundos = tempoSegundos;
            this.totalBuscas = totalBuscas;
            this.totalAdicionados = totalAdicionados;
            this.totalRemovidos = totalRemovidos;
            this.tamanhoInicial = tamanhoInicial;
            this.tamanhoEsperado = tamanhoEsperado;
            this.tamanhoFinal = tamanhoFinal;
            this.contagemConsistente = contagemConsistente;
            this.idsConsistentes = idsConsistentes;
        }

        public boolean isTotalmenteConsistente() {
            return contagemConsistente && idsConsistentes;
        }
    }

    public static ResultadoSimulacao executarSimulacao(Repositorio repo, int numClientes, int operacoesPorCliente) {
        return executarSimulacao(repo, numClientes, operacoesPorCliente, true);
    }

    public static ResultadoSimulacao executarSimulacao(Repositorio repo, int numClientes,
                                                       int operacoesPorCliente, boolean imprimirDetalhes) {
        if (repo == null) {
            throw new IllegalArgumentException("Repositorio nao pode ser nulo.");
        }
        if (numClientes <= 0 || operacoesPorCliente <= 0) {
            throw new IllegalArgumentException("Clientes e operacoes devem ser positivos.");
        }

        int tamanhoInicial = repo.tamanho();
        int limiteIdBuscaRemocao = Math.max(1, tamanhoInicial);
        AtomicInteger totalAdicionados = new AtomicInteger(0);
        AtomicInteger totalRemovidos = new AtomicInteger(0);
        AtomicInteger totalBuscas = new AtomicInteger(0);

        Set<Integer> idsAdicionados = ConcurrentHashMap.newKeySet();
        Set<Integer> idsRemovidos = ConcurrentHashMap.newKeySet();

        ExecutorService executor = Executors.newFixedThreadPool(numClientes);

        if (imprimirDetalhes) {
            System.out.printf("%n=== INICIANDO SIMULACAO (%d clientes x %d ops = %d ops) ===%n",
                    numClientes, operacoesPorCliente, numClientes * operacoesPorCliente);
        }

        long inicio = System.nanoTime();

        for (int c = 0; c < numClientes; c++) {
            executor.submit(() -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                List<String> sintomasPadrao = Arrays.asList("Febre", "Tosse");
                LocalDate hoje = LocalDate.now();

                for (int i = 0; i < operacoesPorCliente; i++) {
                    int sorteio = random.nextInt(100);

                    if (sorteio < 60) {
                        // 60% Buscas
                        int idBusca = random.nextInt(1, limiteIdBuscaRemocao + 1);
                        repo.buscar(idBusca);
                        totalBuscas.incrementAndGet();

                    } else if (sorteio < 80) {
                        // 20% Adições variando os 4 tipos de subclasse via LeitoFactory
                        int novoId = geradorNovoId.incrementAndGet();
                        String tipo = TIPOS_LEITO[random.nextInt(TIPOS_LEITO.length)];
                        double custo = 150.0 + (random.nextDouble() * 300.0);
                        int dias = random.nextInt(1, 31);
                        Leito novoLeito = LeitoFactory.criar(
                                novoId, "Paciente Simulado", custo, dias, hoje, tipo,
                                new ArrayList<>(sintomasPadrao));
                        if (repo.adicionar(novoLeito)) {
                            totalAdicionados.incrementAndGet();
                            idsAdicionados.add(novoId);
                        }

                    } else {
                        // 20% Remoções
                        int idRemover = random.nextInt(1, limiteIdBuscaRemocao + 1);
                        if (repo.remover(idRemover)) {
                            totalRemovidos.incrementAndGet();
                            idsRemovidos.add(idRemover);
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
            System.err.println("Simulacao interrompida.");
        }

        double tempoSegundos = (System.nanoTime() - inicio) / 1_000_000_000.0;
        int tamanhoFinal = repo.tamanho();
        int tamanhoEsperado = tamanhoInicial + totalAdicionados.get() - totalRemovidos.get();
        boolean contagemConsistente = (tamanhoFinal == tamanhoEsperado);

        // Validação adicional de integridade dos IDs adicionados e removidos (amostra de até 1.000 de cada)
        boolean idsConsistentes = validarAmostraIds(repo, idsAdicionados, idsRemovidos, 1000);

        ResultadoSimulacao resultado = new ResultadoSimulacao(
                tempoSegundos,
                totalBuscas.get(),
                totalAdicionados.get(),
                totalRemovidos.get(),
                tamanhoInicial,
                tamanhoEsperado,
                tamanhoFinal,
                contagemConsistente,
                idsConsistentes
        );

        if (imprimirDetalhes) {
            double vazao = (numClientes * (double) operacoesPorCliente) / tempoSegundos;
            System.out.println("--- RESULTADO DA SIMULACAO ---");
            System.out.printf("Tempo total: %.3f segundos | Vazao: %.0f ops/s%n", tempoSegundos, vazao);
            System.out.printf("Buscas realizadas: %d | Adicoes efetivas: %d | Remocoes efetivas: %d%n",
                    resultado.totalBuscas, resultado.totalAdicionados, resultado.totalRemovidos);
            System.out.printf("Registros -> Inicial: %d | Esperado: %d | Final Real: %d%n",
                    resultado.tamanhoInicial, resultado.tamanhoEsperado, resultado.tamanhoFinal);
            System.out.println("Consistencia matematica: " + (contagemConsistente ? "SIM (OK)" : "NAO (FALHA!)"));
            System.out.println("Integridade de IDs (adicionados presentes / removidos ausentes): "
                    + (idsConsistentes ? "SIM (OK)" : "NAO (FALHA!)"));
        }

        return resultado;
    }

    private static boolean validarAmostraIds(Repositorio repo, Set<Integer> idsAdicionados,
                                             Set<Integer> idsRemovidos, int limiteAmostra) {
        int verificados = 0;
        for (Integer id : idsAdicionados) {
            if (repo.buscar(id) == null) {
                return false;
            }
            if (++verificados >= limiteAmostra) {
                break;
            }
        }
        verificados = 0;
        for (Integer id : idsRemovidos) {
            if (repo.buscar(id) != null) {
                return false;
            }
            if (++verificados >= limiteAmostra) {
                break;
            }
        }
        return true;
    }
}
