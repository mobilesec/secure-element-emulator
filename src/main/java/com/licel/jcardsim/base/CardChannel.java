/*
 * Copyright 2013 FH OOe Forschungs & Entwicklungs GmbH, Michael Roland.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.licel.jcardsim.base;

/**
 * Internal class for handling state of logical channels.
 */
class CardChannel {

    /**
     * 
     */
    public final static byte BASIC_CHANNEL = 0;
    
    /**
     * 
     */
    private boolean channelIsOpen;
    /**
     * 
     */
    private AppletInstanceHolder defaultSelectedApplet;
    /**
     * 
     */
    private AppletInstanceHolder currentSelectedApplet;
    /**
     * 
     */
    private AppletInstanceHolder previousSelectedApplet;
    
    /**
     * 
     */
    CardChannel() {
        channelIsOpen = false;
    }
    
    /**
     * 
     * @param applet 
     */
    void setDefaultApplet(AppletInstanceHolder applet) {
        defaultSelectedApplet = applet;
    }

    /**
     * 
     * @return 
     */
    AppletInstanceHolder getDefaultApplet() {
        return defaultSelectedApplet;
    }
    
    /**
     * 
     * @return 
     */
    AppletInstanceHolder getCurrentSelectedApplet() {
        return currentSelectedApplet;
    }
    
    /**
     * 
     * @return 
     */
    AppletInstanceHolder getPreviousSelectedApplet() {
        return previousSelectedApplet;
    }
    
    /**
     * 
     * @return 
     */
    boolean isOpen() {
        return channelIsOpen;
    }
    
    /**
     * 
     */
    void open() {
        channelIsOpen = true;
        previousSelectedApplet = null;
        currentSelectedApplet = null;
    }
    
    /**
     * 
     * @param applet 
     */
    void select(AppletInstanceHolder applet) {
        previousSelectedApplet = currentSelectedApplet;
        currentSelectedApplet = applet;
    }
    
    /**
     * 
     */
    void close() {
        channelIsOpen = false;
        previousSelectedApplet = null;
        currentSelectedApplet = null;
    }
}
