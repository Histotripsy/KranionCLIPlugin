package org.fusfoundation.kranion.plugins;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.fusfoundation.kranion.Button;
import org.fusfoundation.kranion.Renderable;
import org.fusfoundation.kranion.FlyoutPanel;
import org.fusfoundation.kranion.plugin.Plugin;

import org.fusfoundation.kranion.view.View;
import org.fusfoundation.kranion.model.*;
import org.fusfoundation.kranion.controller.Controller;
import org.fusfoundation.kranion.model.image.ImageVolume;
import org.fusfoundation.kranion.model.image.ImageVolume4D;

import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mkomaiha
 */
public class CLIPlugin implements Plugin, ActionListener {
    private Model model;
    private View view;
    private Controller controller;

    @Override
    public String getName() {
        return "CLIPlugin";
    }
    
    @Override
    public void init(Controller controller) {
        this.model = controller.getModel();
        this.view = controller.getView();
        this.controller = controller;

        System.out.println("******* Hello from CLIPlugin !! ****************");

        controller.addActionListener(this);

        Renderable mainPanel = Renderable.lookupByTag("MainFlyout");
        if (mainPanel != null && mainPanel instanceof FlyoutPanel) {
            FlyoutPanel panel = (FlyoutPanel) mainPanel;
            panel.addChild("HydrophoneFiducials", new Button(Button.ButtonType.BUTTON, 10, 240, 220, 25, controller).setTitle("Find Fiducials").setCommand("findFiducials"));
            panel.addChild("HydrophoneFiducials", new Button(Button.ButtonType.BUTTON, 10, 190, 200, 25, controller).setTitle("Show my loc.").setCommand("ShowMyLoc"));
        }
    }
    
    @Override
    public void release() {
        controller.removeActionListener(this);
        controller = null;
        model = null;
        view = null;       
    }    

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("findFiducials")) {
            ImageVolume4D image = (ImageVolume4D)model.getCtImage();
            if (image != null && model != null && view != null) {
                System.out.println("HI");
            }
        }                
        else if (e.getActionCommand().equals("ShowMyLoc")) {
            try {

                System.out.println(" ");
                System.out.println("** My Location is: ");

                try {
                    Vector3f transCT = (Vector3f) model.getCtImage().getAttribute("ImageTranslation");
                    System.out.println("** CT: X axis: " + transCT.x + ",Y axis: " + transCT.y + ", Z axis: " + transCT.z);
                } catch (Exception ect) {
                    System.out.println("\t No CT found.");
                    //ect.printStackTrace();
                }

                try {
                    Vector3f transMR = (Vector3f) model.getMrImage(0).getAttribute("ImageTranslation");
                    System.out.println("** MR: X axis: " + transMR.x + ",Y axis: " + transMR.y + ", Z axis: " + transMR.z);
                } catch (Exception emr) {
                    System.out.println("\t No MR found.");
                    //emr.printStackTrace();
                }
                System.out.println(" ");

            } catch (Exception ei) {
                ei.printStackTrace();
            }
        }
    }
}
