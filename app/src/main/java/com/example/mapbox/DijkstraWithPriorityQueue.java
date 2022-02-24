package com.example.mapbox;

import com.google.common.graph.ValueGraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;


public class DijkstraWithPriorityQueue {
    public static <N> List<N> findShortestPath(ValueGraph<N, Integer> graph, N ida, N llegada) {
        Map<N, NodoContenedor<N>> nodoContenedores = new HashMap<>();
        PriorityQueue<NodoContenedor<N>> queue = new PriorityQueue<>();
        Set<N> rutacortaEncontrada = new HashSet<>();

        // agregamos el nodo inicial a la cola
        NodoContenedor<N> idaContenedor = new NodoContenedor<>(ida, 0, null);
        nodoContenedores.put(ida, idaContenedor);
        queue.add(idaContenedor);

        while (!queue.isEmpty()) {
            NodoContenedor<N> nodoContenedor = queue.poll();
            N nodo = nodoContenedor.getNodo();
            rutacortaEncontrada.add(nodo);

            // Nodo es igual a Nodo llegada --> Se construye la ruta corta del nodo ida al nodo llegada
            if (nodo.equals(llegada)) {
                return construirRuta(nodoContenedor);
            }

            // Iteracion sobre los nodos vecinos
            Set<N> vecinos = graph.adjacentNodes(nodo);
            for (N vecino : vecinos) {
                // Ignorar al vecino si ya se encontró la ruta más corta
                if (rutacortaEncontrada.contains(vecino)) {
                    continue;
                }

                // calculo de la distancia total desde el inicio hasta el vecino a través del nodo actual
                int distancia = graph.edgeValueOrDefault(nodo, vecino,0);
                int totalDistancia = nodoContenedor.getTotalDistancia() + distancia;

                // Vecino aun no descubierto
                NodoContenedor<N> vecinoContenedor = nodoContenedores.get(vecino);
                if (vecinoContenedor == null) {
                    vecinoContenedor = new NodoContenedor<>(vecino, totalDistancia, nodoContenedor);
                    nodoContenedores.put(vecino, vecinoContenedor);
                    queue.add(vecinoContenedor);
                }

                // Vecino descubierto, Comprobacion si  distanciaTotal es menor que el nodo actual
                // --> Actualizando la distanciaTotal y el predecesor del Nodo
                else if (totalDistancia < vecinoContenedor.getTotalDistancia()) {
                    vecinoContenedor.setTotalDistancia(totalDistancia);
                    vecinoContenedor.setPredecesor(nodoContenedor);

                    //PriorityQueue no cambia automaticamente la posicion por lo tanto se debe remover;
                    // Se vuelve a ingresar el nodo
                    queue.remove(vecinoContenedor);
                    queue.add(vecinoContenedor);
                }
            }
        }

        /*
         Returna  null en caso de recorrer y no hallar objetivo
        */

        return null;
    }

    private static <N> List<N> construirRuta(NodoContenedor<N> nodoContenedor) {
        List<N> path = new ArrayList<>();
        while (nodoContenedor != null) {
            path.add(nodoContenedor.getNodo());
            nodoContenedor = nodoContenedor.getPredecesor();
        }
        Collections.reverse(path);
        return path;
    }
}
