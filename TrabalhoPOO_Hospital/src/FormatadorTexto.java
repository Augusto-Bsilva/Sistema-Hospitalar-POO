import java.util.Locale;

/**
 * Formata registros de {@link Leito} em texto estruturado com separador pipe ('|'),
 * incluindo o cálculo polimórfico da fatura total de cada especialidade de leito.
 */
public class FormatadorTexto implements Formatador {

    @Override
    public String formatarLinha(Leito leito) {
        if (leito == null) {
            throw new IllegalArgumentException("Leito nao pode ser nulo.");
        }
        String sintomasFormatados = String.join(", ", leito.getSintomas());
        return String.format(Locale.US,
                "ID: %d | Paciente: %s | Tipo: %s | Entrada: %s | Dias: %d | Custo Base: R$ %.2f | Fatura Total: R$ %.2f | Sintomas: [%s]",
                leito.getidLeito(),
                leito.getNomePaciente(),
                leito.getTipo(),
                leito.getDataEntrada().toString(),
                leito.getDiasInternacao(),
                leito.getCustoBase(),
                leito.calcularFaturaTotal(),
                sintomasFormatados);
    }

    @Override
    public String getNomeFormato() {
        return "Texto estruturado (separador '|' com fatura)";
    }
}
