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
import org.fusfoundation.kranion.Button;
import org.fusfoundation.kranion.ListControl;
import org.fusfoundation.kranion.Renderable;

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
    private ListControl cmdList = null;
    private Button toggleBtn = null;
    
    public CLI(Model model, View view, int portNum) {
    	this.model = model;
        this.view = view;
        this.portNum = portNum;
        this.cmdList = (ListControl)Renderable.lookupByTag("lcCLICmdList");
        this.toggleBtn = (Button)Renderable.lookupByTag("toggleCLIStateBtn");
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
    
    
    public class ErrorProcessInputException extends Exception {
       private String msgData;

       public ErrorProcessInputException(String message, String msgData) {
          super(message);
          this.msgData = msgData;
       }

       public String msgData() {
          return msgData;
       }
    }
   
    private void printError(CLIError err){
        System.out.println(err);
        if (!err.isEmpty() && cmdList != null) {
            cmdList.addItem(err.title, err); // Overflow text behavior? Truncate string on click popup?
        }
    }
    
    private JsonObject processInput(InputStreamReader reader) {
        CLIError err = new CLIError();
        JsonObject jsonReturn = new JsonObject();
        JsonObject jsonObject = null;
        jsonReturn.addProperty("status", "Error");
        try {
            jsonObject = JsonParser.parseReader​(reader).getAsJsonObject();
//            Send a JSONArray with multiple update types that we loop over?
//           [
//             {
//               "msgType": "SetAttribute",
//               "attribute": "quaternion",
//               "data": [x,y,z,w]
//             },
//             {
//               "msgType": "SetAttribute",
//               "attribute": "translation",
//               "data": [x,y,z]
//             }, ...{}
//           ]
            if (jsonObject.has("msgType")) {
                JsonElement msgType = jsonObject.get("msgType");
                switch (msgType.getAsString()) {
                    case "SetAttribute":
                        err.title = "Registration";
                        if (jsonObject.has("attribute")) {
                            JsonElement attrib = jsonObject.get("attribute");
                            switch (attrib.getAsString()) {
                                case "quaternion":
                                    if (jsonObject.has("data")) {
                                        JsonElement data = jsonObject.get("data");
                                        if (data.isJsonArray()) {
                                            JsonArray dataArr = data.getAsJsonArray();
                                            if (dataArr.size() == 4) {
                                                Quaternion obj = new Quaternion(dataArr.get(0).getAsFloat(),
                                                        dataArr.get(1).getAsFloat(),
                                                        dataArr.get(2).getAsFloat(),
                                                        dataArr.get(3).getAsFloat());
                                                ImageVolume img = (ImageVolume)model.getCtImage();
                                                if (model != null && img != null && view != null) {
                                                    img.setAttribute("ImageOrientationQ", obj);
                                                    view.setIsDirty(true);
                                                    jsonReturn.addProperty("status", "Success");
                                                    throw(new Exception("Updated ImageOrientation"));
                                                } else {
                                                    throw(new Exception("Import CT Image First"));
                                                }
                                            } else {
                                                throw(new Exception("Data array must be of length 4"));
                                            }
                                        } else {
                                            throw(new Exception("Data must be a JSONArray"));
                                        }
                                    } else {
                                        throw(new Exception("Missing data key"));
                                    }
                                case "translation":
                                    if (jsonObject.has("data")) {
                                        JsonElement data = jsonObject.get("data");
                                        if (data.isJsonArray()) {
                                            JsonArray dataArr = data.getAsJsonArray();
                                            if (dataArr.size() == 3) {
                                                Vector3f translation = new Vector3f(dataArr.get(0).getAsFloat(),
                                                        dataArr.get(1).getAsFloat(),
                                                        dataArr.get(2).getAsFloat());
                                                ImageVolume img = (ImageVolume)model.getCtImage();
                                                if (model != null && img != null && view != null) {
                                                    img.setAttribute("ImageTranslation", translation);
                                                    view.setIsDirty(true);
                                                    jsonReturn.addProperty("status", "Success");
                                                    throw(new Exception("Updated ImageTranslation"));
                                                } else {
                                                    throw(new Exception("Import CT Image First"));
                                                }
                                            } else {
                                                throw(new Exception("Data array must be of length 4"));
                                            }
                                        } else {
                                            throw(new Exception("Data must be a JSONArray"));
                                        }
                                    } else {
                                        throw(new Exception("missing data key"));
                                    }
                                default:
                                    throw(new Exception("Unknown attribute value"));
                            }
                        } else {
                            throw(new Exception("Missing attribute key"));
                        }
                    default:
                        throw(new Exception("Unknown msgType value"));
                }
            } else {
                throw(new Exception("Missing msgType key"));
            }
        } catch (JsonParseException e) {
            err.title = "Error: JSONParse Invalid or malformed JSON string";
            err.body = e.getMessage();
        } catch (Exception e) {
            err.title = jsonReturn.get("status").getAsString() + ": " + e.getMessage();
            err.body = jsonObject != null ? jsonObject.toString() : "Bad message! Send a stringified JSON!";
        } finally {
            jsonReturn.addProperty("msg", err.title);
//            jsonReturn.addProperty("msgBody", err.body); Don't send back body becuase that's what they sent?
            printError(err); // color error types? red Error, yellow warning, green / inherit success
        }
        return jsonReturn;
    }
    
    @Override
    public void run() {
        running.set(true);
        toggleBtn.setIndicator(true);
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        try {
            serverSocket = new ServerSocket(portNum);
            serverSocket.setSoTimeout(2000);
            while (running.get()) {
                try {
                    clientSocket = serverSocket.accept();
                    JsonObject returnMsg = processInput(new InputStreamReader(clientSocket.getInputStream()));
                    ObjectOutputStream os = new ObjectOutputStream(clientSocket.getOutputStream());
                    os.writeObject(returnMsg.toString());
                } catch (SocketTimeoutException | SocketException e) {
                    // Actually don't care if it gets here this is just for the thread interrupt
                }
//                catch (SocketException e) {
//                    // Catch broken pipe write fails?
//                    // check if it is "Broken pipe"? 
//                }
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
                toggleBtn.setIndicator(false);
                running.set(false);
                stopped.set(true);
            }
    	}
    }
}
