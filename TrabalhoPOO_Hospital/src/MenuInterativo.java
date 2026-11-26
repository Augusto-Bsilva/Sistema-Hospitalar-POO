import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class MenuInterativo {
    public static void iniciar(Repositorio repo) {
        Scanner scanner = new Scanner(System.in);
        int opcao = -1;

        while (opcao != 0) {
            System.out.println("\n=== MENU SISTEMA HOSPITALAR (Registros atuais: " + repo.tamanho() + ") ===");
            System.out.println("1. Buscar leito por ID");
            System.out.println("2. Adicionar novo leito");
            System.out.println("3. Remover leito por ID");
            System.out.println("4. Salvar repositorio em novo arquivo");
            System.out.println("5. Executar simulacao concorrente (N clientes)");
            System.out.println("0. Sair");
            System.out.print("Escolha uma opcao: ");

            try {
                opcao = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Digite um numero valido.");
                continue;
            }

            switch (opcao) {
                case 1:
                    System.out.print("Digite o ID do leito: ");
                    int idBusca = Integer.parseInt(scanner.nextLine());
                    long tBusca = System.nanoTime();
                    Leito encontrado = repo.buscar(idBusca);
                    double msBusca = (System.nanoTime() - tBusca) / 1_000_000.0;

                    if (encontrado != null) {
                        System.out.printf("Encontrado: ID %d | %s | %s | Sintomas: %s | Fatura: R$ %.2f%n",
                                encontrado.getidLeito(), encontrado.getNomePaciente(),
                                encontrado.getTipo(), encontrado.getSintomas(), encontrado.calcularFaturaTotal());
                    } else {
                        System.out.println("Leito nao encontrado.");
                    }
                    System.out.printf("Tempo da busca: %.4f ms%n", msBusca);
                    break;

                case 2:
                    System.out.print("ID do novo leito (ex: 1000001): ");
                    int idNovo = Integer.parseInt(scanner.nextLine());
                    System.out.print("Nome do paciente: ");
                    String nome = scanner.nextLine();
                    System.out.print("Custo base (ex: 250.50): ");
                    double custo = Double.parseDouble(scanner.nextLine().replace(",", "."));
                    System.out.print("Dias de internacao: ");
                    int dias = Integer.parseInt(scanner.nextLine());
                    System.out.print("Tipo (ENFERMARIA, UTI, ISOLAMENTO, NEONATAL): ");
                    String tipo = scanner.nextLine().trim().toUpperCase();
                    System.out.print("Sintomas separados por virgula (ex: Febre,Tosse): ");
                    List<String> sintomas = Arrays.asList(scanner.nextLine().split(","));

                    Leito novo = criarLeitoPorTipo(idNovo, nome, custo, dias, LocalDate.now(), tipo, sintomas);
                    if (novo == null) {
                        System.out.println("Tipo invalido.");
                        break;
                    }

                    long tAdd = System.nanoTime();
                    boolean adicionado = repo.adicionar(novo);
                    double msAdd = (System.nanoTime() - tAdd) / 1_000_000.0;

                    System.out.println(adicionado ? "Leito adicionado com sucesso!" : "Erro: ID ja existente.");
                    System.out.printf("Tempo da adicao: %.4f ms%n", msAdd);
                    break;

                case 3:
                    System.out.print("Digite o ID para remover: ");
                    int idRem = Integer.parseInt(scanner.nextLine());
                    long tRem = System.nanoTime();
                    boolean removido = repo.remover(idRem);
                    double msRem = (System.nanoTime() - tRem) / 1_000_000.0;

                    System.out.println(removido ? "Leito removido com sucesso!" : "ID nao encontrado.");
                    System.out.printf("Tempo da remocao: %.4f ms%n", msRem);
                    break;

                case 4:
                    String caminhoSaida = "dados/gigante_atualizado.txt";
                    System.out.println("Salvando em " + caminhoSaida + "...");
                    long tSave = System.nanoTime();
                    repo.salvarEmArquivo(caminhoSaida);
                    double segSave = (System.nanoTime() - tSave) / 1_000_000_000.0;
                    System.out.printf("Arquivo salvo! Tempo: %.3f segundos%n", segSave);
                    break;

                case 5:
                    System.out.print("Quantidade de clientes (threads, ex: 8): ");
                    int clientes = Integer.parseInt(scanner.nextLine());
                    System.out.print("Operacoes por cliente (ex: 50000): ");
                    int ops = Integer.parseInt(scanner.nextLine());
                    SimuladorConcorrente.executarSimulacao(repo, clientes, ops);
                    break;

                case 0:
                    System.out.println("Encerrando o sistema...");
                    break;

                default:
                    System.out.println("Opcao invalida.");
            }
        }
        scanner.close();
    }

    private static Leito criarLeitoPorTipo(int id, String nome, double custo, int dias,
                                           LocalDate data, String tipo, List<String> sintomas) {
        switch (tipo) {
            case "ENFERMARIA": return new LeitoEnfermaria(id, nome, custo, dias, data, tipo, sintomas);
            case "UTI":        return new LeitoUTI(id, nome, custo, dias, data, tipo, sintomas);
            case "ISOLAMENTO": return new LeitoIsolamento(id, nome, custo, dias, data, tipo, sintomas);
            case "NEONATAL":   return new LeitoNeonatal(id, nome, custo, dias, data, tipo, sintomas);
            default:           return null;
        }
    }
}
