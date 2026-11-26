import java.time.LocalDate;
import java.util.List;

public class LeitoIsolamento extends Leito {
    
    private double taxaEpiDiaria = 85.0;

     public LeitoIsolamento(int idLeito, String nomePaciente, double custoBase, int diasInternacao, LocalDate dataEntrada, String tipo,List<String> sintomas){
        super(idLeito, nomePaciente, custoBase, diasInternacao, dataEntrada, tipo,sintomas);
    }

    @Override 
    public double calcularFaturaTotal(){
        double custoNormal =  this.diasInternacao * this.custoBase;
        double custoEpis = this.diasInternacao * taxaEpiDiaria;
        return custoNormal + custoEpis;
    }
}
