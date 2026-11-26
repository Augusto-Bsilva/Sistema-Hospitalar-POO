import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Locale;

public class GeradorDados {
    
    private static final String[] NOMES = {"Joao Silva", "Maria Oliveira", "Carlos Souza", "Ana Costa", "Pedro Santos", "Lucas Almeida", "Juliana Lima"};
    private static final String[] TIPOS = {"ENFERMARIA", "UTI", "ISOLAMENTO", "NEONATAL"};
    private static final String[] SINTOMAS = {"Gripe", "Febre", "Tosse", "Nausea", "Dor de Cabeca", "Infeccao"};
    
    public static void geraArquivoSequencial(String caminhoDoArquivo, int quantidadeLinhas){
        Random random = new Random();
        try (BufferedWriter escrita = new BufferedWriter(new FileWriter(caminhoDoArquivo))) {
            for(int i = 1; i<= quantidadeLinhas; i++){
                int id = i;
                String nome = NOMES[random.nextInt(NOMES.length)];
                double custoBase = 150.0 + (random.nextDouble()*300.0);
                int diasInternacao = random.nextInt(30) + 1;
                LocalDate dataEntrada = LocalDate.now().minusDays(random.nextInt(365));
                String tipo = TIPOS[random.nextInt(TIPOS.length)];
                String sintoma1 = SINTOMAS[random.nextInt(SINTOMAS.length)];
                String sintoma2 = SINTOMAS[random.nextInt(SINTOMAS.length)];
                String listaSintomas = sintoma1 + "," + sintoma2;


               String linha = String.format(Locale.US,"%d;%s;%.2f;%d;%s;%s;%s", id, nome, custoBase, diasInternacao, dataEntrada.toString(), listaSintomas, tipo);
                

                escrita.write(linha);
                escrita.newLine();
            }
            System.out.println("Arquivo gerado com sucesso: " + caminhoDoArquivo + " (" + quantidadeLinhas + " linhas)");            
        } catch (IOException e) {
            System.err.println("Erro ao escrever no arquivo: " + e.getMessage());
        }
    }

    public static void geraArquivoConcorrente(String caminhoDoArquivo, int quantidadeLinhas, int numThreads){
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        int linhasPorThread = quantidadeLinhas / numThreads;

        List<Callable<List<String>>> tarefas = new ArrayList<>();

        for(int t = 0; t < numThreads; t++){
            final int idInicio = (t * linhasPorThread) + 1;
            final int linhasDestaThread = (t == numThreads - 1) ? quantidadeLinhas - (t * linhasPorThread) : linhasPorThread;

            tarefas.add(()->{
                List <String> linhasGeradas = new ArrayList<>(linhasDestaThread);
                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (int i = 0; i < linhasDestaThread; i++) {
                    int id = idInicio + i;
                    String nome = NOMES[random.nextInt(NOMES.length)];
                    double custoBase = 150.0 + (random.nextDouble() * 300.0);
                    int diasInternacao = random.nextInt(30) + 1;
                    LocalDate dataEntrada = LocalDate.now().minusDays(random.nextInt(365));
                    String tipo = TIPOS[random.nextInt(TIPOS.length)];
                    String sintoma1 = SINTOMAS[random.nextInt(SINTOMAS.length)];
                    String sintoma2 = SINTOMAS[random.nextInt(SINTOMAS.length)];
                    String listaSintomas = sintoma1 + "," + sintoma2;

                   String linha = String.format(Locale.US,"%d;%s;%.2f;%d;%s;%s;%s",id, nome, custoBase, diasInternacao, dataEntrada.toString(), listaSintomas, tipo);
                   linhasGeradas.add(linha);
                }
                return linhasGeradas;
            });
        }


        try(BufferedWriter escrita = new BufferedWriter(new FileWriter(caminhoDoArquivo))) {
            List<Future<List<String>>> resultados = executor.invokeAll(tarefas);

            for(Future<List<String>> resultado : resultados){
                for(String linha : resultado.get()){
                    escrita.write(linha);
                    escrita.newLine();
                }
            }
        } catch (Exception e) {
            System.err.println("Erro na geração concorrente: " + e.getMessage());
        } finally{
            executor.shutdown();
        }
    }
}
