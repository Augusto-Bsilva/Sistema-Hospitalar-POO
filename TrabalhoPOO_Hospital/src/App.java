import java.io.File;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class App {

    private static final int AQUECIMENTO = 2;
    private static final int REPETICOES = 5;
    private static final int[] THREADS_TESTE = {1, 2, 4, 8};

    // Tamanhos dos arquivos da Etapa 1 (ajuste LINHAS_GIGANTE se desejar tempos mais longos)
    private static final int LINHAS_PEQUENO = 1_000;
    private static final int LINHAS_MEDIO = 10_000;
    private static final int LINHAS_GRANDE = 100_000;
    private static final int LINHAS_GIGANTE = 1_000_000;

    // Carga fixa total na Etapa 3 (dividida entre 1, 2, 4 e 8 clientes para medir speedup real)
    private static final int TOTAL_OPS_ETAPA3 = 400_000;
    private static final String ARQUIVO_GIGANTE = "dados/gigante_conc.txt";

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        Scanner scanner = new Scanner(System.in);

        System.out.println("====================================================");
        System.out.println("      SISTEMA DE GESTAO HOSPITALAR - POO / JAVA     ");
        System.out.println("====================================================");
        System.out.println("1. Executar Benchmark Etapa 1 (Geracao de Dados)");
        System.out.println("2. Executar Benchmark Etapa 2 (Leitura e Parsing)");
        System.out.println("3. Executar Benchmark Etapa 3 (Sequencial x Locks x Atores) + Menu");
        System.out.println("4. Executar TODOS os Benchmarks (Etapas 1, 2 e 3)");
        System.out.println("5. Abrir diretamente o Menu Interativo");
        System.out.print("Escolha uma opcao [4]: ");

        String escolha = scanner.nextLine().trim();
        switch (escolha) {
            case "1":
                executarEtapa1();
                break;
            case "2":
                executarEtapa2();
                break;
            case "3":
                executarEtapa3(true, scanner);
                break;
            case "5":
                garantirArquivoGigante();
                List<Leito> base = LeitorDados.lerArquivoConcorrente(ARQUIVO_GIGANTE, 4);
                MenuInterativo.iniciarComSelecao(base, scanner);
                break;
            case "4":
            case "":
            default:
                executarEtapa1();
                executarEtapa2();
                executarEtapa3(false, scanner);
                break;
        }

        scanner.close();
    }

    // =========================================================================
    // ETAPA 1: GERACAO DE DADOS (4 tamanhos + Sequencial vs Concorrente 1,2,4,8)
    // =========================================================================
    public static void executarEtapa1() {
        System.out.println("\n====================================================");
        System.out.println("  ETAPA 1: GERACAO DE DADOS (AQUECIMENTO + " + REPETICOES + " REPS)");
        System.out.println("====================================================");

        System.out.println("Aquecendo JVM (" + AQUECIMENTO + " execucoes descartadas)...");
        for (int i = 0; i < AQUECIMENTO; i++) {
            GeradorDados.geraArquivoSequencial("dados/temp.txt", 50_000);
            GeradorDados.geraArquivoConcorrente("dados/temp.txt", 50_000, 4);
        }

        System.out.println("\n--- Parte 1A: 4 Tamanhos de Arquivo (Sequencial) ---");
        System.out.printf("%-12s | %-10s | %-18s | %-18s%n",
                "Arquivo", "Linhas", "Media +- DP (ms)", "Media +- DP (s)");
        medirGeracaoTamanho("pequeno.txt", LINHAS_PEQUENO);
        medirGeracaoTamanho("medio.txt", LINHAS_MEDIO);
        medirGeracaoTamanho("grande.txt", LINHAS_GRANDE);
        medirGeracaoTamanho("gigante.txt", LINHAS_GIGANTE);

        System.out.println("\n--- Parte 1B: Arquivo Gigante (" + LINHAS_GIGANTE + " linhas) - Sequencial x Concorrente ---");
        System.out.printf("%-26s | %-18s | %-10s%n", "Abordagem", "Media +- DP (s)", "Speedup");

        double[] temposSeq = new double[REPETICOES];
        for (int r = 0; r < REPETICOES; r++) {
            long inicio = System.nanoTime();
            GeradorDados.geraArquivoSequencial("dados/gigante_seq.txt", LINHAS_GIGANTE);
            temposSeq[r] = (System.nanoTime() - inicio) / 1_000_000_000.0;
        }
        double mediaSeq = calcularMedia(temposSeq);
        double dpSeq = calcularDesvioPadrao(temposSeq, mediaSeq);
        System.out.printf("%-26s | %7.3f +- %-7.3f | 1.00x (Base)%n",
                "Sequencial (Random)", mediaSeq, dpSeq);

        for (int threads : THREADS_TESTE) {
            double[] temposConc = new double[REPETICOES];
            for (int r = 0; r < REPETICOES; r++) {
                long inicio = System.nanoTime();
                GeradorDados.geraArquivoConcorrente(ARQUIVO_GIGANTE, LINHAS_GIGANTE, threads);
                temposConc[r] = (System.nanoTime() - inicio) / 1_000_000_000.0;
            }
            double mediaConc = calcularMedia(temposConc);
            double dpConc = calcularDesvioPadrao(temposConc, mediaConc);
            double speedup = mediaSeq / mediaConc;
            System.out.printf("%-26s | %7.3f +- %-7.3f | %.2fx%n",
                    "Concorrente (" + threads + " thread" + (threads > 1 ? "s" : "") + ")",
                    mediaConc, dpConc, speedup);
        }
    }

    private static void medirGeracaoTamanho(String nomeArquivo, int linhas) {
        String caminho = "dados/" + nomeArquivo;
        double[] temposSeg = new double[REPETICOES];
        for (int r = 0; r < REPETICOES; r++) {
            long inicio = System.nanoTime();
            GeradorDados.geraArquivoSequencial(caminho, linhas);
            temposSeg[r] = (System.nanoTime() - inicio) / 1_000_000_000.0;
        }
        double mediaSeg = calcularMedia(temposSeg);
        double dpSeg = calcularDesvioPadrao(temposSeg, mediaSeg);
        System.out.printf("%-12s | %-10d | %7.2f +- %-7.2f | %7.4f +- %-7.4f%n",
                nomeArquivo, linhas, mediaSeg * 1000.0, dpSeg * 1000.0, mediaSeg, dpSeg);
    }

    // =========================================================================
    // ETAPA 2: LEITURA E CONSTRUCAO POLIMORFICA (Sequencial vs 1, 2, 4, 8 threads)
    // =========================================================================
    public static void executarEtapa2() {
        garantirArquivoGigante();

        System.out.println("\n====================================================");
        System.out.println("  ETAPA 2: LEITURA E PARSING (AQUECIMENTO + " + REPETICOES + " REPS)");
        System.out.println("====================================================");

        System.out.println("Aquecendo JVM (" + AQUECIMENTO + " execucoes descartadas)...");
        String arquivoWarmup = new File("dados/pequeno.txt").exists() ? "dados/pequeno.txt" : ARQUIVO_GIGANTE;
        for (int i = 0; i < AQUECIMENTO; i++) {
            LeitorDados.lerArquivoSequencial(arquivoWarmup);
            LeitorDados.lerArquivoConcorrente(arquivoWarmup, 4);
        }

        System.out.printf("%n%-26s | %-18s | %-10s | %-10s%n",
                "Abordagem", "Media +- DP (s)", "Speedup", "Objetos");

        double[] temposSeq = new double[REPETICOES];
        int objetosSeq = 0;
        for (int r = 0; r < REPETICOES; r++) {
            long inicio = System.nanoTime();
            List<Leito> listaSeq = LeitorDados.lerArquivoSequencial(ARQUIVO_GIGANTE);
            temposSeq[r] = (System.nanoTime() - inicio) / 1_000_000_000.0;
            objetosSeq = listaSeq.size();
            listaSeq = null;
            System.gc();
        }
        double mediaSeq = calcularMedia(temposSeq);
        double dpSeq = calcularDesvioPadrao(temposSeq, mediaSeq);
        System.out.printf("%-26s | %7.3f +- %-7.3f | 1.00x      | %d%n",
                "Sequencial (BufferedReader)", mediaSeq, dpSeq, objetosSeq);

        List<Leito> amostraPolimorfismo = null;
        for (int threads : THREADS_TESTE) {
            double[] temposConc = new double[REPETICOES];
            int objetosConc = 0;
            for (int r = 0; r < REPETICOES; r++) {
                long inicio = System.nanoTime();
                List<Leito> listaConc = LeitorDados.lerArquivoConcorrente(ARQUIVO_GIGANTE, threads);
                temposConc[r] = (System.nanoTime() - inicio) / 1_000_000_000.0;
                objetosConc = listaConc.size();
                if (threads == 8 && r == REPETICOES - 1 && listaConc.size() >= 3) {
                    amostraPolimorfismo = listaConc.subList(0, 3);
                }
                listaConc = null;
                System.gc();
            }
            double mediaConc = calcularMedia(temposConc);
            double dpConc = calcularDesvioPadrao(temposConc, mediaConc);
            double speedup = mediaSeq / mediaConc;
            System.out.printf("%-26s | %7.3f +- %-7.3f | %-10s | %d%n",
                    "Concorrente (" + threads + " thread" + (threads > 1 ? "s" : "") + ")",
                    mediaConc, dpConc, String.format("%.2fx", speedup), objetosConc);
        }

        if (amostraPolimorfismo != null) {
            Formatador fmtTexto = new FormatadorTexto();
            System.out.println("\n--- Prova de Polimorfismo (Leito.calcularFaturaTotal + FormatadorTexto) ---");
            for (Leito l : amostraPolimorfismo) {
                System.out.println("Classe real: " + l.getClass().getSimpleName() + " -> " + fmtTexto.formatarLinha(l));
            }
        }
    }

    // =========================================================================
    // ETAPA 3: OPERACOES CONCORRENTES (Sequencial x Locks x Atores / Pekko)
    // =========================================================================
    public static void executarEtapa3(boolean abrirMenuNoFinal, Scanner scanner) {
        garantirArquivoGigante();

        System.out.println("\n====================================================");
        System.out.println("  ETAPA 3: REPOSITORIO (SEQUENCIAL x LOCKS x ATORES)");
        System.out.println("  Carga fixa: " + TOTAL_OPS_ETAPA3 + " ops | Aquecimento: 1 | Reps: " + REPETICOES);
        System.out.println("====================================================");

        System.out.println("Carregando base de dados (4 threads)...");
        List<Leito> leitos = LeitorDados.lerArquivoConcorrente(ARQUIVO_GIGANTE, 4);
        System.out.println("Base carregada: " + leitos.size() + " registros.");

        // 1. Baseline Sequencial (1 cliente executando todas as 400.000 ops)
        System.out.println("\n--- Parte 3A: Simulacao de Carga (" + TOTAL_OPS_ETAPA3 + " operacoes totais) ---");
        System.out.printf("%-28s | %-18s | %-10s | %-12s | %-12s%n",
                "Implementacao / Clientes", "Media +- DP (s)", "Speedup", "Vazao (op/s)", "Consistencia");

        // Aquecimento Sequencial
        SimuladorConcorrente.executarSimulacao(new RepositorioSequencial(leitos), 1, TOTAL_OPS_ETAPA3, false);

        double[] temposBaseSeq = new double[REPETICOES];
        boolean seqConsistente = true;
        for (int r = 0; r < REPETICOES; r++) {
            Repositorio repoSeq = new RepositorioSequencial(leitos);
            SimuladorConcorrente.ResultadoSimulacao res =
                    SimuladorConcorrente.executarSimulacao(repoSeq, 1, TOTAL_OPS_ETAPA3, false);
            temposBaseSeq[r] = res.tempoSegundos;
            seqConsistente &= res.isTotalmenteConsistente();
        }
        double mediaBaseSeq = calcularMedia(temposBaseSeq);
        double dpBaseSeq = calcularDesvioPadrao(temposBaseSeq, mediaBaseSeq);
        System.out.printf("%-28s | %7.3f +- %-7.3f | 1.00x Base | %12.0f | %s%n",
                "Sequencial (1 cliente)", mediaBaseSeq, dpBaseSeq,
                TOTAL_OPS_ETAPA3 / mediaBaseSeq, seqConsistente ? "OK (100%)" : "FALHA");

        // 2. RepositorioComLock (1, 2, 4, 8 clientes)
        SimuladorConcorrente.executarSimulacao(new RepositorioComLock(leitos), 4, TOTAL_OPS_ETAPA3 / 4, false);
        for (int clientes : THREADS_TESTE) {
            int opsPorCliente = TOTAL_OPS_ETAPA3 / clientes;
            double[] tempos = new double[REPETICOES];
            boolean consistente = true;
            for (int r = 0; r < REPETICOES; r++) {
                Repositorio repoLock = new RepositorioComLock(leitos);
                SimuladorConcorrente.ResultadoSimulacao res =
                        SimuladorConcorrente.executarSimulacao(repoLock, clientes, opsPorCliente, false);
                tempos[r] = res.tempoSegundos;
                consistente &= res.isTotalmenteConsistente();
            }
            double media = calcularMedia(tempos);
            double dp = calcularDesvioPadrao(tempos, media);
            double speedup = mediaBaseSeq / media;
            System.out.printf("%-28s | %7.3f +- %-7.3f | %-10s | %12.0f | %s%n",
                    "Locks (" + clientes + " cliente" + (clientes > 1 ? "s" : "") + ")",
                    media, dp, String.format("%.2fx", speedup),
                    TOTAL_OPS_ETAPA3 / media, consistente ? "OK (100%)" : "FALHA");
        }

        // 3. RepositorioComAtores - Apache Pekko (1, 2, 4, 8 clientes)
        RepositorioComAtores warmupAtores = new RepositorioComAtores(leitos);
        SimuladorConcorrente.executarSimulacao(warmupAtores, 4, TOTAL_OPS_ETAPA3 / 4, false);
        warmupAtores.encerrar();

        for (int clientes : THREADS_TESTE) {
            int opsPorCliente = TOTAL_OPS_ETAPA3 / clientes;
            double[] tempos = new double[REPETICOES];
            boolean consistente = true;
            for (int r = 0; r < REPETICOES; r++) {
                RepositorioComAtores repoAtores = new RepositorioComAtores(leitos);
                try {
                    SimuladorConcorrente.ResultadoSimulacao res =
                            SimuladorConcorrente.executarSimulacao(repoAtores, clientes, opsPorCliente, false);
                    tempos[r] = res.tempoSegundos;
                    consistente &= res.isTotalmenteConsistente();
                } finally {
                    repoAtores.encerrar();
                }
            }
            double media = calcularMedia(tempos);
            double dp = calcularDesvioPadrao(tempos, media);
            double speedup = mediaBaseSeq / media;
            System.out.printf("%-28s | %7.3f +- %-7.3f | %-10s | %12.0f | %s%n",
                    "Atores/Pekko (" + clientes + " cliente" + (clientes > 1 ? "s" : "") + ")",
                    media, dp, String.format("%.2fx", speedup),
                    TOTAL_OPS_ETAPA3 / media, consistente ? "OK (100%)" : "FALHA");
        }

        // 4. Operacoes individuais sobre exatamente a mesma base de 1.000.000 de registros
        System.out.println("\n--- Parte 3B: Operacoes Individuais (Media de " + REPETICOES + " reps sobre " + leitos.size() + " registros) ---");
        System.out.printf("%-22s | %-15s | %-15s | %-15s | %-18s%n",
                "Implementacao", "Adicionar (ms)", "Buscar (ms)", "Remover (ms)", "Salvar CSV (s)");

        medirOperacoesIndividuais("Sequencial", new RepositorioSequencial(leitos), false);
        medirOperacoesIndividuais("Locks (RWLock)", new RepositorioComLock(leitos), false);
        medirOperacoesIndividuais("Atores (Pekko)", new RepositorioComAtores(leitos), true);

        if (abrirMenuNoFinal) {
            MenuInterativo.iniciarComSelecao(leitos, scanner);
        }
    }

    private static void medirOperacoesIndividuais(String rotulo, Repositorio repo, boolean encerrarAtores) {
        try {
            Formatador fmtCsv = new FormatadorCsv();
            double[] tAdd = new double[REPETICOES];
            double[] tBus = new double[REPETICOES];
            double[] tRem = new double[REPETICOES];
            double[] tSal = new double[REPETICOES];

            // Aquecimento rapido das operacoes
            Leito warm = LeitoFactory.criar(9_900_000, "Warmup", 200.0, 2, LocalDate.now(),
                    "ENFERMARIA", Arrays.asList("Febre"));
            repo.adicionar(warm);
            repo.buscar(9_900_000);
            repo.remover(9_900_000);

            for (int r = 0; r < REPETICOES; r++) {
                int idTeste = 9_900_001 + r;
                Leito novo = LeitoFactory.criar(idTeste, "Paciente Teste", 300.0, 4, LocalDate.now(),
                        "UTI", Arrays.asList("Febre", "Tosse"));

                long i1 = System.nanoTime();
                repo.adicionar(novo);
                tAdd[r] = (System.nanoTime() - i1) / 1_000_000.0;

                long i2 = System.nanoTime();
                repo.buscar(idTeste);
                tBus[r] = (System.nanoTime() - i2) / 1_000_000.0;

                long i3 = System.nanoTime();
                repo.remover(idTeste);
                tRem[r] = (System.nanoTime() - i3) / 1_000_000.0;

                long i4 = System.nanoTime();
                repo.salvarEmArquivo("dados/temp_saida_etapa3.csv", fmtCsv);
                tSal[r] = (System.nanoTime() - i4) / 1_000_000_000.0;
            }

            System.out.printf("%-22s | %6.4f +- %.4f | %6.4f +- %.4f | %6.4f +- %.4f | %6.3f +- %.3f%n",
                    rotulo,
                    calcularMedia(tAdd), calcularDesvioPadrao(tAdd, calcularMedia(tAdd)),
                    calcularMedia(tBus), calcularDesvioPadrao(tBus, calcularMedia(tBus)),
                    calcularMedia(tRem), calcularDesvioPadrao(tRem, calcularMedia(tRem)),
                    calcularMedia(tSal), calcularDesvioPadrao(tSal, calcularMedia(tSal)));
        } finally {
            if (encerrarAtores && repo instanceof RepositorioComAtores) {
                ((RepositorioComAtores) repo).encerrar();
            }
        }
    }

    private static void garantirArquivoGigante() {
        File arquivo = new File(ARQUIVO_GIGANTE);
        if (!arquivo.exists()) {
            System.out.println("Arquivo " + ARQUIVO_GIGANTE + " nao encontrado. Gerando com 4 threads...");
            GeradorDados.geraArquivoConcorrente(ARQUIVO_GIGANTE, LINHAS_GIGANTE, 4);
        }
    }

    private static double calcularMedia(double[] valores) {
        double soma = 0.0;
        for (double v : valores) {
            soma += v;
        }
        return soma / valores.length;
    }

    private static double calcularDesvioPadrao(double[] valores, double media) {
        if (valores.length <= 1) {
            return 0.0;
        }
        double somaQuadrados = 0.0;
        for (double v : valores) {
            double diff = v - media;
            somaQuadrados += diff * diff;
        }
        return Math.sqrt(somaQuadrados / (valores.length - 1));
    }
}
