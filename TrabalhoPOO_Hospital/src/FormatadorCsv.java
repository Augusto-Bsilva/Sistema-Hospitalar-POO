import java.util.Locale;

/**
 * Formata registros de {@link Leito} no formato CSV delimitado por ponto e vírgula,
 * compatível com a leitura realizada pela classe {@link LeitorDados}.
 */
public class FormatadorCsv implements Formatador {

    @Override
    public String formatarLinha(Leito leito) {
        if (leito == null) {
            throw new IllegalArgumentException("Leito nao pode ser nulo.");
        }
        String sintomasFormatados = String.join(",", leito.getSintomas());
        return String.format(Locale.US, "%d;%s;%.2f;%d;%s;%s;%s",
                leito.getidLeito(),
                leito.getNomePaciente(),
                leito.getCustoBase(),
                leito.getDiasInternacao(),
                leito.getDataEntrada().toString(),
                sintomasFormatados,
                leito.getTipo());
    }

    @Override
    public String getNomeFormato() {
        return "CSV (delimitado por ';')";
    }
}
