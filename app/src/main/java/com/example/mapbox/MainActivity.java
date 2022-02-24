package com.example.mapbox;


import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mapbox.model.Lugar;

import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;
import com.mapbox.mapboxsdk.utils.BitmapUtils;
import com.mapbox.turf.TurfMeasurement;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "";
    private MapView mapView;
    private LocationComponent locationComponent;
    private static final String ID_ICON_MARKER = "marcador";
    private List<LatLng> routeCoordinates;

    //Lista  para la ruta corta buscada
    List<String> direccion=new ArrayList<String>();
    //Lista de los lugares de base de datos
    List<Lugar> lista_lugares = new ArrayList<Lugar>();

    //Lista con la conexiones de los puntos
    List<cone> listaConexion = new ArrayList<>();
    List<cone> listaConexionUnica = new ArrayList<>();





    Lugar lugarSelectIda;
    Lugar lugarSelectLlegada;

    //Base de datos
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;


    //Busquedad de lugar
    Spinner lugarIda, lugarLlegada;
    ArrayList<Lugar> lugarIdaList = new ArrayList<>();
    ArrayList<Lugar> lugarLlegadaList = new ArrayList<>();
    ArrayAdapter<Lugar> arrayAdapterLugarIda;
    ArrayAdapter<Lugar> arrayAdapterLugarLlegada;

    //boton
    Button btnGuiar;

    //Distancia entre puntos
    private List<Point> pointList = new ArrayList<>();
    private double totalLineDistance = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));

        setContentView(R.layout.activity_main);


        lugarIda = findViewById(R.id.spinnerPuntoPartida);
        lugarLlegada = findViewById(R.id.spinnerPuntoLlegada);
        btnGuiar = findViewById(R.id.btnGuiar);

        lugarIda.setEnabled(false);
        //Llamada a la funcion de inicializacion de base de datos
        inicializarFirebase();
        //Llamada a la funcion para listar los puntos dentro mapa de interiores
        listarLugarIda();
        listarLugarLlegada();


        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);


    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        mapboxMap.setStyle(new Style.Builder().fromUri("mapbox://styles/ronstreet/ckyewxzwy004u15s7qoeb8ln0"), new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {

                // Creacion del SymbolManager.
                SymbolManager symbolManager = new SymbolManager(mapView, mapboxMap, style);
                // Set non-data-driven properties.
                symbolManager.setIconAllowOverlap(true);
                symbolManager.setTextAllowOverlap(true);

                //Punto al que se desplegara en el mapa como inicio
                CameraPosition position = new CameraPosition.Builder()
                        .target(new LatLng(-0.1161695, -78.498495))
                        .zoom(20.27)
                        .build();
                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 2000);

                //Limite de Zoom del mapa
                mapboxMap.setMaxZoomPreference(22);

                //Llamada del Intent contenedor del lector codigo QR
                findViewById(R.id.floatingActionButtonQR).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new IntentIntegrator(MainActivity.this).initiateScan();
                    }
                });
                //Accion del boton Guiar
                btnGuiar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        symbolManager.deleteAll();
                        mapboxMap.clear();
                        String idIda;
                        String idLlegada;
                        String nombreIda;
                        String nombreLlegada;
                        //int posIda;
                        //int posLlegada;

                        nombreIda = lugarIda.getSelectedItem().toString();
                        lugarSelectIda = (Lugar) lugarIda.getItemAtPosition(lugarIda.getSelectedItemPosition());
                        //posIda=lugarIda.getSelectedItemPosition();
                        idIda = lugarSelectIda.getId().toString();


                        //agregarMarcador(lugarSelect,style,symbolManager);
                        //agregarMarcador(lugarSelectIda, mapboxMap);

                        nombreLlegada = lugarLlegada.getSelectedItem().toString();
                        lugarSelectLlegada = (Lugar) lugarLlegada.getItemAtPosition(lugarLlegada.getSelectedItemPosition());
                        //posLlegada=lugarLlegada.getSelectedItemPosition();
                        idLlegada = lugarSelectLlegada.getId().toString();

                        Log.i("IDida: "+idIda," IDllegada: "+idLlegada);
                        //agregarMarcador(lugarSelect,style,symbolManager);
                        if(!idIda.equals(idLlegada)){
                            agregarMarcador(lugarSelectIda, mapboxMap);
                            agregarMarcador(lugarSelectLlegada, mapboxMap);
                            //medirDistancia2(lugarSelectIda,lugarSelectLlegada);
                            rutacortadijstra(idIda, idLlegada, mapboxMap);
                        }
                        else {
                            Toast.makeText(MainActivity.this,"El punto de llegada no puede ser igual al punto de ida",Toast.LENGTH_LONG).show();
                        }

                    }
                });

            }
        });


    }

    private void iniciarRuta(List<String> direccion, MapboxMap mapboxMap) {
        ArrayList<LatLng> dire = new ArrayList<>();
        for(int d=0;d<direccion.size();d++){
            for(int r=0;r<lista_lugares.size();r++){
                if(direccion.get(d).equals(lista_lugares.get(r).getId())){
                    dire.add(new LatLng(Double.parseDouble(lista_lugares.get(r).getLatitud()),Double.parseDouble(lista_lugares.get(r).getLongitud())));
                }
            }
        }



        Log.i("RutaCompleta", ": encontrado"+dire.toString());
        DibujarLineas(mapboxMap, dire, direccion.size());


    }

    private void agregarMarcadorNuevo(@NonNull Lugar lugar, @NonNull Style style, @NonNull SymbolManager symbolManager) {

        style.addImage(ID_ICON_MARKER, BitmapUtils.getBitmapFromDrawable(getResources().getDrawable(R.drawable.ic_baseline_location_on_24)));
        // Create a symbol at the specified location.
        SymbolOptions symbolOptions = new SymbolOptions()
                .withLatLng(new LatLng(Double.parseDouble(lugar.getLatitud()), Double.parseDouble(lugar.getLongitud())))
                .withIconImage(ID_ICON_MARKER)
                .withIconSize(1.3f);
        medirDistancia(lugar);
        // Use the manager to draw the symbol.
        Symbol symbol = symbolManager.create(symbolOptions);

    }

    private void agregarMarcador(@NonNull Lugar lugar, @NonNull MapboxMap mapboxMap) {
        mapboxMap.addMarker(new MarkerOptions()
                .position(new LatLng(Double.parseDouble(lugar.getLatitud()), Double.parseDouble(lugar.getLongitud())))
                .title("Marcador"));

    }

    private void medirDistancia(Lugar lugar) {
        pointList.add(Point.fromLngLat(Double.parseDouble(lugar.getLatitud()), Double.parseDouble(lugar.getLongitud())));
        int pointListSize = pointList.size();

        double distanceBetweenLastAndSecondToLastClickPoint = 0;
        // Make the Turf calculation between the last tap point and the second-to-last tap point.
        if (pointList.size() >= 2) {
            distanceBetweenLastAndSecondToLastClickPoint = TurfMeasurement.distance(
                    pointList.get(pointListSize - 2), pointList.get(pointListSize - 1));
        }
        if (pointListSize >= 2 && distanceBetweenLastAndSecondToLastClickPoint > 0) {
            totalLineDistance += distanceBetweenLastAndSecondToLastClickPoint;
        }
        //Toast.makeText(MainActivity.this, "La distancia es: " + totalLineDistance * 1000, Toast.LENGTH_LONG).show();
    }

    public Double medirDistanciaGoogle(Double idaLatitud, Double idaLongitud, Double llegadaLatitud, Double llegadaLongitud) {
        Log.i("idaLatitud: " + idaLatitud, " idaLongitud " + idaLongitud);
        Log.i("llegadaLatitud: " + llegadaLatitud, " llegadaLongitud " + llegadaLongitud);
        double earthRadius = 3958.75;
        double dLat = Math.toRadians(llegadaLatitud - idaLatitud);
        double dLng = Math.toRadians(llegadaLongitud - idaLongitud);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(idaLatitud)) * Math.cos(Math.toRadians(llegadaLatitud)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double dist = earthRadius * c;
        //Toast.makeText(MainActivity.this, "La distancia es: " + dist, Toast.LENGTH_LONG).show();
        Log.i("distancia: ", "" + dist);
        return dist;

    }

    public Double distancia(String idIda, String idLlegada) {
        Lugar ida = new Lugar();
        Lugar llegada = new Lugar();
        for (int i = 0; i < lista_lugares.size(); i++) {
            if (lista_lugares.get(i).getId().equals(idIda)) {
                ida = lista_lugares.get(i);
            }
            if (lista_lugares.get(i).getId().equals(idLlegada)) {
                llegada = lista_lugares.get(i);
            }
        }
        Double medido = medirDistanciaLugar(ida, llegada);
        Log.i("idIda: " + idIda + " idLlegada: " + idLlegada, " medido: " + medido);

        Log.i("idIda1: " + idIda + " idLlegada1: " + idLlegada, " medido1: " + medido);
        return medido;
    }

    private void medirDistancia2(Lugar lugarIda, Lugar lugarLLegada) {
        Point puntoIda = Point.fromLngLat(Double.parseDouble(lugarIda.getLatitud()), Double.parseDouble(lugarIda.getLongitud()));
        Point puntoLlegada = Point.fromLngLat(Double.parseDouble(lugarLLegada.getLatitud()), Double.parseDouble(lugarLLegada.getLongitud()));
        double distancia = TurfMeasurement.distance(puntoIda, puntoLlegada);
        Toast.makeText(MainActivity.this, "La distancia es: " + distancia * 1000, Toast.LENGTH_LONG).show();
    }

    public Double medirDistanciaLugar(Lugar lugarIda, Lugar lugarLLegada) {
        Point puntoIda = Point.fromLngLat(Double.parseDouble(lugarIda.getLatitud()), Double.parseDouble(lugarIda.getLongitud()));
        Point puntoLlegada = Point.fromLngLat(Double.parseDouble(lugarLLegada.getLatitud()), Double.parseDouble(lugarLLegada.getLongitud()));
        double distancia = TurfMeasurement.distance(puntoIda, puntoLlegada);
        //Toast.makeText(MainActivity.this, "La distancia es: " + distancia * 1000, Toast.LENGTH_LONG).show();
        Log.i("idIda: " + lugarIda.getId() + " idLlegada: " + lugarLLegada.getId(), " distancia: " + distancia);
        return distancia;
    }

    Double medirDistancias(Double idaLatitud, Double idaLongitud, Double llegadaLatitud, Double llegadaLongitud) {
        Point puntoIda = Point.fromLngLat(idaLongitud, idaLatitud);
        Point puntoLlegada = Point.fromLngLat(llegadaLongitud, llegadaLatitud);
        double distancia = TurfMeasurement.distance(puntoIda, puntoLlegada);
        Toast.makeText(MainActivity.this, "La distancia es: " + distancia * 10000, Toast.LENGTH_LONG).show();
        return (distancia);
    }


    private void busqueda(String id) {
        Query query = databaseReference.child("Lugar/" + id + "/lugares");
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Log.i("Conexion", "Entrando");
                    //String c=snapshot.getValue().toString();
                    //listaConexion.add(new cone(id,c));
                    //Log.i("String: "+id,"con: "+snapshot.getValue().toString());
                    Log.i("CountChildren: ", "" + snapshot.getChildrenCount());
                    if (snapshot.getChildrenCount() == 0) {
                        String c = snapshot.getValue().toString();
                        listaConexion.add(new cone(id, c));
                        Log.i("Conexion: " + id, "con: " + c);
                    }
                    for (DataSnapshot objSnapshot : snapshot.getChildren()) {
                        String c1 = objSnapshot.getValue(String.class);
                        listaConexion.add(new cone(id, c1));
                        Log.i("Conexion: " + id, "con: " + c1);
                        //Toast.makeText(MainActivity.this,"Texto: " +l.getId(), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    Double busquedaLatitud(String id) {
        Double Latitud = 0.0;
        for (int i = 0; i < lista_lugares.size(); i++) {
            if (lista_lugares.get(i).getId() == id) {
                Latitud = Double.parseDouble(lista_lugares.get(i).getLatitud());
            }
        }
        return Latitud;
    }

    Double busquedaLongitud(String id) {
        Double Longitud = 0.0;
        for (int i = 0; i < lista_lugares.size(); i++) {
            if (lista_lugares.get(i).getId() == id) {
                Longitud = Double.parseDouble(lista_lugares.get(i).getLongitud());
            }
        }
        return Longitud;

    }

    private void listarLugarIda() {
        databaseReference.child("Lugar").addValueEventListener((new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot objSnapshot : snapshot.getChildren()) {
                    Lugar l = objSnapshot.getValue(Lugar.class);
                    lugarIdaList.add(l);
                    lista_lugares.add(l);
                    busqueda(l.getId());
                    arrayAdapterLugarIda = new ArrayAdapter<Lugar>(MainActivity.this, android.R.layout.simple_list_item_1, lugarIdaList);
                    lugarIda.setAdapter(arrayAdapterLugarIda);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        }));
    }

    private void listarLugarIda1(String id) {
        lugarIdaList.clear();
        Query query = databaseReference.child("Lugar").orderByChild("id").equalTo(id);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot objSnapshot : snapshot.getChildren()) {
                        Lugar l = objSnapshot.getValue(Lugar.class);
                        lugarIdaList.add(l);
                        arrayAdapterLugarIda = new ArrayAdapter<Lugar>(MainActivity.this, android.R.layout.simple_list_item_1, lugarIdaList);
                        lugarIda.setAdapter(arrayAdapterLugarIda);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void inicializarFirebase() {
        FirebaseApp.initializeApp(this);
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
    }

    private void listarLugarLlegada() {
        databaseReference.child("Lugar").addValueEventListener((new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot objSnapshot : snapshot.getChildren()) {
                    Lugar l = objSnapshot.getValue(Lugar.class);
                    lugarLlegadaList.add(l);

                    arrayAdapterLugarLlegada = new ArrayAdapter<Lugar>(MainActivity.this, android.R.layout.simple_list_item_1, lugarLlegadaList);
                    lugarLlegada.setAdapter(arrayAdapterLugarLlegada);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        }));
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelado", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "El valor escaneado es:" + result.getContents().toString(), Toast.LENGTH_LONG).show();
                listarLugarIda1(result.getContents().toString());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
            super.onActivityResult(requestCode, resultCode, data);
        }

    }


    private void rutacortadijstra(String idIda, String idLlegada, MapboxMap mapboxMap) {
        direccion.clear();
        int posIda = 0;
        int posLlegada = 0;
        if (idIda == idLlegada) {
            Toast.makeText(MainActivity.this, "El punto de partida y llegada no pueden ser iguales", Toast.LENGTH_LONG).show();
        } else {

            Double dista;
            listaConexionUnica=listaConexion;
            MutableValueGraph<String, Integer> graph = ValueGraphBuilder.undirected().build();

            for(int n=0;n<lista_lugares.size();n++){
                for(int l=0;l<listaConexionUnica.size();l++){
                    if(lista_lugares.get(n).getId().equals(listaConexionUnica.get(l).getNodo())){
                        for(int r=0;r<lista_lugares.size();r++){
                            if(lista_lugares.get(r).getId().equals(listaConexionUnica.get(l).getDestino())){
                                dista=distancia(lista_lugares.get(n).getId(),lista_lugares.get(r).getId());
                                int t=(int)Math.round(dista*10000);
                                graph.putEdgeValue(lista_lugares.get(n).getId(),lista_lugares.get(r).getId(),t);
                            }
                        }
                    }
                }
            }
            //ValueGraph<String, Integer> graph = createSampleGraph();

            System.out.println("graph = " + graph);

            direccion= busquedaImpresionRutaCorta(graph, idIda, idLlegada);
            iniciarRuta(direccion,mapboxMap);
            Log.i("Ruta", ":" + direccion.toString());
        }

    }

    public List<String> busquedaImpresionRutaCorta(
            ValueGraph<String, Integer> graph, String inicio, String llegada) {
            List<String> rutaCorta = DijkstraWithPriorityQueue.findShortestPath(graph, inicio, llegada);
            System.out.printf("ruta corta desde %s a %s = %s%n", inicio, llegada, rutaCorta);
            return  rutaCorta;
    }



    private void DibujarLineas(MapboxMap mapboxMap, ArrayList<LatLng> dire,Integer tamanio) {
        Log.i("QuePaso tamaño", "" + dire.size());
        if (dire.size() == tamanio) {
            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(dire)
                    .color(Color.RED)
                    .width(3f);

            // add polyline to MapboxMap object
            mapboxMap.addPolyline(polylineOptions);
            Log.i("QuePaso tamaño", "" + dire.size());
        }
    }

}