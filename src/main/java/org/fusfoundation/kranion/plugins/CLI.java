/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fusfoundation.kranion.plugins;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import org.fusfoundation.kranion.model.Model;
import org.fusfoundation.kranion.model.image.ImageVolume;
import org.fusfoundation.kranion.view.View;

import java.net.*;
import java.io.*; 
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author mkomaiha
 */

public class CLI extends Thread {
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(true);
    private Model model = null;
    private View view = null;
    private int portNum = 0;
    
    public CLI(Model model, View view, int portNum) {
    	this.model = model;
        this.view = view;
        this.portNum = portNum;
    }
    
    @Override
    public void interrupt() {
        running.set(false);
    }
    
    public boolean isRunning() {
        return running.get();
    }
    
    public boolean isStopped() {
        return stopped.get();
    }
    
    private void processInput(InputStreamReader reader) {
        try {
            JsonObject jsonObject = JsonParser.parseReader​(reader).getAsJsonObject();
//            Send a JSONArray with multiple update types that we loop over?
//           [
//             {
//               "msgType": "REGISTRATION",
//               "attribute": "quaternion",
//               "data": [x,y,z,w]
//             },
//             {
//               "msgType": "REGISTRATION",
//               "attribute": "translation",
//               "data": [x,y,z]
//             }, ...{}
//           ]
            if (jsonObject.has("msgType")) {
                JsonElement msgType = jsonObject.get("msgType");
                switch (msgType.getAsString()) {
                    case "REGISTRATION" -> {
                        System.out.println("Registration");
                        if (jsonObject.has("attribute")) {
                            JsonElement attrib = jsonObject.get("attribute");
                            switch (attrib.getAsString()) {
                                case "quaternion" -> {
                                    if (jsonObject.has("data")) {
                                        JsonElement data = jsonObject.get("data");
                                        if (data.isJsonArray()) {
                                            JsonArray dataArr = data.getAsJsonArray();
                                            if (dataArr.size() == 4) {
                                                System.out.println(data);
                                                System.out.println("Toggled ON");
                                                Quaternion obj = new Quaternion(dataArr.get(0).getAsFloat(),
                                                        dataArr.get(1).getAsFloat(),
                                                        dataArr.get(2).getAsFloat(),
                                                        dataArr.get(3).getAsFloat());
                                                ImageVolume img = (ImageVolume)model.getCtImage();
                                                if (model != null && img != null && view != null) {
                                                    img.setAttribute("ImageOrientationQ", obj);
                                                    view.setIsDirty(true);
                                                } else {
                                                    System.out.println("Import CT Image First");
                                                }
                                            } else {
                                                System.out.println("data array must be of length 4");
                                            }
                                        } else {
                                            System.out.println("data must be a JSONArray");
                                        }
                                    }
                                }
                                case "translation" -> {
                                    if (jsonObject.has("data")) {
                                        JsonElement data = jsonObject.get("data");
                                        if (data.isJsonArray()) {
                                            JsonArray dataArr = data.getAsJsonArray();
                                            if (dataArr.size() == 3) {
                                                System.out.println(data);
                                                Vector3f translation = new Vector3f(dataArr.get(0).getAsFloat(),
                                                        dataArr.get(1).getAsFloat(),
                                                        dataArr.get(2).getAsFloat());
                                                ImageVolume img = (ImageVolume)model.getCtImage();
                                                if (model != null && img != null && view != null) {
                                                    img.setAttribute("ImageTranslation", translation);
                                                    view.setIsDirty(true);
                                                } else {
                                                    System.out.println("Import CT Image First");
                                                }
                                            } else {
                                                System.out.println("data array must be of length 3");
                                            }
                                        } else {
                                            System.out.println("data must be a JSONArray");
                                        }
                                    }
                                }
                                default -> System.out.print("Unknown attribute type");
                            }
                        }
                    }
                    default -> System.out.print("Unknown msg type");
                }
            }
        } catch (JsonParseException e) {
            System.out.print("Invalid or malformed JSON string");
        }
    }
    
    @Override
    public void run() {
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        try {
            serverSocket = new ServerSocket(portNum);
            serverSocket.setSoTimeout(2000);
            running.set(true);
            while (running.get()) {
                try {
                    clientSocket = serverSocket.accept();
                    processInput(new InputStreamReader(clientSocket.getInputStream()));
                } catch (SocketTimeoutException e) {
                    // Actually don't care if it gets here this is just for the thread interrupt
                }
            }
    	} catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                stopped.set(true);
            }
    	}
    }
}
