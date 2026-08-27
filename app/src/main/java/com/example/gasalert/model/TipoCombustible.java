package com.example.gasalert.model;

/** Tipos de combustible que mostramos. El "campoJson" es el nombre exacto del
 *  campo tal como lo llama la API del Ministerio — si algún día quieres añadir
 *  otro (Gasóleo Premium, GLP...), solo hace falta un valor más aquí. */
public enum TipoCombustible {
    GASOLEO_A("Precio Gasoleo A", "Gasóleo A"),
    GASOLINA_95("Precio Gasolina 95 E5", "Gasolina 95");

    private final String campoJson;
    private final String etiqueta;

    TipoCombustible(String campoJson, String etiqueta) {
        this.campoJson = campoJson;
        this.etiqueta = etiqueta;
    }

    public String getCampoJson() { return campoJson; }
    public String getEtiqueta() { return etiqueta; }

    @Override
    public String toString() { return etiqueta; } // así se ve bien directamente en un Spinner/RadioButton
}