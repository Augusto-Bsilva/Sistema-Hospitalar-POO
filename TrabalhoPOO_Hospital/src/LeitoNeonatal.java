import java.time.LocalDate;
import java.util.List;

public class LeitoNeonatal extends Leito {
    private double taxaIncubadora = 250.0;
    
    public LeitoNeonatal(int idLeito, String nomePaciente, double custoBase, int diasInternacao, LocalDate dataEntrada, String tipo,List<String> sintomas){
        super(idLeito, nomePaciente, custoBase, diasInternacao, dataEntrada, tipo,sintomas);
    }
    @Override
    public double calcularFaturaTotal(){
        return (this.diasInternacao * this.custoBase) + taxaIncubadora;
    }
}
