package com.smartenergy.model;

import com.smartenergy.enumeration.TypeSecteur;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Sector implements Serializable {
    private static final long serialVersionUID = 1L;
    public TypeSecteur type;
    public List<GridPoint> points = new ArrayList<>();

    public Sector(TypeSecteur type) {
        this.type = type;
    }
}
