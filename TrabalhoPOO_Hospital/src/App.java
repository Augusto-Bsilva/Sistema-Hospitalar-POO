import java.util.List;
public class App {
    public static void main(String[] args) throws Exception {
        // Descomente a linha para usar cada metodo
        
        // executarEtapa1();

        //executarEtapa2();

        executarEtapa3();
    }

    public static void executarEtapa1() {
        int linhasGigante = 1_000_000;

        System.out.println("Aquecendo a JVM (descartando primeiras execuções)...");
        GeradorDados.geraArquivoSequencial("dados/temp.txt", 50_000);

        System.out.println("\n--- Iniciando Medições (Arquivo Gigante) ---");
        
        // Medição Sequencial
        long inicioSeq = System.nanoTime();
        GeradorDados.geraArquivoSequencial("dados/gigante_seq.txt", linhasGigante);
        long fimSeq = System.nanoTime();
        double tempoSeq = (fimSeq - inicioSeq) / 1_000_000_000.0;
        System.out.printf("Tempo sequencial: %.2f s\n", tempoSeq);

        // Medição Concorrente
        int numThreads = 8;
        long inicioConc = System.nanoTime();
        GeradorDados.geraArquivoConcorrente("dados/gigante_conc.txt", linhasGigante, numThreads);
        long fimConc = System.nanoTime();
        double tempoConc = (fimConc - inicioConc) / 1_000_000_000.0;
        System.out.printf("Tempo Concorrente (%d threads): %.2f s\n", numThreads, tempoConc);

        double speedup = tempoSeq / tempoConc;
        System.out.printf("Speedup: %.2fx mais rápido usando threads!\n", speedup);

        GeradorDados.geraArquivoSequencial("dados/pequeno.txt", 1_000);
        GeradorDados.geraArquivoSequencial("dados/medio.txt", 10_000);
        GeradorDados.geraArquivoSequencial("dados/grande.txt", 100_000);
        GeradorDados.geraArquivoSequencial("dados/gigante.txt", 1_000_000);

        System.out.println("Geração de dados concluída!");
    }

    public static void executarEtapa2() {
        String arquivoGigante = "dados/gigante_conc.txt";

        System.out.println("=== AQUECIMENTO JVM (ETAPA 2) ===");
        LeitorDados.lerArquivoSequencial("dados/pequeno.txt");

        System.out.println("\n=== BENCHMARK ETAPA 2 (1 MILHAO DE LINHAS) ===");

        // 1. Teste Sequencial
        long inicioSeq = System.nanoTime();
        List<Leito> listaSeq = LeitorDados.lerArquivoSequencial(arquivoGigante);
        double tempoSeq = (System.nanoTime() - inicioSeq) / 1_000_000_000.0;
        System.out.printf("Sequencial: %.3f segundos | Objetos carregados: %d%n", tempoSeq, listaSeq.size());

        // Libera a referência da memória antes do próximo teste
        listaSeq = null;
        System.gc();

        // 2. Testes Concorrentes (1, 2, 4 e 8 threads)
        int[] threadsTeste = {1, 2, 4, 8};
        for (int threads : threadsTeste) {
            long inicioConc = System.nanoTime();
            List<Leito> listaConc = LeitorDados.lerArquivoConcorrente(arquivoGigante, threads);
            double tempoConc = (System.nanoTime() - inicioConc) / 1_000_000_000.0;
            double speedup = tempoSeq / tempoConc;

            System.out.printf("Concorrente (%d threads): %.3f segundos | Speedup: %.2fx | Objetos: %d%n",
                    threads, tempoConc, speedup, listaConc.size());

            // Na última iteração, exibe 3 exemplos para provar o polimorfismo
            if (threads == 8 && !listaConc.isEmpty()) {
                System.out.println("\n--- Prova de Polimorfismo (Primeiros 3 registros) ---");
                for (int i = 0; i < 3; i++) {
                    Leito l = listaConc.get(i);
                    System.out.printf("ID: %d | Classe real: %s | Sintomas: %s | Fatura: R$ %.2f%n",
                            l.getidLeito(), l.getClass().getSimpleName(), l.getSintomas(), l.calcularFaturaTotal());
                }
            }
        }
    }

    public static void executarEtapa3() {
        System.out.println("Carregando 1 milhao de registros (4 threads - ponto otimo da Etapa 2)...");
        List<Leito> leitos = LeitorDados.lerArquivoConcorrente("dados/gigante_conc.txt", 4);

        Repositorio repositorio = new RepositorioComLock(leitos);
        MenuInterativo.iniciar(repositorio);
    }
}
