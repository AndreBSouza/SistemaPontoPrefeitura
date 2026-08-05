package br.gov.ponto.biometria.domain;

/**
 * Comparacao biometrica 1:1: similaridade de cosseno entre dois descritores faciais
 * (embeddings) com limiar de aceitacao. Regra de dominio pura e testavel — o nucleo
 * do reconhecimento "esta pessoa e o servidor cadastrado?".
 *
 * <p>A EXTRACAO do descritor a partir do rosto exige um modelo de reconhecimento
 * (FaceNet/MobileFaceNet em TFLite on-device, ou serviço gerenciado como AWS
 * Rekognition) — essa parte e externa. Aqui fica a comparacao + o limiar.</p>
 */
public final class ComparadorFacial {

    /** Limiar padrao de aceitacao (similaridade de cosseno). Calibravel por ente. */
    public static final double LIMIAR_PADRAO = 0.80;

    private ComparadorFacial() {
    }

    /** Similaridade de cosseno em [-1, 1]; 1 = descritores idênticos. */
    public static double similaridade(double[] a, double[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) {
            throw new IllegalArgumentException("Descritores invalidos ou de tamanhos diferentes");
        }
        double produto = 0;
        double normaA = 0;
        double normaB = 0;
        for (int i = 0; i < a.length; i++) {
            produto += a[i] * b[i];
            normaA += a[i] * a[i];
            normaB += b[i] * b[i];
        }
        if (normaA == 0 || normaB == 0) {
            return 0;
        }
        return produto / (Math.sqrt(normaA) * Math.sqrt(normaB));
    }

    /** Verdadeiro se a similaridade atinge o limiar informado. */
    public static boolean corresponde(double[] a, double[] b, double limiar) {
        return similaridade(a, b) >= limiar;
    }

    /** Faz o parse de um descritor serializado em CSV ("0.12,-0.34,0.56,..."). */
    public static double[] parse(String csv) {
        if (csv == null || csv.isBlank()) {
            throw new IllegalArgumentException("Descritor vazio");
        }
        String[] partes = csv.trim().split(",");
        double[] v = new double[partes.length];
        for (int i = 0; i < partes.length; i++) {
            v[i] = Double.parseDouble(partes[i].trim());
        }
        return v;
    }
}
