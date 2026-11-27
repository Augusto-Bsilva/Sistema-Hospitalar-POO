# Sistema de Gestão Hospitalar — Arquivos, Objetos e Concorrência (Java)

Sistema orientado a objetos em Java desenvolvido para a disciplina de Programação Orientada a Objetos (IC/UFF), abordando geração massiva de dados em arquivo texto, leitura e instanciação polimórfica de objetos em memória e operações concorrentes sobre uma base de **1.000.000 de registros** de leitos hospitalares.

---

## Requisitos

- **Java JDK 21+** (`maven.compiler.source` e `target` configurados para `21`)
- **Apache Maven 3.8+** (ou extensão Java/Maven no VS Code / IntelliJ IDEA)
- Dependências gerenciadas pelo `pom.xml`:
  - `org.apache.pekko:pekko-actor-typed_2.13:1.1.2` (Modelo de Atores — bônus da Etapa 3)
  - `org.slf4j:slf4j-simple:2.0.16` (Logger exigido pelo Pekko)

---

## Como Compilar e Executar

### Via IDE (VS Code / IntelliJ IDEA)
1. Abra a pasta `TrabalhoPOO_Hospital` (onde está o `pom.xml`) na IDE para que as dependências Maven sejam resolvidas automaticamente.
2. Abra `src/App.java` e clique em **Run** no método `main`.
3. No menu inicial exibido no console, escolha a opção desejada:
   - `1` — Benchmark da **Etapa 1** (4 tamanhos de arquivo + Sequencial vs. Concorrente com 1, 2, 4 e 8 threads)
   - `2` — Benchmark da **Etapa 2** (Leitura e parsing Sequencial vs. Concorrente com 1, 2, 4 e 8 threads + prova de polimorfismo)
   - `3` — Benchmark da **Etapa 3** (`RepositorioSequencial` vs. `RepositorioComLock` vs. `RepositorioComAtores` com 1, 2, 4 e 8 clientes + operações individuais + **Menu Interativo**)
   - `4` — Executar **todos os benchmarks** (Etapas 1, 2 e 3) em sequência
   - `5` — Abrir diretamente o **Menu Interativo** (com escolha e troca de implementação de `Repositorio` e `Formatador`)

### Via Maven (Terminal)
```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="App"
```

---

## Estrutura do Projeto

```text
TrabalhoPOO_Hospital/
├── pom.xml                        # Configuração Maven (Java 21 + Apache Pekko Typed)
├── README.md                      # Documentação principal do projeto
├── dados/                         # Arquivos gerados em tempo de execução (ignorados no Git)
└── src/
    ├── Leito.java                 # Superclasse abstrata (private, protected, public + validações)
    ├── LeitoEnfermaria.java       # Subclasse 1 — fatura: dias * custoBase
    ├── LeitoUTI.java              # Subclasse 2 — fatura: (dias * custoBase) * 1.5
    ├── LeitoIsolamento.java       # Subclasse 3 — fatura: (dias * custoBase) + (dias * 85.0)
    ├── LeitoNeonatal.java         # Subclasse 4 — fatura: (dias * custoBase) + 250.0
    ├── LeitoFactory.java          # Fábrica centralizada para instanciar subclasses por tipo
    │
    ├── Formatador.java            # Interface de formatação de registros de Leito
    ├── FormatadorCsv.java         # Implementação 1 — CSV delimitado por ';'
    ├── FormatadorTexto.java       # Implementação 2 — Texto estruturado ('|') com fatura polimórfica
    │
    ├── GeradorDados.java          # Etapa 1 — Geração sequencial (Random) e concorrente (ThreadLocalRandom)
    ├── LeitorDados.java           # Etapa 2 — Leitura sequencial (BufferedReader) e parsing paralelo por blocos
    │
    ├── Repositorio.java           # Interface comum de acesso aos dados (Etapa 3)
    ├── RepositorioSequencial.java # Implementação 1 — HashMap sem locks (baseline sequencial)
    ├── RepositorioComLock.java    # Implementação 2 — HashMap protegido por ReentrantReadWriteLock
    ├── ComandoRepositorio.java    # Interface de mensagens imutáveis (Adicionar, Remover, Buscar, Tamanho, Salvar)
    ├── RepositorioActor.java      # Ator proprietário exclusivo dos dados (sem locks manuais)
    ├── RepositorioComAtores.java  # Implementação 3 — Adaptador Repositorio usando Apache Pekko + AskPattern
    │
    ├── SimuladorConcorrente.java  # Simulador multi-cliente com validação matemática e de integridade de IDs
    ├── MenuInterativo.java        # Interface CLI com medição por operação, troca de Repositorio e Formatador
    └── App.java                   # Orquestrador dos benchmarks (aquecimento + 5 repetições, média, DP e speedup)
```

