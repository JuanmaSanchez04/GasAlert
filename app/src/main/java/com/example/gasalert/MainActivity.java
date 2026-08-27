package com.example.gasalert;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.gasalert.adapter.EstacionAdapter;
import com.example.gasalert.model.CriterioOrden;
import com.example.gasalert.model.Estacion;
import com.example.gasalert.model.TipoCombustible;
import com.example.gasalert.network.PreciosRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import android.widget.EditText;
import android.widget.RadioGroup;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int MAX_RESULTADOS = 10;
    private static final int RADIO_MIN_KM = 1;
    private static final int RADIO_MAX_KM = 100;

    private static final long TIMEOUT_UBICACION_MS = 10_000;

    // Estado actual de los filtros — cambia según lo que el usuario elija en la UI
    private Double radioKmActual = null;
    private TipoCombustible tipoActual = TipoCombustible.GASOLEO_A;
    private CriterioOrden ordenActual = CriterioOrden.PRECIO;

    private RecyclerView recyclerView;
    private EstacionAdapter adapter;
    private Button btnRefrescar;
    private TextView textEstado;
    private EditText editRadio;
    private RadioGroup radioGroupCombustible;
    private RadioGroup radioGroupOrden;
    private FusedLocationProviderClient fusedLocationClient;
    private final PreciosRepository repository = new PreciosRepository();

    /** Diálogo de permiso moderno (sustituye a onRequestPermissionsResult). */
    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
                if (concedido) {
                    obtenerUbicacionYBuscar();
                } else {
                    mostrarEstado("Necesitamos permiso de ubicación para buscar gasolineras cerca de ti.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        enlazarVistas();
        configurarLista();
        configurarListeners();
    }

    /** Paso 1: conecta cada variable Java con su elemento del XML (findViewById). */
    private void enlazarVistas() {
        recyclerView = findViewById(R.id.recyclerEstaciones);
        btnRefrescar = findViewById(R.id.btnRefrescar);
        textEstado = findViewById(R.id.textEstado);
        editRadio = findViewById(R.id.editRadio);
        radioGroupCombustible = findViewById(R.id.radioGroupCombustible);
        radioGroupOrden = findViewById(R.id.radioGroupOrden);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    /** Paso 2: prepara el RecyclerView (la lista) para que sepa cómo pintar cada fila. */
    private void configurarLista() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EstacionAdapter(this::abrirEnGoogleMaps);
        recyclerView.setAdapter(adapter);
    }

    /** Paso 3: define qué pasa cuando el usuario toca/escribe algo en cada control. */
    private void configurarListeners() {
        // Botón "Refrescar": valida el radio y, si es correcto, busca.
        btnRefrescar.setOnClickListener(v -> {
            if (validarYActualizarRadio()) comprobarPermisoYBuscar();
        });

        // Tecla "Listo" del teclado numérico: hace lo mismo que el botón.
        editRadio.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (validarYActualizarRadio()) comprobarPermisoYBuscar();
                return true;
            }
            return false;
        });

        // Cambiar de Gasóleo A a Gasolina (o al revés): solo actualiza el filtro, no busca todavía.
        radioGroupCombustible.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioGasolina) {
                tipoActual = TipoCombustible.GASOLINA_95;
            } else {
                tipoActual = TipoCombustible.GASOLEO_A;
            }
        });

