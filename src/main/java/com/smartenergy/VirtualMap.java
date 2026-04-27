package com.smartenergy;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.smartenergy.enumeration.*;
import com.smartenergy.model.*;

public class VirtualMap extends Canvas {
    private static final int DEFAULT_COLS = 90;
    private static final int DEFAULT_ROWS = 56;
    private static final double DEFAULT_CELL_SIZE = 10;
    private static final double DEFAULT_PADDING = 10;
    private static final double DOT_RADIUS = 1;

    private final int cols, rows;
    private final double cellSize, padding;
    private final GraphicsContext gc;
    private final List<GridPoint> selectedPoints = new ArrayList<>();
    private final List<Sector> sectors = new ArrayList<>();

    private boolean drawingMode = false;
    private boolean shapeClosed = false;

    private double zoomFactor = 1.0;
    private static final double MIN_ZOOM = 0.3;
    private static final double MAX_ZOOM = 5.0;
    private static final double ZOOM_STEP = 1.1;

    private TypeSecteur activeSecteurType = null;
    private Function<TypeSecteur, Color> colorResolver = type -> Color.BLUE;

    public VirtualMap() {
        this(DEFAULT_COLS, DEFAULT_ROWS, DEFAULT_CELL_SIZE, DEFAULT_PADDING);
    }

    public VirtualMap(int cols, int rows, double cellSize, double padding) {
        super(cols * cellSize + 2 * padding, rows * cellSize + 2 * padding);
        this.cols = cols;
        this.rows = rows;
        this.cellSize = cellSize;
        this.padding = padding;
        this.gc = getGraphicsContext2D();

        drawGrid();
        setupMouseHandler();
        setupZoomHandler();
    }

    public void allowDrawing(boolean bool) {
        drawingMode = bool;
        selectedPoints.clear();
        shapeClosed = false;
        drawGrid();
    }

    public void closeShape() {
        if (drawingMode && selectedPoints.size() >= 3) {
            finalizeCurrentShape();
        }
    }

    public void reset() {
        selectedPoints.clear();
        sectors.clear();
        drawingMode = false;
        shapeClosed = false;
        drawGrid();
    }

    public void setActiveSecteurType(TypeSecteur type) {
        this.activeSecteurType = type;
        drawGrid();
    }

    public TypeSecteur getActiveSecteurType() { return activeSecteurType; }

    public void setColorResolver(Function<TypeSecteur, Color> resolver) {
        this.colorResolver = resolver != null ? resolver : type -> Color.BLUE;
        drawGrid();
    }

    public List<Sector> getSectors() { return sectors; }

    public boolean isDrawingMode() { return drawingMode; }

    public void zoomIn() { setZoom(zoomFactor * ZOOM_STEP); }
    public void zoomOut() { setZoom(zoomFactor / ZOOM_STEP); }

    public void setZoom(double newZoom) {
        zoomFactor = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
        drawGrid();
    }
    public double getZoom() { return zoomFactor; }

    // In VirtualMap.java
    public void setSectors(List<Sector> secteurs) {
        this.sectors.clear();
        this.sectors.addAll(secteurs);
        drawGrid();
    }

    private void setupMouseHandler() {
        setOnMouseClicked(event -> {
            if (!drawingMode) return;

            double mouseX = event.getX();
            double mouseY = event.getY();
            double virtualX = (mouseX - getWidth()/2) / zoomFactor + getWidth()/2;
            double virtualY = (mouseY - getHeight()/2) / zoomFactor + getHeight()/2;

            int col = (int) Math.round((virtualX - padding) / cellSize);
            int row = (int) Math.round((virtualY - padding) / cellSize);
            if (col < 0 || col >= cols || row < 0 || row >= rows) return;

            GridPoint point = new GridPoint(col, row);

            if (event.getClickCount() == 1 && event.getButton() == MouseButton.PRIMARY) {
                if (!selectedPoints.contains(point)) {
                    selectedPoints.add(point);
                    shapeClosed = false;
                    drawGrid();
                }
            }
            else if (event.getClickCount() == 1 && event.getButton() == MouseButton.SECONDARY) {
                finalizeCurrentShape();
            }
        });
    }

    private void setupZoomHandler() {
        setOnScroll(event -> {
            if (event.getDeltaY() > 0) zoomIn(); else zoomOut();
        });
    }

    private void finalizeCurrentShape() {
        if (selectedPoints.size() >= 3) {
            if (!selectedPoints.get(0).equals(selectedPoints.get(selectedPoints.size() - 1))) {
                selectedPoints.add(selectedPoints.get(0));
            }
            if (activeSecteurType != null) {
                Sector secteur = new Sector(activeSecteurType);
                secteur.points = new ArrayList<>(selectedPoints);
                sectors.add(secteur);
            }
        }
        selectedPoints.clear();
        shapeClosed = false;
        drawGrid();
    }

    public void drawGrid() {
        gc.setFill(Color.SLATEBLUE);
        gc.fillRect(0, 0, getWidth(), getHeight());

        gc.save();
        double cx = getWidth() / 2;
        double cy = getHeight() / 2;
        gc.translate(cx, cy);
        gc.scale(zoomFactor, zoomFactor);
        gc.translate(-cx, -cy);

        for (Sector s : sectors) {
            Color color = colorResolver.apply(s.type);
            drawClosedPolygon(s.points, color);
        }

        if (drawingMode) {
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    double x = padding + col * cellSize;
                    double y = padding + row * cellSize;
                    GridPoint gp = new GridPoint(col, row);
                    gc.setFill(selectedPoints.contains(gp) ? Color.RED : Color.BLACK);
                    gc.fillOval(x - DOT_RADIUS, y - DOT_RADIUS, DOT_RADIUS * 2, DOT_RADIUS * 2);
                }
            }

            if (!selectedPoints.isEmpty()) {
                double[] xs = selectedPoints.stream().mapToDouble(p -> padding + p.col * cellSize).toArray();
                double[] ys = selectedPoints.stream().mapToDouble(p -> padding + p.row * cellSize).toArray();
                Color drawColor = colorResolver.apply(activeSecteurType);
                gc.setStroke(drawColor);
                gc.setLineWidth(2);
                if (shapeClosed) {
                    gc.strokePolygon(xs, ys, xs.length);
                } else {
                    gc.strokePolyline(xs, ys, xs.length);
                }
            }
        }
        gc.restore();
    }

    private void drawClosedPolygon(List<GridPoint> points, Color color) {
        if (points.size() < 3) return;
        double[] xs = points.stream().mapToDouble(p -> padding + p.col * cellSize).toArray();
        double[] ys = points.stream().mapToDouble(p -> padding + p.row * cellSize).toArray();

        gc.setFill(color);
        gc.fillPolygon(xs, ys, xs.length);

        gc.setStroke(color);
        gc.setLineWidth(2);
        gc.strokePolygon(xs, ys, xs.length);
    }
}
