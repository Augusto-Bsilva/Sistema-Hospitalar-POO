import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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

public class LeitorDados {
    private static Leito parseLinha(String linha) {
        String[] partes = linha.split(";");

        int id = Integer.parseInt(partes[0]);
        String nome = partes[1];
        double custoBase = Double.parseDouble(partes[2]);
        int diasInternacao = Integer.parseInt(partes[3]);
        LocalDate dataEntrada = LocalDate.parse(partes[4]);
        List<String> sintomas = Arrays.asList(partes[5].split(","));
        String tipo = partes[6];

        switch (tipo) {
            case "ENFERMARIA":
                return new LeitoEnfermaria(id, nome, custoBase, diasInternacao, dataEntrada, tipo, sintomas);
            case "UTI":
                return new LeitoUTI(id, nome, custoBase, diasInternacao, dataEntrada, tipo, sintomas);
            case "ISOLAMENTO":
                return new LeitoIsolamento(id, nome, custoBase, diasInternacao, dataEntrada, tipo, sintomas);
            case "NEONATAL":
                return new LeitoNeonatal(id, nome, custoBase, diasInternacao, dataEntrada, tipo, sintomas);
            default:
                throw new IllegalArgumentException("Tipo de leito desconhecido: " + tipo);
        }
    }

    // Versão Sequencial
    public static List<Leito> lerArquivoSequencial(String caminhoDoArquivo) {
        List<Leito> leitos = new ArrayList<>();

        try (BufferedReader leitura = new BufferedReader(new FileReader(caminhoDoArquivo))) {
            String linha;
            while ((linha = leitura.readLine()) != null) {
                leitos.add(parseLinha(linha));
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo: " + e.getMessage());
        }

        return leitos;
    }

    // Versão Concorrente (Leitura sequencial do disco + Parsing paralelo por blocos)
    public static List<Leito> lerArquivoConcorrente(String caminhoDoArquivo, int numThreads) {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        try {
            //Leitura sequencial das linhas do disco para a RAM
            List<String> todasAsLinhas = Files.readAllLines(Paths.get(caminhoDoArquivo));
            int totalLinhas = todasAsLinhas.size();
            int tamanhoBloco = totalLinhas / numThreads;

            List<Callable<List<Leito>>> tarefas = new ArrayList<>();

            // 2. Parsing paralelo em blocos
            for (int t = 0; t < numThreads; t++) {
                final int inicio = t * tamanhoBloco;
                final int fim = (t == numThreads - 1) ? totalLinhas : inicio + tamanhoBloco;

                tarefas.add(() -> {
                    List<Leito> blocoLeitos = new ArrayList<>(fim - inicio);
                    for (int i = inicio; i < fim; i++) {
                        blocoLeitos.add(parseLinha(todasAsLinhas.get(i)));
                    }
                    return blocoLeitos;
                });
            }

            //Junção dos resultados em uma única coleção polimórfica
            List<Future<List<Leito>>> resultados = executor.invokeAll(tarefas);
            List<Leito> leitos = new ArrayList<>(totalLinhas);

            for (Future<List<Leito>> futuro : resultados) {
                leitos.addAll(futuro.get());
            }

            return leitos;

        } catch (Exception e) {
            System.err.println("Erro na leitura concorrente: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            executor.shutdown();
        }
    }
}
