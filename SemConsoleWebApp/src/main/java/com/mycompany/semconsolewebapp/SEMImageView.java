/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.semconsolewebapp;

import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 *
 * @author gmein
 */
//
// concept:
// A SEMImageView is an AnchorPane with a GridPane for the images, a sliders on the right side for contrast and brightness, 
// and a metabadge in the bottom right corner. The GridPane holds up to 4 StackPanes with an ImageView and a 
// channel-only or full metabadge
//
public class SEMImageView extends AnchorPane {

    SEMImage si;
    ImageView[] aiv = new ImageView[4];
    StackPane[] asp = new StackPane[4];
    MiniBadge[] amb = new MiniBadge[4];
    MetaBadge mb;
    Stage stage;
    double dpi;
    boolean isPhoto;

    SEMImageView(SEMImage si, Stage stage, double dpi, boolean isPhoto) {
        this.si = si;
        this.stage = stage;
        this.isPhoto = isPhoto;
        this.dpi = dpi;

        for (int i = 0; i < si.channels; i++) {
            aiv[i] = new ImageView();
            this.aiv[i] = new ImageView(si.images[i]);
            this.aiv[i].setSmooth(false);
            this.aiv[i].setCache(true);

            // make the 3 secondary views appear in a slightly different color effect
            if (i != 0) {
                ColorAdjust colorAdjust = new ColorAdjust();
                colorAdjust.setHue(new double[]{0, -1, -0.2, 0.3}[i]);
//                colorAdjust.setBrightness(0.1);
//                colorAdjust.setSaturation(0.2);
//                colorAdjust.setContrast(0.1);
                this.aiv[i].setEffect(colorAdjust);
            }

            // add to their own StackPane so we can slap a MetaBadge on top
            this.asp[i] = new StackPane();
            asp[i].getChildren().add(this.aiv[i]);

        }

        // add 4 StackPanes to the grid pane, anchor gridpane at top left
        GridPane gp = new GridPane();
        gp.add(asp[0], 0, 0);
        if (asp[1] != null) {
            gp.add(asp[1], 1, 0);
        }
        if (asp[2] != null) {
            gp.add(asp[2], 0, 1);
        }
        if (asp[3] != null) {
            gp.add(asp[3], 1, 1);
        }
        gp.setHgap(4.0);
        gp.setVgap(4.0);

        AnchorPane.setTopAnchor(gp, 4.0);
        AnchorPane.setBottomAnchor(gp, 4.0);
        super.getChildren().add(gp);

        // contrast and brightness controls
        Slider weight = new Slider();
        if (!isPhoto) {
            weight.valueProperty().addListener((f) -> {
                si.dContrast = weight.getValue();
                if (!isPhoto) {
                    Console.dContrast = weight.getValue();
                }
            });
            weight.setPrefHeight(300);
            weight.prefHeightProperty().bind(stage.heightProperty().subtract(450));
            weight.setMin(0.002);
            weight.setMax(1.0);
            weight.setValue(isPhoto ? si.dContrast : Console.dContrast);
            weight.setShowTickMarks(true);
            weight.setShowTickLabels(true);
            weight.setMajorTickUnit(0.1);
            weight.setOrientation(Orientation.VERTICAL);
        }

        Slider contrast = new Slider();
        contrast.valueProperty().addListener((f) -> {
            si.dContrast = contrast.getValue();
            if (!isPhoto) {
                Console.dContrast = contrast.getValue();
                setSEMImage(si);
                autoContrast2();
            }
        });
        contrast.prefHeightProperty().bind(stage.heightProperty().subtract(500));
        contrast.setMin(0.0);
        contrast.setMax(5.0);
        contrast.setValue(isPhoto ? si.dContrast : Console.dContrast);
        contrast.setShowTickMarks(true);
        contrast.setShowTickLabels(true);
        contrast.setMajorTickUnit(1.0);
        contrast.setOrientation(Orientation.VERTICAL);
        contrast.setDisable(Console.autoContrast);

        Slider brightness = new Slider();
        brightness.valueProperty().addListener((f) -> {
            si.dBrightness = brightness.getValue();
            if (!isPhoto) {
                Console.dBrightness = brightness.getValue();
                setSEMImage(si);
                autoContrast2();
            }

        });
        brightness.prefHeightProperty().bind(stage.heightProperty().subtract(500));
        brightness.setMin(0.0);
        brightness.setMax(1.0);
        brightness.setValue(isPhoto ? si.dBrightness : Console.dBrightness);
        brightness.setShowTickMarks(true);
        brightness.setShowTickLabels(true);
        brightness.setMajorTickUnit(0.1);
        brightness.setOrientation(Orientation.VERTICAL);
        brightness.setDisable(Console.autoContrast);

        // put the controls together just so
        CheckBox auto = new CheckBox("Auto");
        auto.setSelected(true);
        auto.selectedProperty().addListener((e) -> {
            Console.autoContrast = auto.isSelected();
            contrast.setDisable(Console.autoContrast);
            brightness.setDisable(Console.autoContrast);
        });
        HBox cb = new HBox();
        cb.getChildren().addAll(contrast, brightness);
        VBox cba = new VBox();
        cba.getChildren().addAll(cb, auto);
        HBox controls = new HBox();
        controls.getChildren().addAll(cba);
        if (!isPhoto) {
            controls.getChildren().addAll(weight);
        }

        AnchorPane.setTopAnchor(controls, 4.0);
        AnchorPane.setRightAnchor(controls, 4.0);
        super.getChildren().add(controls);

        // metabadge for bottom right corner
        this.mb = new MetaBadge(si, si.channels > 1 ? -1 : si.capturedChannels[0], dpi);
        AnchorPane.setBottomAnchor(mb, 4.0);
        AnchorPane.setRightAnchor(mb, 4.0);
        super.getChildren().add(mb);
    }

