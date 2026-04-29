package com.smartenergy;

import com.smartenergy.enumeration.*;
import com.smartenergy.model.*;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class App extends Application {
    private BatimentService batimentService = new BatimentService();
    private AnalyseService analyseService = new AnalyseService();

    private List<Consommation> consommations = new ArrayList<>();

    private ListView<Batiment> listeBatiments = new ListView<>();
    private ListView<Consommation> listeConsommations = new ListView<>();

    private Label dashboard = new Label("Tableau de bord");

    private static Scene scene;

    private VirtualMap map;
    private static final String MAP_SAVE_FILE = "sectors.dat";

    @Override
    public void start(Stage stage) {
        Image appIcon = new Image(getClass().getResourceAsStream("icon.png"));
        stage.getIcons().add(appIcon);

        TabPane tabs = new TabPane();

        tabs.getTabs().add(new Tab("Carte", vueCarte()));
        tabs.getTabs().add(new Tab("Bâtiments", vueBatiments()));
        tabs.getTabs().add(new Tab("Consommations", vueConsommations()));
        tabs.getTabs().add(new Tab("Tableau de bord", vueDashboard()));
        tabs.getTabs().add(new Tab("Graphiques", vueGraphiques()));
        tabs.getTabs().add(new Tab("Analyse", vueAnalyse()));

        scene = new Scene(tabs, 1280, 720);

        // raccourci pour l'objet map
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (map != null && map.isDrawMode()) {
                if (event.isControlDown() && event.isShiftDown() && event.getCode() == KeyCode.Z) {
                    map.redoLastPoint();
                    event.consume();
                } else if (event.isControlDown() && event.getCode() == KeyCode.Z) {
                    map.undoLastPoint();
                    event.consume();
                }
            }
        });

        stage.setTitle("Smart Energy Manager");
        stage.setScene(scene);
        stage.show();
    }

    private GridPane vueCarte() {
        this.map = new VirtualMap();

        ToggleGroup modeGroup = new ToggleGroup();

        ToggleButton btnDraw = new ToggleButton("Dessiner");
        btnDraw.setToggleGroup(modeGroup);
        btnDraw.textProperty().bind(
            Bindings.when(btnDraw.selectedProperty())
                    .then("Arrêter le dessin")
                    .otherwise("Dessiner")
        );
        btnDraw.setOnAction(event -> {
                if (btnDraw.isSelected()) {
                    map.setEraseMode(false);
                    map.setDrawMode(true);
                } else {
                    map.setDrawMode(false);
                }
            });

        ToggleButton btnErase = new ToggleButton("Effacer");
        btnErase.setToggleGroup(modeGroup);
        btnErase.textProperty().bind(
            Bindings.when(btnErase.selectedProperty())
                    .then("Arrêter l'effaçage")
                    .otherwise("Effacer")
        );
        btnErase.setOnAction(event -> {
                if (btnErase.isSelected()) {
                    map.setEraseMode(true);
                } else {
                    map.setEraseMode(false);
                }
            });

        Button btnShowAll = new Button("Recadrer");
        btnShowAll.setOnAction(event -> map.zoomToFit());

        Button btnSave = new Button("Sauvegarder");
        btnSave.setOnAction(event -> saveSectors(map));

        Button btnClear = new Button("Réinitialiser");
        btnClear.setOnAction(event -> {
            map.reset();
            deleteFile(MAP_SAVE_FILE);
        });

        Button btnTuto = new Button("Tuto");
        btnTuto.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Tuto");
            alert.setHeaderText(null);
            alert.setContentText("""
                Clic gauche : sélectionner les points
                Clic droit : confirmer la forme
                Ctrl+Z : annuler le placement d'un point
                Ctrl+Shift+Z : replacer un point précédemment annuler
                """);
            Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
            Image appIcon = ((Stage) map.getScene().getWindow()).getIcons().get(0);
            alertStage.getIcons().add(appIcon);
            alert.show();
        });

        ComboBox<TypeSecteur> sectorTypeSelector = new ComboBox<>();
        sectorTypeSelector.getItems().setAll(TypeSecteur.values());
        sectorTypeSelector.getSelectionModel().select(TypeSecteur.BATIMENT);
        map.setColorResolver(this::getColorForSector);
        map.setActiveSecteurType(TypeSecteur.BATIMENT);

        sectorTypeSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            map.setActiveSecteurType(newVal != null ? newVal : null);
        });


        HBox drawBox = new HBox(15, btnDraw, btnErase, btnSave, btnClear, btnShowAll, btnTuto);
        HBox rightBox = new HBox(10, sectorTypeSelector);
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new javafx.geometry.Insets(16));
        grid.add(drawBox, 0, 0);
        grid.add(map, 0, 1);
        grid.add(rightBox, 1, 0);

        loadSectors(map);

        return grid;
    }

    private Color getColorForSector(TypeSecteur type) {
        return switch (type) {
            case BATIMENT -> Color.web("#b2b2b1");
            case EAU -> Color.web("#bed7f9");
            case ESPACE_VERT -> Color.web("#cce1ab");
            case ROUTE -> Color.web("#ffffff");
        };
    }

    private VBox vueBatiments() {
        TextField nom = new TextField();
        nom.setPromptText("Nom");

        ComboBox<TypeBatiment> type = new ComboBox<>();
        type.getItems().addAll(TypeBatiment.values());
        type.setPromptText("Type");

        TextField adresse = new TextField();
        adresse.setPromptText("Adresse");

        TextField surface = new TextField();
        surface.setPromptText("Surface");

        Button ajouter = new Button("Créer");
        Button modifier = new Button("Modifier");
        Button supprimer = new Button("Supprimer");
        Button cloner = new Button("Cloner");

        ajouter.setOnAction(e -> {
            try {
                Batiment b = batimentService.ajouter(
                        nom.getText(),
                        type.getValue(),
                        adresse.getText(),
                        Double.parseDouble(surface.getText())
                );
                listeBatiments.getItems().add(b);
                nom.clear();
                adresse.clear();
                surface.clear();
            } catch (Exception ex) {
                alerte("Erreur", "Vérifie les informations du bâtiment.");
            }
        });

        modifier.setOnAction(e -> {
            Batiment b = listeBatiments.getSelectionModel().getSelectedItem();
            if (b != null) {
                b.setNom(nom.getText());
                b.setType(type.getValue());
                b.setAdresse(adresse.getText());
                b.setSurface(Double.parseDouble(surface.getText()));
                listeBatiments.refresh();
            }
        });

        supprimer.setOnAction(e -> {
            Batiment b = listeBatiments.getSelectionModel().getSelectedItem();
            if (b != null) {
                batimentService.supprimer(b);
                listeBatiments.getItems().remove(b);
            }
        });

        cloner.setOnAction(e -> {
            Batiment b = listeBatiments.getSelectionModel().getSelectedItem();
            if (b != null) {
                Batiment copie = batimentService.cloner(b);
                listeBatiments.getItems().add(copie);
            }
        });

        listeBatiments.setOnMouseClicked(e -> {
            Batiment b = listeBatiments.getSelectionModel().getSelectedItem();
            if (b != null) {
                nom.setText(b.getNom());
                type.setValue(b.getType());
                adresse.setText(b.getAdresse());
                surface.setText(String.valueOf(b.getSurface()));
            }
        });

        HBox boutons = new HBox(10, ajouter, modifier, supprimer, cloner);
        VBox form = new VBox(10, nom, type, adresse, surface, boutons);

        VBox root = new VBox(15, new Label("Gestion des bâtiments"), form, listeBatiments);
        root.setStyle("-fx-padding: 20;");
        return root;
    }

    private VBox vueConsommations() {
        ComboBox<Batiment> batimentBox = new ComboBox<>();
        batimentBox.setPromptText("Bâtiment");
        batimentBox.setOnMouseClicked(e -> {
            batimentBox.getItems().setAll(batimentService.getBatiments());
        });

        ComboBox<TypeEnergie> energieBox = new ComboBox<>();
        energieBox.getItems().addAll(TypeEnergie.values());
        energieBox.setPromptText("Type énergie");

        TextField quantite = new TextField();
        quantite.setPromptText("Quantité");

        TextField cout = new TextField();
        cout.setPromptText("Coût estimé");

        Button ajouter = new Button("Ajouter consommation");
        Button donneesTest = new Button("Générer données test");

        ajouter.setOnAction(e -> {
            try {
                Consommation c = new Consommation(
                        batimentBox.getValue(),
                        LocalDateTime.now(),
                        energieBox.getValue(),
                        Double.parseDouble(quantite.getText()),
                        Double.parseDouble(cout.getText())
                );

                consommations.add(c);
                listeConsommations.getItems().add(c);
                majDashboard();

                quantite.clear();
                cout.clear();
            } catch (Exception ex) {
                alerte("Erreur", "Vérifie les informations de consommation.");
            }
        });

        donneesTest.setOnAction(e -> genererDonneesTest());

        VBox root = new VBox(15,
                new Label("Gestion des consommations"),
                batimentBox,
                energieBox,
                quantite,
                cout,
                new HBox(10, ajouter, donneesTest),
                listeConsommations
        );

        root.setStyle("-fx-padding: 20;");
        return root;
    }

    private VBox vueDashboard() {
        Button actualiser = new Button("Actualiser");

        actualiser.setOnAction(e -> majDashboard());

        VBox root = new VBox(20, new Label("Tableau de bord principal"), dashboard, actualiser);
        root.setStyle("-fx-padding: 20;");
        return root;
    }

    private VBox vueGraphiques() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Consommation par bâtiment");

        Button actualiser = new Button("Actualiser graphique");

        actualiser.setOnAction(e -> {
            chart.getData().clear();

            XYChart.Series<String, Number> serie = new XYChart.Series<>();
            serie.setName("Consommation");

            for (Batiment b : batimentService.getBatiments()) {
                double total = 0;

                for (Consommation c : consommations) {
                    if (c.getBatiment() == b) {
                        total += c.getQuantite();
                    }
                }

                serie.getData().add(new XYChart.Data<>(b.getNom(), total));
            }

            chart.getData().add(serie);
        });

        VBox root = new VBox(20, chart, actualiser);
        root.setStyle("-fx-padding: 20;");
        return root;
    }

    private VBox vueAnalyse() {
        Label resultat = new Label("Analyse");

        Button analyser = new Button("Analyser");

        analyser.setOnAction(e -> {
            Batiment plus = analyseService.batimentPlusConsommateur(consommations);
            TypeEnergie energie = analyseService.energieDominante(consommations);
            boolean pic = analyseService.existePic(consommations);

            String texte = "";

            texte += "Bâtiment qui consomme le plus : " + (plus == null ? "Aucun" : plus.getNom()) + "\n";
            texte += "Type d'énergie dominant : " + (energie == null ? "Aucun" : energie) + "\n";
            texte += "Estimation facture mensuelle : " + analyseService.estimationMensuelle(consommations) + " €\n";
            texte += "Pics de consommation : " + (pic ? "Oui" : "Non") + "\n";

            resultat.setText(texte);
        });

        VBox root = new VBox(20, new Label("Analyse énergétique"), analyser, resultat);
        root.setStyle("-fx-padding: 20;");
        return root;
    }

    private void genererDonneesTest() {
        if (batimentService.getBatiments().isEmpty()) {
            alerte("Erreur", "Crée d'abord un bâtiment.");
            return;
        }

        Random r = new Random();

        for (Batiment b : batimentService.getBatiments()) {
            for (int i = 0; i < 5; i++) {
                TypeEnergie energie = TypeEnergie.values()[r.nextInt(TypeEnergie.values().length)];
                double qte = 50 + r.nextInt(600);
                double cout = qte * 0.2;

                Consommation c = new Consommation(
                        b,
                        LocalDateTime.now().minusDays(i),
                        energie,
                        qte,
                        cout
                );

                consommations.add(c);
                listeConsommations.getItems().add(c);
            }
        }

        majDashboard();
    }

    private void majDashboard() {
        Batiment plus = analyseService.batimentPlusConsommateur(consommations);

        String texte = "";
        texte += "Consommation totale du jour : " + analyseService.totalDuJour(consommations) + "\n";
        texte += "Consommation totale générale : " + analyseService.totalConsommation(consommations) + "\n";
        texte += "Coût énergétique estimé : " + analyseService.totalCout(consommations) + " €\n";
        texte += "Bâtiment le plus consommateur : " + (plus == null ? "Aucun" : plus.getNom()) + "\n";
        texte += "Alertes : " + (analyseService.existePic(consommations) ? "Pic détecté" : "Aucune alerte") + "\n";
        texte += "Indicateur de performance : " + indicateurPerformance();

        dashboard.setText(texte);
    }

    private String indicateurPerformance() {
        double total = analyseService.totalConsommation(consommations);

        if (total < 500) return "Bonne consommation";
        if (total < 1500) return "Consommation moyenne";
        return "Consommation élevée";
    }

    private void alerte(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setContentText(message);
        alert.show();
    }

    private void saveSectors(VirtualMap map) {
        List<Sector> sectors = map.getSectors();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(MAP_SAVE_FILE))) {
            oos.writeObject(sectors);
            System.out.println("secteurs sauvegardés : " + sectors.size());
        } catch (IOException e) {
            alerte("Erreur", "Impossible de sauvegarder les sectors.\n" + e.getMessage());
        }
    }

    private boolean loadSectors(VirtualMap map) {
        File file = new File(MAP_SAVE_FILE);
        if (!file.exists()) return false;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            @SuppressWarnings("unchecked")
            List<Sector> sectors = (List<Sector>) ois.readObject();
            map.setSectors(sectors);
            System.out.println("sectors chargés : " + sectors.size());
            return true;
        } catch (IOException | ClassNotFoundException e) {
            alerte("Erreur", "Impossible de charger les sectors.\n" + e.getMessage());
            return false;
        }
    }

    private void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                System.out.println("Fichier de sauvegarde supprimé.");
            } else {
                alerte("Erreur", "Impossible de supprimer le fichier de sauvegarde.");
            }
        }
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}
