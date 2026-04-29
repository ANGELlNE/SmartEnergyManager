package com.smartenergy;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.smartenergy.enumeration.*;
import com.smartenergy.model.*;

public class VirtualMap extends Canvas {
    private static final int TOTAL_COLS = 180;
    private static final int TOTAL_ROWS = 112;
    private static final int VISIBLE_COLS = 90;
    private static final int VISIBLE_ROWS = 56;
    private static final double CELL_SIZE = 10;
    private static final double PADDING = 10;
    private static final double DOT_RADIUS = 1;

    private final GraphicsContext gc;
    private final List<GridPoint> selectedPoints = new ArrayList<>();
    private List<GridPoint> redoStack = new ArrayList<>();
    private final List<Sector> sectors = new ArrayList<>();

    private boolean drawMode = false;
    private boolean eraseMode = false;
    private boolean shapeClosed = false;

    private double offsetCol = (TOTAL_COLS - VISIBLE_COLS) / 2.0;
    private double offsetRow = (TOTAL_ROWS - VISIBLE_ROWS) / 2.0;

    private boolean panning = false;
    private double panStartX, panStartY;
    private double panStartOffsetCol, panStartOffsetRow;

    private double zoomFactor = 1.0;
    private static final double MIN_ZOOM = 0.3;
    private static final double MAX_ZOOM = 5.0;
    private static final double ZOOM_STEP = 1.1;

    private TypeSecteur activeSecteurType = null;
    private Function<TypeSecteur, Color> colorResolver = type -> Color.BLUE;

    public VirtualMap() {
        super(VISIBLE_COLS * CELL_SIZE + 2 * PADDING,
              VISIBLE_ROWS * CELL_SIZE + 2 * PADDING);
        this.gc = getGraphicsContext2D();

        drawGrid();
        setupMouseHandler();
        setupZoomHandler();
    }

    public void setDrawMode(boolean bool) {
        drawMode = bool;
        selectedPoints.clear();
        redoStack.clear();
        shapeClosed = false;
        panning = false;
        if (bool) {
            requestFocus();
        }
        drawGrid();
    }

    public void setEraseMode(boolean erase) {
        this.eraseMode = erase;
        if (erase) {
            setDrawMode(false);
        }
        panning = false;
        drawGrid();
    }

    public void reset() {
        selectedPoints.clear();
        redoStack.clear();
        sectors.clear();
        shapeClosed = false;
        drawGrid();
    }

    public void setActiveSecteurType(TypeSecteur type) {
        this.activeSecteurType = type;
        drawGrid();
    }

    public void setColorResolver(Function<TypeSecteur, Color> resolver) {
        this.colorResolver = resolver != null ? resolver : type -> Color.BLUE;
        drawGrid();
    }

    public List<Sector> getSectors() { return sectors; }

    public void setSectors(List<Sector> secteurs) {
        this.sectors.clear();
        this.sectors.addAll(secteurs);
        drawGrid();
    }

    public boolean isDrawMode() { return drawMode; }

    public void zoomIn() { setZoom(zoomFactor * ZOOM_STEP); }
    public void zoomOut() { setZoom(zoomFactor / ZOOM_STEP); }

    public void setZoom(double newZoom) {
        double oldZoom = zoomFactor;
        double clampedZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
        if (clampedZoom == oldZoom) return;

        double centreCol = offsetCol + (TOTAL_COLS - VISIBLE_COLS) / 2.0;
        double centreRow = offsetRow + (TOTAL_ROWS - VISIBLE_ROWS) / 2.0;

        zoomFactor = clampedZoom;
        offsetCol = centreCol - (TOTAL_COLS - VISIBLE_COLS) / 2.0;
        offsetRow = centreRow - (TOTAL_ROWS - VISIBLE_ROWS) / 2.0;

        clampOffset();
        drawGrid();
    }

    public double getZoom() { return zoomFactor; }

