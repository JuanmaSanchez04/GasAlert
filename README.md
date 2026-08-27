# Gasóleo cercano

App Android que usa tu ubicación para mostrarte, al segundo, la gasolinera más barata cerca de ti. Los datos son oficiales, de la API pública del Ministerio para la Transición Ecológica.

## Capturas

<p align="center">
  <img src="screenshots/pantalla_principal.png" width="250" alt="Pantalla principal">
</p>

## Qué hace

- Pide tu ubicación y calcula la distancia a cada gasolinera cercana.
- Puedes elegir el radio de búsqueda (entre 1 y 15 km).
- Puedes elegir entre Gasóleo A o Gasolina 95.
- Puedes ordenar los resultados por precio o por distancia.
- Cada gasolinera tiene un botón para abrirla directamente en Google Maps.

## Cómo está hecho

- **Java** (Android nativo, sin frameworks externos de UI).
- **Google Play Services (FusedLocationProviderClient)** para la ubicación.
- **OkHttp** para consultar la API del Ministerio.
- **RecyclerView** para la lista de resultados.

## De dónde salen los datos

API pública y gratuita del Ministerio para la Transición Ecológica y el Reto Demográfico, sin necesidad de API key:
```
https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/EstacionesTerrestres/
```

## Cómo ejecutarlo

1. Clona el repositorio.
2. Ábrelo con Android Studio.
3. Deja que Gradle sincronice las dependencias.
4. Ejecuta la app en un emulador o en tu móvil (necesitas Android 7.0 / API 24 o superior).

## Por qué lo hice

Empezó como un pequeño programa en Java que me enviaba un email cada día con el precio más barato. Por el camino me encontré con varios problemas de infraestructura (bloqueos de red, geolocalización de Windows rota...) que me hicieron ir cambiando de enfoque hasta llegar a esta app. Cuento el proceso completo aquí: [enlace a la publicación de LinkedIn]
