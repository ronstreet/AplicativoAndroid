package com.example.mapbox;

public class cone {
    String nodo;
    String destino;

    public cone(String nodo, String destino) {
        this.nodo = nodo;
        this.destino = destino;
    }
    public String getNodo() {
        return nodo;
    }

    public void setNodo(String nodo) {
        this.nodo = nodo;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }
}
