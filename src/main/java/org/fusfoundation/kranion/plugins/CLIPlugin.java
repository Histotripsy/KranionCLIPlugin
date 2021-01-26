package org.fusfoundation.kranion.plugins;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.fusfoundation.kranion.Button;
import org.fusfoundation.kranion.Renderable;
import org.fusfoundation.kranion.FlyoutPanel;
import org.fusfoundation.kranion.GUIControl.HPosFormat;
import org.fusfoundation.kranion.ListControl;
import org.fusfoundation.kranion.MessageBoxDialog;
import org.fusfoundation.kranion.Rectangle;
import org.fusfoundation.kranion.RenderLayer;
import org.fusfoundation.kranion.TextBox;
import org.fusfoundation.kranion.plugin.Plugin;

import org.fusfoundation.kranion.view.View;
import org.fusfoundation.kranion.model.*;
import org.fusfoundation.kranion.controller.Controller;

/**
 *
 * @author mkomaiha
 */
public class CLIPlugin implements Plugin, ActionListener {
    private Model model;
    private View view;
    private Controller controller;
    private CLI cliThread;
    private int cliPort = 9000;
    private ListControl cmdList;
    private MessageBoxDialog messageDialog;
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
//            RenderLayer
            RenderLayer overlay = (RenderLayer)Renderable.lookupByTag("DefaultView.overlay_layer");
            messageDialog = new MessageBoxDialog("");
            messageDialog.setTag("mbCLIDialog");
            messageDialog.setBounds(0,0,500,500); // Set better bounds? Allow for overflow!
            if (overlay != null) {
                overlay.addChild(messageDialog);
            }
//            view.
            FlyoutPanel panel = (FlyoutPanel) mainPanel;
            Rectangle bounds = panel.getBounds();
            int height = bounds.getIntHeight();
            Button cliStateToggle = new Button(Button.ButtonType.TOGGLE_BUTTON, 50, height - 75, 240, 25, controller);
            cliStateToggle.setTitle("CLI State");
            cliStateToggle.setIndicatorRadius(8f);
            cliStateToggle.setCommand("toggleCLI");
            cliStateToggle.setTag("toggleCLIStateBtn");
            
            TextBox portNumBox = new TextBox(190, height - 105, 100, 25, Integer.toString(cliPort), controller);
            portNumBox.setIsNumeric(true);
            portNumBox.setTextEditable(true);
            portNumBox.setTextHorzAlignment(HPosFormat.HPOSITION_RIGHT);
            portNumBox.setTitle("Listen Port Number:");
            portNumBox.setCommand("portNumChange");
            
            this.cmdList = new ListControl(300, 50, Math.max(bounds.getIntWidth() - 300 - 10, 300), height - 100, controller);
            cmdList.setCommand("cmdSelected");
            cmdList.setTag("lcCLICmdList");
            
            panel.addChild("CLIPlugin",cliStateToggle);
            panel.addChild("CLIPlugin",portNumBox);
            panel.addChild("CLIPlugin",cmdList);
            
            this.cliThread = new CLI(model, view, cliPort);
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
        String command = e.getActionCommand();
        Object source = e.getSource();
        switch (command) {
            case "toggleCLI" -> {
                boolean cliOn = ((Button)source).getIndicator();
//                            portNumBox.setTextEditable(true);
                if (cliOn) {
                    if (cliThread.isStopped() && !cliThread.isRunning()) {
                        try {
                            cliThread.join();
                            System.out.println(cliPort);
                            cliThread = new CLI(this.model, this.view, cliPort);
                            cliThread.start();
//                            portNumBox.setTextEditable(false);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(CLIPlugin.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                        // Failed to start
                        ((Button)source).setIndicator(false);
                    }
                } else {
                    cliThread.interrupt();
                }
            }
            case "portNumChange" -> {
                TextBox portBox = (TextBox)source;
                String portNum = portBox.getText();
                portNum = portNum.replaceAll("[^0-9]","");
                if (portNum.length() > 0) {
                    int number = Integer.parseInt(portNum);
                    if (number > 65353) { // max port number?
                        portNum = "65353";
                    }
                } else {
                    portNum = "0";
                }
                portBox.setText(portNum);
                cliPort = Integer.parseInt(portNum);
            }
            case "doubleClick" -> {
                if (e.getSource() instanceof ListControl) {
                    ListControl lc = (ListControl)e.getSource();
                    CLIError err = (CLIError)lc.getSelectedValue();
                    messageDialog.setDialogTitle(err.title);
                    messageDialog.setMessageText(err.body);
                    messageDialog.open();
                    System.out.println(err.body);
                }
            }
        }
    }
}