// Cambiar el orden (Precio / Distancia): igual, solo actualiza el filtro.
        radioGroupOrden.setOnCheckedChangeListener((group, checkedId) -> {
            ordenActual = checkedId == R.id.radioOrdenDistancia ? CriterioOrden.DISTANCIA : CriterioOrden.PRECIO;
        });
    }

    /** Lee el radio escrito por el usuario y comprueba que esté entre RADIO_MIN_KM y RADIO_MAX_KM.
     *  Si es válido, actualiza radioKmActual y devuelve true. Si no, marca el campo en rojo con
     *  un mensaje de error (EditText.setError) y devuelve false — así el botón/Enter no llega
     *  a lanzar ninguna búsqueda con un valor fuera de rango. */
    private boolean validarYActualizarRadio() {
        String texto = editRadio.getText().toString().trim();

        if (texto.isEmpty()) {
            radioKmActual = null; // radio opcional: sin límite
            return true;
        }

        int valor;
        try {
            valor = Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            editRadio.setError("Solo números enteros");
            return false;
        }

        if (valor < RADIO_MIN_KM || valor > RADIO_MAX_KM) {
            editRadio.setError("Entre " + RADIO_MIN_KM + " y " + RADIO_MAX_KM + " km");
            return false;
        }

        radioKmActual = (double) valor;
        return true;
    }

    private void comprobarPermisoYBuscar() {
        boolean concedido = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (concedido) {
            obtenerUbicacionYBuscar();
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission") // el permiso ya se comprueba en comprobarPermisoYBuscar() antes de llamar aquí
    private void obtenerUbicacionYBuscar() {
        mostrarEstado("Obteniendo tu ubicación…");
        btnRefrescar.setEnabled(false);

        // getCurrentLocation() (no getLastLocation()) fuerza una lectura nueva con GPS,
        // en vez de devolver una posición en caché que puede estar desfasada si te estás
        // moviendo — por ejemplo, si abres la app en marcha durante un viaje.
        CancellationTokenSource cancelToken = new CancellationTokenSource();
        Handler timeoutHandler = new Handler(Looper.getMainLooper());
        boolean[] agotado = {false};

        // Si el GPS tarda demasiado (mala cobertura, túnel, etc.), cancelamos la
        // petición a los TIMEOUT_UBICACION_MS y avisamos, en vez de dejar a la persona
        // esperando indefinidamente con el mensaje "Obteniendo tu ubicación…".
        Runnable avisoTimeout = () -> {
            agotado[0] = true;
            cancelToken.cancel();
            mostrarEstado("No se pudo obtener tu ubicación a tiempo. Comprueba el GPS e inténtalo de nuevo.");
            btnRefrescar.setEnabled(true);
        };
        timeoutHandler.postDelayed(avisoTimeout, TIMEOUT_UBICACION_MS);

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancelToken.getToken())
                .addOnSuccessListener(location -> {
                    timeoutHandler.removeCallbacks(avisoTimeout);
                    if (location == null) {
                        mostrarEstado("No se pudo obtener tu ubicación. Comprueba que el GPS esté activado.");
                        btnRefrescar.setEnabled(true);
                        return;
                    }
                    buscarPrecios(location.getLatitude(), location.getLongitude());
                })
                .addOnFailureListener(e -> {
                    timeoutHandler.removeCallbacks(avisoTimeout);
                    // Si ya mostramos el mensaje de timeout, no lo pisamos con el de error genérico
                    // (cancelar la petición también dispara este listener).
                    if (!agotado[0]) {
                        mostrarEstado("Error al obtener la ubicación: " + e.getMessage());
                        btnRefrescar.setEnabled(true);
                    }
                });
    }

    private void buscarPrecios(double lat, double lon) {
        mostrarEstado("Buscando gasolineras cerca de ti…");

        double radioParaBusqueda = radioKmActual != null ? radioKmActual : RADIO_MAX_KM ;

        repository.buscarCercanas(lat, lon, radioParaBusqueda, MAX_RESULTADOS, tipoActual, ordenActual,
                new PreciosRepository.PreciosCallback() {
                    @Override
                    public void onExito(List<Estacion> estaciones) {
                        btnRefrescar.setEnabled(true);
                        if (estaciones.isEmpty()) {
                            String radioTexto = radioKmActual != null ? " en " + radioKmActual.intValue() + " km" : "";
                            mostrarEstado("No hay gasolineras con " + tipoActual.getEtiqueta() + radioTexto + ".");
                            adapter.actualizar(new ArrayList<>(), tipoActual);
                            return;
                        }
                        ocultarEstado();
                        adapter.actualizar(estaciones, tipoActual);
                    }

                    @Override
                    public void onError(String mensaje) {
                        btnRefrescar.setEnabled(true);
                        mostrarEstado("No se pudo consultar los precios: " + mensaje);
                    }
                });
    }

    /** Abre Google Maps con un geo: URI. La etiqueta entre paréntesis hace que
     *  el pin muestre el nombre de la gasolinera en vez de las coordenadas. */
    private void abrirEnGoogleMaps(Estacion estacion) {
        String etiqueta = Uri.encode(estacion.getNombre());
        String uri = "geo:0,0?q=" + estacion.getLat() + "," + estacion.getLon() + "(" + etiqueta + ")";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(intent);
    }

    private void mostrarEstado(String texto) {
        textEstado.setVisibility(View.VISIBLE);
        textEstado.setText(texto);
    }

    private void ocultarEstado() {
        textEstado.setVisibility(View.GONE);
    }
}