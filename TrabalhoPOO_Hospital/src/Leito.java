import java.time.LocalDate;
import java.util.List;

public abstract class Leito {
    private int idLeito;
    private String nomePaciente;
    private String tipo;
    private double custoBase;
    private int diasInternacao;
    private LocalDate dataEntrada;
    private List<String> sintomas;

    public Leito(int idLeito, String nomePaciente, double custoBase, int diasInternacao, LocalDate dataEntrada, String tipo,List<String> sintomas){
            this.idLeito = idLeito;
            this.nomePaciente = nomePaciente;
            this.tipo = tipo;
            this.custoBase = custoBase;
            this.diasInternacao = diasInternacao;
            this.dataEntrada = dataEntrada;
            this.sintomas = sintomas;
    }
    
    public int getidLeito(){
        return idLeito;
    }   
    public String getNomePaciente(){
        return nomePaciente;
    }    
    public String getTipo(){
        return tipo;
    } 
    public double getCustoBase(){
        return custoBase;
    }
    public int getDiasInternacao(){
        return diasInternacao;
    }
    public LocalDate getDataEntrada(){
        return dataEntrada;
    }
    public List<String> getSintomas() {
        return sintomas;
    }

    public void setidLeito(int idLeito){
        this.idLeito = idLeito;
    }   
    public void setNomePaciente(String nomePaciente){
        this.nomePaciente = nomePaciente;
    }
    public void setTipo(String tipo){
        this.tipo = tipo;
    }
    public void setCustoBase(double custoBase){
        this.custoBase = custoBase;
    }
    public void setDataEntrada(LocalDate dataEntrada){
        this.dataEntrada = dataEntrada;
    }
    public void setSintomas(List<String> sintomas) {
        this.sintomas = sintomas;
    }            
    
    public abstract double calcularFaturaTotal();

}
