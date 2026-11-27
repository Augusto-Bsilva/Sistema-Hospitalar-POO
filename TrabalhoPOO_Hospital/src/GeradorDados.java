import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Classe responsável pela Etapa 1 do sistema hospitalar: geração massiva de arquivos
 * de texto com registros sintéticos de leitos hospitalares.
 *
 * <p>Cada linha gerada contém 7 colunas delimitadas por ponto e vírgula ({@code ;}):
 * {@code id;nomePaciente;custoBase;diasInternacao;dataEntrada;sintomas;tipo}.</p>
 *
 * <p>Na versão sequencial utiliza {@link Random}; na versão concorrente divide a carga
 * em blocos via {@link ExecutorService}, garantindo um gerador isolado por thread através
 * de {@link ThreadLocalRandom#current()}.</p>
 */
public class GeradorDados {

    private static final String[] NOMES = {
        "Joao Silva", "Maria Oliveira", "Carlos Souza", "Ana Costa",
        "Pedro Santos", "Lucas Almeida", "Juliana Lima"
    };
    private static final String[] TIPOS = {"ENFERMARIA", "UTI", "ISOLAMENTO", "NEONATAL"};
    private static final String[] SINTOMAS = {
        "Gripe", "Febre", "Tosse", "Nausea", "Dor de Cabeca", "Infeccao"
    };

    private static void garantirDiretorioPai(String caminhoDoArquivo) {
        File arquivo = new File(caminhoDoArquivo);
        File pai = arquivo.getParentFile();
        if (pai != null && !pai.exists()) {
            pai.mkdirs();
        }
    }

    /**
     * Gera um arquivo de texto sequencialmente utilizando {@link Random}.
     *
     * @param caminhoDoArquivo caminho do arquivo de saída
     * @param quantidadeLinhas quantidade de registros a gerar (deve ser maior que zero)
     */
    public static void geraArquivoSequencial(String caminhoDoArquivo, int quantidadeLinhas) {
        if (quantidadeLinhas <= 0) {
            throw new IllegalArgumentException("Quantidade de linhas deve ser positiva: " + quantidadeLinhas);
        }
        garantirDiretorioPai(caminhoDoArquivo);
        Random random = new Random();
        LocalDate hoje = LocalDate.now();

        try (BufferedWriter escrita = new BufferedWriter(new FileWriter(caminhoDoArquivo))) {
            for (int i = 1; i <= quantidadeLinhas; i++) {
                String nome = NOMES[random.nextInt(NOMES.length)];
                double custoBase = 150.0 + (random.nextDouble() * 300.0);
                int diasInternacao = random.nextInt(30) + 1;
                LocalDate dataEntrada = hoje.minusDays(random.nextInt(365));
                String tipo = TIPOS[random.nextInt(TIPOS.length)];
                String sintoma1 = SINTOMAS[random.nextInt(SINTOMAS.length)];
                String sintoma2 = SINTOMAS[random.nextInt(SINTOMAS.length)];
                String listaSintomas = sintoma1 + "," + sintoma2;

                String linha = String.format(Locale.US, "%d;%s;%.2f;%d;%s;%s;%s",
                        i, nome, custoBase, diasInternacao, dataEntrada.toString(), listaSintomas, tipo);

                escrita.write(linha);
                escrita.newLine();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao escrever no arquivo: " + caminhoDoArquivo, e);
        }
    }

    /**
     * Gera um arquivo de texto dividindo a criação das linhas em blocos paralelos com
     * {@link ExecutorService} e realizando a escrita final sequencialmente em disco para
     * evitar corrupção por condições de corrida.
     *
     * <p>Cada thread trabalhadora obtém sua própria instância isolada de gerador aleatório
     * por meio de {@link ThreadLocalRandom#current()}, eliminando contenção.</p>
     *
     * @param caminhoDoArquivo caminho do arquivo de saída
     * @param quantidadeLinhas quantidade total de registros a gerar (maior que zero)
     * @param numThreads       quantidade de threads no pool (maior que zero)
     */
    public static void geraArquivoConcorrente(String caminhoDoArquivo, int quantidadeLinhas, int numThreads) {
        if (quantidadeLinhas <= 0) {
            throw new IllegalArgumentException("Quantidade de linhas deve ser positiva: " + quantidadeLinhas);
        }
        if (numThreads <= 0) {
            throw new IllegalArgumentException("Numero de threads deve ser positivo: " + numThreads);
        }
        garantirDiretorioPai(caminhoDoArquivo);

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        int linhasPorThread = quantidadeLinhas / numThreads;
        LocalDate hoje = LocalDate.now();

        List<Callable<List<String>>> tarefas = new ArrayList<>(numThreads);

        for (int t = 0; t < numThreads; t++) {
            final int idInicio = (t * linhasPorThread) + 1;
            final int linhasDestaThread = (t == numThreads - 1)
                    ? quantidadeLinhas - (t * linhasPorThread)
                    : linhasPorThread;

            tarefas.add(() -> {
                List<String> linhasGeradas = new ArrayList<>(linhasDestaThread);
                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (int i = 0; i < linhasDestaThread; i++) {
                    int id = idInicio + i;
                    String nome = NOMES[random.nextInt(NOMES.length)];
                    double custoBase = 150.0 + (random.nextDouble() * 300.0);
                    int diasInternacao = random.nextInt(30) + 1;
                    LocalDate dataEntrada = hoje.minusDays(random.nextInt(365));
                    String tipo = TIPOS[random.nextInt(TIPOS.length)];
                    String sintoma1 = SINTOMAS[random.nextInt(SINTOMAS.length)];
                    String sintoma2 = SINTOMAS[random.nextInt(SINTOMAS.length)];
                    String listaSintomas = sintoma1 + "," + sintoma2;

                    String linha = String.format(Locale.US, "%d;%s;%.2f;%d;%s;%s;%s",
                            id, nome, custoBase, diasInternacao, dataEntrada.toString(), listaSintomas, tipo);
                    linhasGeradas.add(linha);
                }
                return linhasGeradas;
            });
        }

        try (BufferedWriter escrita = new BufferedWriter(new FileWriter(caminhoDoArquivo))) {
            List<Future<List<String>>> resultados = executor.invokeAll(tarefas);

            for (Future<List<String>> resultado : resultados) {
                for (String linha : resultado.get()) {
                    escrita.write(linha);
                    escrita.newLine();
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Erro de I/O na geracao concorrente: " + caminhoDoArquivo, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Geracao concorrente interrompida.", e);
        } catch (Exception e) {
            throw new RuntimeException("Erro na geracao concorrente: " + e.getMessage(), e);
        } finally {
            executor.shutdown();
        }
    }
}
