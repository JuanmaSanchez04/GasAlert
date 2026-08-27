package com.example.gasalert.model;

public enum CriterioOrden {
    PRECIO("Precio"),
    DISTANCIA("Distancia");

    private final String etiqueta;

    CriterioOrden(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() { return etiqueta; }
}