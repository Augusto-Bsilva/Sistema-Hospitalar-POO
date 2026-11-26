public class App {
    public static void main(String[] args) throws Exception {

        int linhasGigante = 1_000_000;

        System.out.println("Aquecendo a JVM (descartando primeiras execuções)...");
        GeradorDados.geraArquivoSequencial("dados/temp.txt", 50_000);


        System.out.println("\n--- Iniciando Medições (Arquivo Gigante) ---");
        
        //Medição Sequencial
        long inicioSeq = System.nanoTime();
        GeradorDados.geraArquivoSequencial("dados/gigante_seq.txt", linhasGigante);
        long fimSeq = System.nanoTime();
        double tempoSeq = (fimSeq - inicioSeq) / 1_000_000_000.0;
        System.out.printf("Tempo sequencial: %.2f s\n", tempoSeq );

        //Medição Concorrente
        int numThreads = 8;
        long inicioConc = System.nanoTime();
        GeradorDados.geraArquivoConcorrente("dados/gigante_conc.txt", linhasGigante, numThreads);
        long fimConc = System.nanoTime();
        double tempoConc = (fimConc - inicioConc) / 1_000_000_000.0;
        System.out.printf("Tempo Concorrente (%d threads): %.2f s\n", numThreads, tempoConc);

        double speedup = tempoSeq/tempoConc;
        System.out.printf("Speedup: %.2fx mais rápido usando threads!\n", speedup);

        GeradorDados.geraArquivoSequencial("dados/pequeno.txt", 1_000); // 1 mil linhas
        GeradorDados.geraArquivoSequencial("dados/medio.txt", 10_000);  // 10 mil linhas
        GeradorDados.geraArquivoSequencial("dados/grande.txt", 100_000); // 100 mil linhas
        GeradorDados.geraArquivoSequencial("dados/gigante.txt", 1_000_000); // 1 milhão de linhas

        System.out.println("Geração de dados concluída!");
    }
}
