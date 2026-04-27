package com.smartenergy.model;

import java.time.LocalDateTime;

import com.smartenergy.enumeration.TypeEnergie;

public class Consommation {
    private Batiment batiment;
    private LocalDateTime dateHeure;
    private TypeEnergie typeEnergie;
    private double quantite;
    private double cout;

    public Consommation(Batiment batiment, LocalDateTime dateHeure, TypeEnergie typeEnergie, double quantite, double cout) {
        this.batiment = batiment;
        this.dateHeure = dateHeure;
        this.typeEnergie = typeEnergie;
        this.quantite = quantite;
        this.cout = cout;
    }

    public Batiment getBatiment() { return batiment; }
    public LocalDateTime getDateHeure() { return dateHeure; }
    public TypeEnergie getTypeEnergie() { return typeEnergie; }
    public double getQuantite() { return quantite; }
    public double getCout() { return cout; }

    @Override
    public String toString() {
        return batiment.getNom() + " | " + typeEnergie + " | " + quantite + " | " + cout + " €";
    }
}
