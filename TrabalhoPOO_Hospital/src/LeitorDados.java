import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Classe responsável pela Etapa 2: leitura dos arquivos de texto e reconstrução
 * polimórfica dos objetos da hierarquia {@link Leito}.
 */
public class LeitorDados {

    private static Leito parseLinha(String linha) {
        String[] partes = linha.split(";");
        if (partes.length < 7) {
            throw new IllegalArgumentException("Linha malformada (esperado 7 colunas): " + linha);
        }

        int id = Integer.parseInt(partes[0]);
        String nome = partes[1];
        double custoBase = Double.parseDouble(partes[2]);
        int diasInternacao = Integer.parseInt(partes[3]);
        LocalDate dataEntrada = LocalDate.parse(partes[4]);
        List<String> sintomas = new ArrayList<>(Arrays.asList(partes[5].split(",")));
        String tipo = partes[6];

        return LeitoFactory.criar(id, nome, custoBase, diasInternacao, dataEntrada, tipo, sintomas);
    }

    // Versão Sequencial
    public static List<Leito> lerArquivoSequencial(String caminhoDoArquivo) {
        List<Leito> leitos = new ArrayList<>();

        try (BufferedReader leitura = new BufferedReader(new FileReader(caminhoDoArquivo))) {
            String linha;
            while ((linha = leitura.readLine()) != null) {
                if (!linha.isBlank()) {
                    leitos.add(parseLinha(linha));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao ler o arquivo: " + caminhoDoArquivo, e);
        }

        return leitos;
    }

    // Versão Concorrente (Leitura sequencial do disco + Parsing paralelo por blocos)
    public static List<Leito> lerArquivoConcorrente(String caminhoDoArquivo, int numThreads) {
        if (numThreads <= 0) {
            throw new IllegalArgumentException("Numero de threads deve ser positivo: " + numThreads);
        }

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        try {
            // 1. Leitura sequencial das linhas do disco para a RAM
            List<String> todasAsLinhas = Files.readAllLines(Paths.get(caminhoDoArquivo));
            int totalLinhas = todasAsLinhas.size();
            int tamanhoBloco = totalLinhas / numThreads;

            List<Callable<List<Leito>>> tarefas = new ArrayList<>(numThreads);

            // 2. Parsing paralelo em blocos
            for (int t = 0; t < numThreads; t++) {
                final int inicio = t * tamanhoBloco;
                final int fim = (t == numThreads - 1) ? totalLinhas : inicio + tamanhoBloco;

                tarefas.add(() -> {
                    List<Leito> blocoLeitos = new ArrayList<>(Math.max(0, fim - inicio));
                    for (int i = inicio; i < fim; i++) {
                        String linha = todasAsLinhas.get(i);
                        if (!linha.isBlank()) {
                            blocoLeitos.add(parseLinha(linha));
                        }
                    }
                    return blocoLeitos;
                });
            }

            // 3. Junção dos resultados em uma única coleção polimórfica
            List<Future<List<Leito>>> resultados = executor.invokeAll(tarefas);
            List<Leito> leitos = new ArrayList<>(totalLinhas);

            for (Future<List<Leito>> futuro : resultados) {
                leitos.addAll(futuro.get());
            }

            return leitos;

        } catch (IOException e) {
            throw new UncheckedIOException("Erro de I/O na leitura concorrente: " + caminhoDoArquivo, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Leitura concorrente interrompida.", e);
        } catch (Exception e) {
            throw new RuntimeException("Erro na leitura concorrente: " + e.getMessage(), e);
        } finally {
            executor.shutdown();
        }
    }
}
