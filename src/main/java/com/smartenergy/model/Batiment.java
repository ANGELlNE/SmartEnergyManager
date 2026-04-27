package com.smartenergy.model;

import com.smartenergy.enumeration.TypeBatiment;

public class Batiment {
    private int id;
    private String nom;
    private TypeBatiment type;
    private String adresse;
    private double surface;

    public Batiment(int id, String nom, TypeBatiment type, String adresse, double surface) {
        this.id = id;
        this.nom = nom;
        this.type = type;
        this.adresse = adresse;
        this.surface = surface;
    }

    public Batiment cloner(int nouvelId) {
        return new Batiment(nouvelId, nom + " copie", type, adresse, surface);
    }

    public int getId() { return id; }
    public String getNom() { return nom; }
    public TypeBatiment getType() { return type; }
    public String getAdresse() { return adresse; }
    public double getSurface() { return surface; }

    public void setNom(String nom) { this.nom = nom; }
    public void setType(TypeBatiment type) { this.type = type; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public void setSurface(double surface) { this.surface = surface; }

    @Override
    public String toString() {
        return nom + " (" + type + ")";
    }
}
