package com.smartenergy.main.model;

import com.smartenergy.main.enumeration.TypeEnergie;
import java.time.LocalDate;
import java.util.*;

public class AnalyseService {

    public double totalConsommation(List<Consommation> consommations) {
        return consommations.stream().mapToDouble(Consommation::getQuantite).sum();
    }

    public double totalCout(List<Consommation> consommations) {
        return consommations.stream().mapToDouble(Consommation::getCout).sum();
    }

    public Batiment batimentPlusConsommateur(List<Consommation> consommations) {
        Map<Batiment, Double> map = new HashMap<>();

        for (Consommation c : consommations) {
            map.put(c.getBatiment(), map.getOrDefault(c.getBatiment(), 0.0) + c.getQuantite());
        }

        return map.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }

    public TypeEnergie energieDominante(List<Consommation> consommations) {
        Map<TypeEnergie, Double> map = new HashMap<>();

        for (Consommation c : consommations) {
            map.put(c.getTypeEnergie(), map.getOrDefault(c.getTypeEnergie(), 0.0) + c.getQuantite());
        }

        return map.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }

    public boolean existePic(List<Consommation> consommations) {
        for (Consommation c : consommations) {
            if (c.getQuantite() > 500) {return true;
            }
        }
        return false;
    }

    public double totalDuJour(List<Consommation> consommations) {
        LocalDate aujourdHui = LocalDate.now();

        return consommations.stream().filter(c -> c.getDateHeure().toLocalDate().equals(aujourdHui)).mapToDouble(Consommation::getQuantite).sum();
    }

    public double estimationMensuelle(List<Consommation> consommations) {
        return totalCout(consommations);
    }
}