---

## Destaques de Orientação a Objetos

- **Encapsulamento (3 níveis + validação):** `Leito` utiliza `private final` para dados imutáveis (`idLeito`, `tipo`, `dataEntrada`), `private` com validação em `nomePaciente`, `protected` para os atributos de negócio usados diretamente pelas subclasses (`custoBase`, `diasInternacao`, `sintomas`) e `public` para a API pública, retornando visão imutável de `sintomas`.
- **Herança:** 4 subclasses concretas estendendo a classe abstrata `Leito` (`LeitoEnfermaria`, `LeitoUTI`, `LeitoIsolamento`, `LeitoNeonatal`), além de `RepositorioActor` estendendo `AbstractBehavior<ComandoRepositorio>`.
- **Interfaces com múltiplas implementações:**
  - `Repositorio` → `RepositorioSequencial`, `RepositorioComLock`, `RepositorioComAtores`
  - `Formatador` → `FormatadorCsv`, `FormatadorTexto`
  - `ComandoRepositorio` → `Adicionar`, `Remover`, `Buscar`, `Tamanho`, `Salvar`
- **Polimorfismo:** `LeitorDados` retorna `List<Leito>`; `FormatadorTexto` e `MenuInterativo` invocam `leito.calcularFaturaTotal()` por despacho dinâmico; `MenuInterativo` e `SimuladorConcorrente` dependem exclusivamente da interface `Repositorio`; a persistência delega a serialização à interface `Formatador`.

---

## Resumo dos Resultados das 3 Etapas

Todas as medições em `App.java` utilizam `System.nanoTime()`, com descarte de execuções de aquecimento (JIT warmup), cálculo de **média ± desvio padrão** (5 repetições) e **speedup** para 1, 2, 4 e 8 threads/clientes:

1. **Etapa 1 — Geração de Dados (`1.000`, `10.000`, `100.000` e `1.000.000` de linhas):**
   - Cada thread utiliza `ThreadLocalRandom.current()` isolado para gerar linhas em memória, com escrita final sequencial em disco para evitar corrupção.
   - No arquivo gigante (`1.000.000` de linhas), o tempo caiu de **0,677 ± 0,011 s** (sequencial) para **0,233 ± 0,013 s** com 8 threads, atingindo **speedup de 2,90x**.
2. **Etapa 2 — Leitura e Construção Polimórfica (`1.000.000` de objetos `Leito`):**
   - Arquitetura híbrida: leitura sequencial do disco (`Files.readAllLines`) seguida de parsing paralelo em blocos via `ExecutorService`.
   - O tempo caiu de **0,701 ± 0,039 s** (sequencial) para **0,400 ± 0,014 s** com 4 threads (**1,75x**) e **0,375 ± 0,014 s** com 8 threads (**1,87x**), apresentando retorno decrescente próximo à saturação devido à parcela sequencial de I/O (Lei de Amdahl) e pressão na Heap/GC.
3. **Etapa 3 — Operações Concorrentes (`400.000` operações misturadas: 60% buscas, 20% adições, 20% remoções sobre `1.000.000` de registros):**
   - **Consistência:** **100% validada** (`final = inicial + adicionados - removidos` e integridade amostral de IDs) em todas as configurações de `RepositorioSequencial`, `RepositorioComLock` e `RepositorioComAtores`.
   - **Locks vs. Atores (Pekko):** Com 8 clientes, `RepositorioComLock` (`ReentrantReadWriteLock`) entregou **1.626.985 op/s** (`0,246 ± 0,015 s`), cerca de **4,2x mais vazão** que `RepositorioComAtores` (**390.033 op/s**, `1,026 ± 0,020 s`), que por sua vez apresentou ganho de **2,62x** ao sair de 1 para 8 clientes (efeito de *pipelining* na *mailbox*) eliminando travas manuais. Na persistência de `1.000.000` de registros em CSV, as três abordagens empataram (`0,649 s` a `0,663 s`) por limitação de I/O.

Para análise detalhada, diagrama de classes e tabelas completas, consulte o Relatório Geral.


