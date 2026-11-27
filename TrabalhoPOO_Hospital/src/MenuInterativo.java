import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class MenuInterativo {

    public static void iniciar(Repositorio repo) {
        iniciar(repo, null, new Scanner(System.in), true);
    }

    public static void iniciarComSelecao(List<Leito> baseInicial, Scanner scanner) {
        Repositorio repoAtual = escolherRepositorio(baseInicial, scanner);
        if (repoAtual == null) {
            return;
        }
        iniciar(repoAtual, baseInicial, scanner, false);
    }

    private static void iniciar(Repositorio repoInicial, List<Leito> baseInicial,
                                Scanner scanner, boolean fecharScannerAoSair) {
        Repositorio repo = repoInicial;
        int opcao = -1;

        while (opcao != 0) {
            System.out.println("\n=== MENU SISTEMA HOSPITALAR ===");
            System.out.println("Implementacao ativa: " + repo.getClass().getSimpleName()
                    + " | Registros atuais: " + repo.tamanho());
            System.out.println("1. Buscar leito por ID");
            System.out.println("2. Adicionar novo leito");
            System.out.println("3. Remover leito por ID");
            System.out.println("4. Salvar repositorio em novo arquivo (escolher Formatador)");
            System.out.println("5. Executar simulacao concorrente (N clientes)");
            if (baseInicial != null) {
                System.out.println("6. Trocar implementacao do repositorio (Sequencial / Locks / Atores)");
            }
            System.out.println("0. Sair");
            System.out.print("Escolha uma opcao: ");

            try {
                opcao = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Entrada invalida. Digite um numero inteiro.");
                continue;
            }

            try {
                switch (opcao) {
                    case 1:
                        System.out.print("Digite o ID do leito: ");
                        int idBusca = Integer.parseInt(scanner.nextLine().trim());
                        long tBusca = System.nanoTime();
                        Leito encontrado = repo.buscar(idBusca);
                        double msBusca = (System.nanoTime() - tBusca) / 1_000_000.0;
                        if (encontrado != null) {
                            System.out.printf("Encontrado: ID %d | %s | %s | Sintomas: %s | Fatura: R$ %.2f%n",
                                    encontrado.getidLeito(), encontrado.getNomePaciente(),
                                    encontrado.getTipo(), encontrado.getSintomas(),
                                    encontrado.calcularFaturaTotal());
                        } else {
                            System.out.println("Leito nao encontrado.");
                        }
                        System.out.printf("Tempo da busca: %.4f ms%n", msBusca);
                        break;

                    case 2:
                        System.out.print("ID do novo leito (ex: 1000001): ");
                        int idNovo = Integer.parseInt(scanner.nextLine().trim());
                        System.out.print("Nome do paciente: ");
                        String nome = scanner.nextLine();
                        System.out.print("Custo base (ex: 250.50): ");
                        double custo = Double.parseDouble(scanner.nextLine().trim().replace(",", "."));
                        System.out.print("Dias de internacao: ");
                        int dias = Integer.parseInt(scanner.nextLine().trim());
                        System.out.print("Tipo (ENFERMARIA, UTI, ISOLAMENTO, NEONATAL): ");
                        String tipo = scanner.nextLine().trim().toUpperCase();
                        System.out.print("Sintomas separados por virgula (ex: Febre,Tosse): ");
                        List<String> sintomas = new ArrayList<>(Arrays.asList(scanner.nextLine().split(",")));

                        Leito novo = LeitoFactory.criar(idNovo, nome, custo, dias, LocalDate.now(), tipo, sintomas);

                        long tAdd = System.nanoTime();
                        boolean adicionado = repo.adicionar(novo);
                        double msAdd = (System.nanoTime() - tAdd) / 1_000_000.0;
                        System.out.println(adicionado ? "Leito adicionado com sucesso!" : "Erro: ID ja existente.");
                        System.out.printf("Tempo da adicao: %.4f ms%n", msAdd);
                        break;

                    case 3:
                        System.out.print("Digite o ID para remover: ");
                        int idRem = Integer.parseInt(scanner.nextLine().trim());
                        long tRem = System.nanoTime();
                        boolean removido = repo.remover(idRem);
                        double msRem = (System.nanoTime() - tRem) / 1_000_000.0;

                        System.out.println(removido ? "Leito removido com sucesso!" : "ID nao encontrado.");
                        System.out.printf("Tempo da remocao: %.4f ms%n", msRem);
                        break;

                    case 4:
                        Formatador formatador = escolherFormatador(scanner);
                        String caminhoSaida = (formatador instanceof FormatadorCsv)
                                ? "dados/gigante_atualizado.csv"
                                : "dados/gigante_atualizado_legivel.txt";
                        System.out.println("Salvando em " + caminhoSaida + " usando " + formatador.getNomeFormato() + "...");
                        long tSave = System.nanoTime();
                        repo.salvarEmArquivo(caminhoSaida, formatador);
                        double segSave = (System.nanoTime() - tSave) / 1_000_000_000.0;
                        System.out.printf("Arquivo salvo (%d registros)! Tempo: %.3f segundos%n",
                                repo.tamanho(), segSave);
                        break;

                    case 5:
                        System.out.print("Quantidade de clientes (threads, ex: 8): ");
                        int clientes = Integer.parseInt(scanner.nextLine().trim());
                        System.out.print("Operacoes por cliente (ex: 50000): ");
                        int ops = Integer.parseInt(scanner.nextLine().trim());
                        if (repo instanceof RepositorioSequencial && clientes > 1) {
                            System.out.println("Aviso: RepositorioSequencial nao possui sincronizacao; usando 1 cliente.");
                            clientes = 1;
                        }
                        SimuladorConcorrente.executarSimulacao(repo, clientes, ops);
                        break;

                    case 6:
                        if (baseInicial == null) {
                            System.out.println("Opcao invalida.");
                            break;
                        }
                        Repositorio novoRepo = escolherRepositorio(baseInicial, scanner);
                        if (novoRepo != null) {
                            encerrarSeAtores(repo);
                            repo = novoRepo;
                        }
                        break;

                    case 0:
                        System.out.println("Encerrando o menu...");
                        break;

                    default:
                        System.out.println("Opcao invalida.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Dados invalidos: " + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("Erro ao executar operacao: " + e.getMessage());
            }
        }

        encerrarSeAtores(repo);
        if (fecharScannerAoSair) {
            scanner.close();
        }
    }

    private static Formatador escolherFormatador(Scanner scanner) {
        System.out.println("Escolha o formato de gravacao:");
        System.out.println("1. CSV (delimitado por ';')");
        System.out.println("2. Texto estruturado (separador '|' com fatura calculada)");
        System.out.print("Opcao [1]: ");
        String entrada = scanner.nextLine().trim();
        if ("2".equals(entrada)) {
            return new FormatadorTexto();
        }
        return new FormatadorCsv();
    }

    private static Repositorio escolherRepositorio(List<Leito> baseInicial, Scanner scanner) {
        System.out.println("\nEscolha a implementacao do Repositorio:");
        System.out.println("1. RepositorioSequencial (sem locks - baseline)");
        System.out.println("2. RepositorioComLock (ReentrantReadWriteLock)");
        System.out.println("3. RepositorioComAtores (Apache Pekko Typed)");
        System.out.print("Opcao [2]: ");
        String entrada = scanner.nextLine().trim();
        switch (entrada) {
            case "1":
                return new RepositorioSequencial(baseInicial);
            case "3":
                return new RepositorioComAtores(baseInicial);
            case "2":
            case "":
            default:
                return new RepositorioComLock(baseInicial);
        }
    }

    private static void encerrarSeAtores(Repositorio repo) {
        if (repo instanceof RepositorioComAtores) {
            ((RepositorioComAtores) repo).encerrar();
        }
    }
}
