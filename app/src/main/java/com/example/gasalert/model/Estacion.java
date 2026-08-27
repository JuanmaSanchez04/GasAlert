package com.example.gasalert.model;


public class Estacion {

    private final String nombre;
    private final String municipio;
    /** Puede ser null si esa estación no vende Gasóleo A. */
    private final Double precioGasoleoA;
    /** Puede ser null si esa estación no vende Gasolina 95. */
    private final Double precioGasolina95;
    private final double distanciaKm;
    private final double lat;
    private final double lon;

    public Estacion(String nombre, String municipio, Double precioGasoleoA, Double precioGasolina95,
                    double distanciaKm, double lat, double lon) {
        this.nombre = nombre;
        this.municipio = municipio;
        this.precioGasoleoA = precioGasoleoA;
        this.precioGasolina95 = precioGasolina95;
        this.distanciaKm = distanciaKm;
        this.lat = lat;
        this.lon = lon;
    }

    public String getNombre() {
        return nombre;
    }

    public String getMunicipio() {
        return municipio;
    }

    public Double getPrecioGasoleoA() {
        return precioGasoleoA;
    }

    public Double getPrecioGasolina95() {
        return precioGasolina95;
    }

    public double getDistanciaKm() {
        return distanciaKm;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public Double getPrecio(TipoCombustible tipo) {
        return tipo == TipoCombustible.GASOLINA_95 ? precioGasolina95 : precioGasoleoA;
    }
}