package com.smartenergy.main.model;

import com.smartenergy.main.enumeration.TypeBatiment;
import java.util.ArrayList;
import java.util.List;

public class BatimentService {
    private List<Batiment> batiments = new ArrayList<>();
    private int prochainId = 1;

    public Batiment ajouter(String nom, TypeBatiment type, String adresse, double surface) {
        Batiment b = new Batiment(prochainId++, nom, type, adresse, surface);
        batiments.add(b);
        return b;
    }

    public void supprimer(Batiment b) {
        batiments.remove(b);
    }

    public Batiment cloner(Batiment b) {
        Batiment copie = b.cloner(prochainId++);
        batiments.add(copie);
        return copie;
    }

    public List<Batiment> getBatiments() {
        return batiments;
    }
}