package com.smartenergy.model;

import com.smartenergy.enumeration.TypeSecteur;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.smartenergy.enumeration.TypeSecteur;
import com.smartenergy.model.Batiment;

public class Sector implements Serializable {
    private static final long serialVersionUID = 1L;
    public TypeSecteur type;
    public List<GridPoint> points = new ArrayList<>();
    public Batiment building;

    public Sector(TypeSecteur type) {
        this.type = type;
    }

    public Sector(Batiment building) {
        this.type = TypeSecteur.BATIMENT;
        this.building = building;
    }
}
