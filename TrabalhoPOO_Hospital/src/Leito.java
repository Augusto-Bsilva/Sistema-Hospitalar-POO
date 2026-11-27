import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Leito {
    // private: dados cadastrais imutáveis ou controlados estritamente pela superclasse
    private final int idLeito;
    private String nomePaciente;
    private final String tipo;
    private final LocalDate dataEntrada;

    // protected: atributos da regra de negócio acessíveis diretamente pelas subclasses
    protected double custoBase;
    protected int diasInternacao;
    protected List<String> sintomas;

    public Leito(int idLeito, String nomePaciente, double custoBase, int diasInternacao,
                 LocalDate dataEntrada, String tipo, List<String> sintomas) {
        if (idLeito <= 0) {
            throw new IllegalArgumentException("ID do leito deve ser positivo: " + idLeito);
        }
        if (dataEntrada == null) {
            throw new IllegalArgumentException("Data de entrada nao pode ser nula.");
        }
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("Tipo do leito nao pode ser nulo ou vazio.");
        }

        this.idLeito = idLeito;
        this.tipo = tipo.trim().toUpperCase();
        this.dataEntrada = dataEntrada;

        setNomePaciente(nomePaciente);
        setCustoBase(custoBase);
        setDiasInternacao(diasInternacao);
        setSintomas(sintomas);
    }

    public int getidLeito() {
        return idLeito;
    }

    public String getNomePaciente() {
        return nomePaciente;
    }

    public String getTipo() {
        return tipo;
    }

    public double getCustoBase() {
        return custoBase;
    }

    public int getDiasInternacao() {
        return diasInternacao;
    }

    public LocalDate getDataEntrada() {
        return dataEntrada;
    }

    public List<String> getSintomas() {
        return Collections.unmodifiableList(sintomas);
    }

    public void setNomePaciente(String nomePaciente) {
        if (nomePaciente == null || nomePaciente.isBlank()) {
            throw new IllegalArgumentException("Nome do paciente nao pode ser nulo ou vazio.");
        }
        this.nomePaciente = nomePaciente.trim();
    }

    public void setCustoBase(double custoBase) {
        if (custoBase <= 0 || Double.isNaN(custoBase) || Double.isInfinite(custoBase)) {
            throw new IllegalArgumentException("Custo base deve ser um valor positivo: " + custoBase);
        }
        this.custoBase = custoBase;
    }

    public void setDiasInternacao(int diasInternacao) {
        if (diasInternacao <= 0) {
            throw new IllegalArgumentException("Dias de internacao deve ser positivo: " + diasInternacao);
        }
        this.diasInternacao = diasInternacao;
    }

    public void setSintomas(List<String> sintomas) {
        if (sintomas == null || sintomas.isEmpty()) {
            throw new IllegalArgumentException("Lista de sintomas nao pode ser nula ou vazia.");
        }
        List<String> copia = new ArrayList<>(sintomas.size());
        for (String s : sintomas) {
            if (s != null && !s.isBlank()) {
                copia.add(s.trim());
            }
        }
        if (copia.isEmpty()) {
            throw new IllegalArgumentException("Lista de sintomas deve conter ao menos um sintoma valido.");
        }
        this.sintomas = copia;
    }

    public abstract double calcularFaturaTotal();
}
