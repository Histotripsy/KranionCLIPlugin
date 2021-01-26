/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fusfoundation.kranion.plugins;

/**
 *
 * @author mkomaiha
 */
public class CLIError {
    String body;
    String title;
    public CLIError() {
        body = "";
        title = "";
    }
    public CLIError(String title, String body) {
        this.title = title;
        this.body = body;
    }
    public boolean isEmpty() {
        return title.length() == 0 && body.length() == 0;
    }
//        public void addLine(String text) {
//            body += "\n" + text;
//        }
//        public void setTitleBody(String text) {
//            title = text;
//            body = text;
//        }
}
