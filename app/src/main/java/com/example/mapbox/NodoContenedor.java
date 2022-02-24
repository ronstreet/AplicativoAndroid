package com.example.mapbox;

public class NodoContenedor<N> implements Comparable<NodoContenedor<N>> {
    private final N nodo;
    private int totalDistancia;
    private NodoContenedor<N> predecesor;

    NodoContenedor(N nodo, int totalDistancia, NodoContenedor<N> predecessor) {
        this.nodo = nodo;
        this.totalDistancia = totalDistancia;
        this.predecesor = predecessor;
    }

    N getNodo() {
        return nodo;
    }

    void setTotalDistancia(int totalDistancia) {
        this.totalDistancia = totalDistancia;
    }

    public int getTotalDistancia() {
        return totalDistancia;
    }

    public void setPredecesor(NodoContenedor<N> predecesor) {
        this.predecesor = predecesor;
    }

    public NodoContenedor<N> getPredecesor() {
        return predecesor;
    }

    @Override
    public int compareTo(NodoContenedor<N> other) {
        return Integer.compare(this.totalDistancia, other.totalDistancia);
    }


    @Override
    public boolean equals(Object other) {
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
