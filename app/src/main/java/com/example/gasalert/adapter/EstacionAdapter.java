package com.example.gasalert.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.example.gasalert.R;
import com.example.gasalert.model.Estacion;
import com.example.gasalert.model.TipoCombustible;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EstacionAdapter extends RecyclerView.Adapter<EstacionAdapter.EstacionViewHolder> {

    /** Se llama cuando el usuario toca el botón de mapa; MainActivity la usa para abrir Google Maps. */
    public interface OnEstacionClickListener {
        void onClick(Estacion estacion);
    }

    private List<Estacion> estaciones = new ArrayList<>();
    private TipoCombustible tipoActual = TipoCombustible.GASOLEO_A;
    private final OnEstacionClickListener listener;

    public EstacionAdapter(OnEstacionClickListener listener) {
        this.listener = listener;
    }

    /** Sustituye la lista completa y el tipo de combustible mostrado, y refresca la UI.
     *  Se llama tras cada búsqueda nueva o cambio de combustible. */
    public void actualizar(List<Estacion> nuevasEstaciones, TipoCombustible tipo) {
        this.estaciones = nuevasEstaciones;
        this.tipoActual = tipo;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EstacionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_estacion, parent, false);
        return new EstacionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EstacionViewHolder holder, int position) {
        Estacion estacion = estaciones.get(position);
        holder.nombre.setText(estacion.getNombre());
        holder.municipio.setText(estacion.getMunicipio());
        holder.distancia.setText(String.format(Locale.getDefault(), "%.1f km", estacion.getDistanciaKm()));

        // Solo se muestra la línea del combustible seleccionado; la otra se oculta.
        if (tipoActual == TipoCombustible.GASOLINA_95) {
            holder.precioGasoleo.setVisibility(View.GONE);
            holder.precioGasolina.setVisibility(View.VISIBLE);
            holder.precioGasolina.setText(tipoActual.getEtiqueta() + ": " + formatoPrecio(estacion.getPrecio(tipoActual)));
        } else {
            holder.precioGasolina.setVisibility(View.GONE);
            holder.precioGasoleo.setVisibility(View.VISIBLE);
            holder.precioGasoleo.setText(tipoActual.getEtiqueta() + ": " + formatoPrecio(estacion.getPrecio(tipoActual)));
        }

        holder.btnVerMapa.setOnClickListener(v -> listener.onClick(estacion));
    }

    private String formatoPrecio(Double precio) {
        return precio == null ? "—" : String.format(Locale.getDefault(), "%.3f €", precio);
    }

    @Override
    public int getItemCount() {
        return estaciones.size();
    }

    static class EstacionViewHolder extends RecyclerView.ViewHolder {
        final TextView nombre;
        final TextView municipio;
        final TextView distancia;
        final TextView precioGasoleo;
        final TextView precioGasolina;
        final Button btnVerMapa;

        EstacionViewHolder(@NonNull View itemView) {
            super(itemView);
            nombre = itemView.findViewById(R.id.textNombre);
            municipio = itemView.findViewById(R.id.textMunicipio);
            distancia = itemView.findViewById(R.id.textDistancia);
            precioGasoleo = itemView.findViewById(R.id.textPrecioGasoleo);
            precioGasolina = itemView.findViewById(R.id.textPrecioGasolina);
            btnVerMapa = itemView.findViewById(R.id.btnVerMapa);
        }
    }
}
