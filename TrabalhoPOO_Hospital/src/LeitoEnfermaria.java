import java.time.LocalDate;
import java.util.List;

public class LeitoEnfermaria extends Leito {
    
    public LeitoEnfermaria(int idLeito, String nomePaciente, double custoBase, int diasInternacao, LocalDate dataEntrada, String tipo, List<String> sintomas){
        super(idLeito, nomePaciente, custoBase, diasInternacao, dataEntrada, tipo, sintomas);
    }

    @Override 
    public double calcularFaturaTotal(){
        return this.diasInternacao * this.custoBase;
    }
}