    public void zoomToFit() {
        double zoomX = getWidth() / (TOTAL_COLS * CELL_SIZE + 2 * PADDING);
        double zoomY = getHeight() / (TOTAL_ROWS * CELL_SIZE + 2 * PADDING);
        double newZoom = Math.min(zoomX, zoomY);

        zoomFactor = newZoom;

        offsetCol = (VISIBLE_COLS - 1) / 2.0;
        offsetRow = (VISIBLE_ROWS - 1) / 2.0;

        clampOffset();
        drawGrid();
    }

    public void undoLastPoint() {
        if (!drawMode || selectedPoints.isEmpty()) return;
        GridPoint removed = selectedPoints.get(selectedPoints.size() - 1);
        selectedPoints.remove(removed);
        redoStack.add(removed);
        shapeClosed = false;
        drawGrid();
    }

    public void redoLastPoint() {
        if (!drawMode || redoStack.isEmpty()) return;
        GridPoint point = redoStack.remove(redoStack.size() - 1);
        selectedPoints.add(point);
        drawGrid();
    }

    private void setupMouseHandler() {
        setOnMousePressed(this::onMousePressed);
        setOnMouseDragged(this::onMouseDragged);
        setOnMouseReleased(event -> panning = false);
        setOnMouseClicked(this::onMouseClicked);
    }

    private void onMousePressed(MouseEvent event) {
        if (event.getButton() == MouseButton.MIDDLE) {
            panning = true;
            panStartX = event.getX();
            panStartY = event.getY();
            panStartOffsetCol = offsetCol;
            panStartOffsetRow = offsetRow;
        }
    }

    private void onMouseDragged(MouseEvent event) {
        if (!panning) return;
        double dx = (event.getX() - panStartX) / zoomFactor;
        double dy = (event.getY() - panStartY) / zoomFactor;
        offsetCol = panStartOffsetCol - dx / CELL_SIZE;
        offsetRow = panStartOffsetRow - dy / CELL_SIZE;
        clampOffset();
        drawGrid();
    }

