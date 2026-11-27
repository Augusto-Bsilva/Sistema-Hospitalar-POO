import java.time.LocalDate;
import java.util.List;

/**
 * Fábrica centralizada para instanciar subclasses concretas de {@link Leito}
 * a partir do identificador de tipo, evitando duplicação de lógica entre
 * leitura de arquivos, menu interativo e simulação concorrente.
 */
public final class LeitoFactory {

    private LeitoFactory() {
    }

    public static Leito criar(int id, String nome, double custoBase, int diasInternacao,
                              LocalDate dataEntrada, String tipo, List<String> sintomas) {
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("Tipo de leito nao pode ser nulo ou vazio.");
        }
        String tipoNormalizado = tipo.trim().toUpperCase();
        switch (tipoNormalizado) {
            case "ENFERMARIA":
                return new LeitoEnfermaria(id, nome, custoBase, diasInternacao, dataEntrada, tipoNormalizado, sintomas);
            case "UTI":
                return new LeitoUTI(id, nome, custoBase, diasInternacao, dataEntrada, tipoNormalizado, sintomas);
            case "ISOLAMENTO":
                return new LeitoIsolamento(id, nome, custoBase, diasInternacao, dataEntrada, tipoNormalizado, sintomas);
            case "NEONATAL":
                return new LeitoNeonatal(id, nome, custoBase, diasInternacao, dataEntrada, tipoNormalizado, sintomas);
            default:
                throw new IllegalArgumentException("Tipo de leito desconhecido: " + tipo);
        }
    }
}