    public void setSEMImage(SEMImage si) {
        this.si = si;

        // get rid of stale channel badges
        for (int i = 0; i < 4; i++) {
            if (amb[i] != null) {
                asp[i].getChildren().remove(amb[i]);
            }
        }

        // set images and make new mini badges
        for (int i = 0; i < si.channels; i++) {
            this.aiv[i].setImage(si.displayImages[i] != null ? si.displayImages[i] : si.images[i]);

            if (si.channels > 1) {
                MiniBadge mb = new MiniBadge(this.si.capturedChannels[i]);
                StackPane.setAlignment(mb, Pos.BOTTOM_RIGHT);
                asp[i].getChildren().add(mb);
            }

            setSizeNormal(this.aiv[i], i, this.stage, this.isPhoto);
        }

        // metabadge for bottom right corner
        if (this.mb != null) {
            this.getChildren().remove(this.mb);
        }
        this.mb = new MetaBadge(si, si.channels > 1 ? -1 : si.capturedChannels[0], dpi);
        AnchorPane.setBottomAnchor(mb, 4.0);
        AnchorPane.setRightAnchor(mb, 4.0);
        super.getChildren().add(mb);
    }

    private void setSizeNormal(ImageView iv, int channel, Stage stage, boolean isPhoto) {
        double margin = isPhoto ? 0 : 260;
        switch (this.si.channels) {
            case 2:
                if (channel < 2) {
                    iv.fitHeightProperty().bind(stage.widthProperty().subtract(margin).multiply(3).divide(8));
                    iv.fitWidthProperty().bind(stage.widthProperty().subtract(margin).divide(2));
                } else {
                    iv.fitHeightProperty().unbind();
                    iv.fitWidthProperty().unbind();
                    iv.setFitWidth(0);
                    iv.setFitHeight(0);
                }
                break;
            case 1:
                if (channel < 1) {
                    iv.fitHeightProperty().bind(stage.heightProperty().subtract(margin));
                    iv.fitWidthProperty().bind(stage.heightProperty().subtract(margin).multiply(4).divide(3));
                } else {
                    iv.fitHeightProperty().unbind();
                    iv.fitWidthProperty().unbind();
                    iv.setFitWidth(0);
                    iv.setFitHeight(0);
                }
                break;
            default:
                iv.fitHeightProperty().bind(stage.heightProperty().subtract(margin).divide(2));
                iv.fitWidthProperty().bind(stage.heightProperty().subtract(margin).divide(2).multiply(4).divide(3));
                break;
        }
    }

    int adjustPixelValue(int value, double contrast, double brightness) {
        //return value;
        int newValue = (int) (((double) value) * contrast + brightness * 4096);
        if (newValue > 4095) {
            newValue = 4095;
        } else if (newValue < 0) {
            newValue = 0;
        }
        return newValue;
    }

    void adjustLevels(double contrast, double brightness) {
        for (int c = 0; c < si.channels; c++) {

            WritableImage newImage = new WritableImage(si.width, si.height);
            PixelWriter pw = newImage.getPixelWriter();

            for (int line = 0; line < si.height; line++) {
                int[] line2 = new int[si.width];
                try {
                    si.readers[c].getPixels(0, line, si.width, 1, si.format, line2, 0, si.width);
                    for (int i = 0; i < si.width; i++) {
                        int intensity = si.intensityFromARGB(line2[i]);
                        intensity = adjustPixelValue(intensity, contrast, brightness);
                        line2[i] = si.ARGBFromIntensity(intensity);
                    }
                    // write rawBuffer into images[c]
                    pw.setPixels(0, line, si.width, 1, si.format, line2, 0, si.width);
                } catch (Exception e) {
                    System.err.println("adjustLevels: write failed, " + e.getMessage());
                    e.printStackTrace(System.err);
                    return;
                }
            }

            si.images[c] = newImage;
            si.displayImages[c] = null;
        }
    }

    void determineLevels() {
        // need to perform autoContrast for every pixel
        // todo: add brightness, add UI controls
        for (int c = 0; c < si.channels; c++) {

            // determine image ranges for each channel
            si.rangeMin[c] = 4096;
            si.rangeMax[c] = 0;

            for (int line = 0; line < si.height; line++) {
                int[] line2 = new int[si.width];
                try {
                    si.readers[c].getPixels(0, line, si.width, 1, si.format, line2, 0, si.width);
                    for (int i = 0; i < si.width; i++) {
                        int intensity = si.intensityFromARGB(line2[i]);
                        si.rangeMin[c] = Math.min(si.rangeMin[c], intensity);
                        si.rangeMax[c] = Math.max(si.rangeMax[c], intensity);
                    }
                } catch (Exception e) {
                    System.err.println("determineLevels: range read failed, " + e.getMessage());
                    System.err.println("  si:"+si);
                    System.err.println("  si.readers[c]:"+si.readers[c]);
                    System.err.println("  si.format:"+si.format);
                    
                    e.printStackTrace(System.err);
                    return;
                }
            }
        }
    }

    void autoContrast2() {
        if (Console.autoContrast) {
            determineLevels();
            Console.dContrast = 4096 / (double) (si.rangeMax[0] - si.rangeMin[0]);
            Console.dBrightness = Math.max(0, 500 - si.rangeMin[0]) / (double) 4096;
        }
        adjustLevels(Console.dContrast, Console.dBrightness);
    }

}