    private void onMouseClicked(MouseEvent event) {
        if (panning) return;

        double mouseX = event.getX();
        double mouseY = event.getY();
        double virtualX = (mouseX - getWidth()/2) / zoomFactor + getWidth()/2;
        double virtualY = (mouseY - getHeight()/2) / zoomFactor + getHeight()/2;

        double colD = (virtualX - PADDING) / CELL_SIZE + offsetCol;
        double rowD = (virtualY - PADDING) / CELL_SIZE + offsetRow;
        int col = (int) Math.round(colD);
        int row = (int) Math.round(rowD);

        if (eraseMode && event.getButton() == MouseButton.PRIMARY) {
            if (event.getClickCount() == 1) {
                for (int i = sectors.size() - 1; i >= 0; i--) {
                    Sector s = sectors.get(i);
                    if (isPointInPolygon(virtualX, virtualY, s.points)) {
                        sectors.remove(i);
                        drawGrid();
                        break;
                    }
                }
            }
            return;
        }

        if (drawMode) {
            if (col < 0 || col >= TOTAL_COLS || row < 0 || row >= TOTAL_ROWS) return;
            GridPoint point = new GridPoint(col, row);
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1) {
                if (!selectedPoints.contains(point)) {
                    selectedPoints.add(point);
                    shapeClosed = false;
                    drawGrid();
                }
            } else if (event.getButton() == MouseButton.SECONDARY && event.getClickCount() == 1) {
                finalizeCurrentShape();
            }
        }
    }

    private void clampOffset() {
        double minOffsetCol = 46.0 / zoomFactor - 45.0;
        double maxOffsetCol = 134.0 - 46.0 / zoomFactor;
        if (minOffsetCol > maxOffsetCol) {
            offsetCol = 44.5;
        } else {
            offsetCol = Math.max(minOffsetCol, Math.min(maxOffsetCol, offsetCol));
        }

        double minOffsetRow = 29.0 / zoomFactor - 28.0;
        double maxOffsetRow = 83.0 - 29.0 / zoomFactor;
        if (minOffsetRow > maxOffsetRow) {
            offsetRow = 27.5;
        } else {
            offsetRow = Math.max(minOffsetRow, Math.min(maxOffsetRow, offsetRow));
        }
    }

    private void setupZoomHandler() {
        setOnScroll(event -> {
            if (event.getDeltaY() == 0) return;
            else if (event.getDeltaY() > 0) zoomIn(); else zoomOut();
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

    private void drawGrid() {
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
            drawTranslatedPolygon(s.points, color);
        }

        if (drawMode && !eraseMode) {
            double invZoom = 1.0 / zoomFactor;

            double minColD = offsetCol + 45.0 - 46.0 * invZoom;
            double maxColD = offsetCol + 45.0 + 46.0 * invZoom;
            int startCol = (int) Math.floor(minColD);
            int endCol   = (int) Math.ceil(maxColD);
            startCol = Math.max(0, Math.min(startCol, TOTAL_COLS - 1));
            endCol   = Math.max(0, Math.min(endCol,   TOTAL_COLS - 1));

            double minRowD = offsetRow + 28.0 - 29.0 * invZoom;
            double maxRowD = offsetRow + 28.0 + 29.0 * invZoom;
            int startRow = (int) Math.floor(minRowD);
            int endRow   = (int) Math.ceil(maxRowD);
            startRow = Math.max(0, Math.min(startRow, TOTAL_ROWS - 1));
            endRow   = Math.max(0, Math.min(endRow,   TOTAL_ROWS - 1));

            for (int row = startRow; row <= endRow; row++) {
                for (int col = startCol; col <= endCol; col++) {
                    double x = PADDING + (col - offsetCol) * CELL_SIZE;
                    double y = PADDING + (row - offsetRow) * CELL_SIZE;
                    GridPoint gp = new GridPoint(col, row);
                    gc.setFill(selectedPoints.contains(gp) ? Color.RED : Color.BLACK);
                    gc.fillOval(x - DOT_RADIUS, y - DOT_RADIUS, DOT_RADIUS * 2, DOT_RADIUS * 2);
                }
            }

            if (!selectedPoints.isEmpty()) {
                double[] xs = selectedPoints.stream()
                        .mapToDouble(p -> PADDING + (p.col - offsetCol) * CELL_SIZE)
                        .toArray();
                double[] ys = selectedPoints.stream()
                        .mapToDouble(p -> PADDING + (p.row - offsetRow) * CELL_SIZE)
                        .toArray();
                Color drawColor = (activeSecteurType == null) ? Color.BLUE : colorResolver.apply(activeSecteurType);
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

    private void drawTranslatedPolygon(List<GridPoint> points, Color color) {
        if (points == null || points.size() < 3) return;
        double[] xs = points.stream()
                .mapToDouble(p -> PADDING + (p.col - offsetCol) * CELL_SIZE)
                .toArray();
        double[] ys = points.stream()
                .mapToDouble(p -> PADDING + (p.row - offsetRow) * CELL_SIZE)
                .toArray();

        gc.setFill(color);
        gc.fillPolygon(xs, ys, xs.length);
        gc.setStroke(color);
        gc.setLineWidth(2);
        gc.strokePolygon(xs, ys, xs.length);
    }

    private boolean isPointInPolygon(double canvasX, double canvasY, List<GridPoint> points) {
        if (points == null || points.size() < 3) return false;
        double[] xs = points.stream().mapToDouble(p -> PADDING + p.col * CELL_SIZE).toArray();
        double[] ys = points.stream().mapToDouble(p -> PADDING + p.row * CELL_SIZE).toArray();
        int n = xs.length;
        boolean inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = xs[i], yi = ys[i];
            double xj = xs[j], yj = ys[j];
            if ((yi > canvasY) != (yj > canvasY) &&
                canvasX < (xj - xi) * (canvasY - yi) / (yj - yi) + xi) {
                inside = !inside;
            }
        }
        return inside;
    }
}
