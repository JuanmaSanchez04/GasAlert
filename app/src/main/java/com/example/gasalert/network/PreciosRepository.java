package com.example.gasalert.network;

import android.os.Handler;
import android.os.Looper;

import com.example.gasalert.model.CriterioOrden;
import com.example.gasalert.model.Estacion;
import com.example.gasalert.model.TipoCombustible;
import com.example.gasalert.util.Haversine;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Descarga el JSON público de precios de carburantes y filtra las estaciones
 * dentro de un radio alrededor de un punto.
 *
 * A diferencia de las versiones de escritorio (PC / GitHub Actions), aquí NO
 * hace falta el truco de invocar `curl` como proceso externo: el teléfono
 * sale a internet con la IP de tu operador móvil, no con una IP de datacenter,
 * así que la API del Ministerio no la bloquea. Se usa OkHttp directamente.
 *
 * Toda la descarga y el parseo se hacen en un hilo secundario (ExecutorService),
 * porque Android prohíbe hacer peticiones de red en el hilo principal — el
 * resultado se entrega de vuelta en el hilo principal (Handler) para poder
 * tocar la UI con seguridad desde el callback.
 */
public class PreciosRepository {

    private static final String API_URL =
            "https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/EstacionesTerrestres/";

    public interface PreciosCallback {
        void onExito(List<Estacion> estaciones);
        void onError(String mensaje);
    }

    private final OkHttpClient client;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public PreciosRepository() {
        client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
    }

    public void buscarCercanas(double lat, double lon, double radioKm, int maxResultados,
                               TipoCombustible tipo, CriterioOrden orden, PreciosCallback callback) {
        executor.execute(() -> {
            try {
                String json = descargarJson();
                List<Estacion> resultado = parsearYFiltrar(json, lat, lon, radioKm, maxResultados, tipo, orden);
                mainHandler.post(() -> callback.onExito(resultado));
            } catch (Exception e) {
                String mensaje = e.getMessage() != null ? e.getMessage() : e.toString();
                mainHandler.post(() -> callback.onError(mensaje));
            }
        });
    }

    private String descargarJson() throws IOException {
        Request request = new Request.Builder()
                .url(API_URL)
                .header("Accept", "application/json, text/plain, */*")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36")
                .header("Referer", "https://sedeaplicaciones.minetur.gob.es/")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("La API devolvió el código " + response.code());
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Respuesta vacía de la API");
            }
            return body.string();
        }
    }

    private List<Estacion> parsearYFiltrar(String json, double lat, double lon, double radioKm, int maxResultados,
                                           TipoCombustible tipo, CriterioOrden orden) throws Exception {
        List<Estacion> resultado = new ArrayList<>();
        JSONObject root = new JSONObject(json);
        JSONArray lista = root.getJSONArray("ListaEESSPrecio");

        for (int i = 0; i < lista.length(); i++) {
            JSONObject est = lista.getJSONObject(i);

            // El precio que determina si esta estación cuenta o no es el del tipo elegido
            // (si buscas gasolina, no tiene sentido enseñar una estación que solo vende diésel).
            String precioFiltroStr = est.optString(tipo.getCampoJson(), "").replace(",", ".");
            if (precioFiltroStr.isEmpty()) continue;

            String latStr = est.optString("Latitud", "").replace(",", ".");
            String lonStr = est.optString("Longitud (WGS84)", "").replace(",", ".");
            if (latStr.isEmpty() || lonStr.isEmpty()) continue;

            try {
                double estLat = Double.parseDouble(latStr);
                double estLon = Double.parseDouble(lonStr);
                double distancia = Haversine.distanciaKm(lat, lon, estLat, estLon);
                if (distancia > radioKm) continue;

                // Guardamos los dos precios (aunque solo filtremos por uno) para poder
                // enseñar ambos en la lista sin tener que volver a consultar la API.
                Double precioGasoleoA = parsearPrecioOpcional(est, TipoCombustible.GASOLEO_A.getCampoJson());
                Double precioGasolina95 = parsearPrecioOpcional(est, TipoCombustible.GASOLINA_95.getCampoJson());

                resultado.add(new Estacion(
                        est.optString("Rótulo", "Sin nombre"),
                        est.optString("Municipio", ""),
                        precioGasoleoA, precioGasolina95,
                        distancia, estLat, estLon
                ));
            } catch (NumberFormatException ignored) {
                // fila con datos corruptos en la fuente oficial: se ignora esa estación
            }
        }

        Comparator<Estacion> comparador = orden == CriterioOrden.DISTANCIA
                ? Comparator.comparingDouble(Estacion::getDistanciaKm)
                : Comparator.comparingDouble(e -> tipo == TipoCombustible.GASOLEO_A
                                                  ? e.getPrecioGasoleoA() : e.getPrecioGasolina95());

        Collections.sort(resultado, comparador);
        return resultado.size() > maxResultados ? resultado.subList(0, maxResultados) : resultado;
    }

    /** Devuelve el precio como Double, o null si esa estación no reporta ese combustible. */
    private Double parsearPrecioOpcional(JSONObject est, String campo) {
        String valor = est.optString(campo, "").replace(",", ".");
        if (valor.isEmpty()) return null;
        try {
            return Double.parseDouble(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}