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
 * Internal class to handle physical card interface state.
 */
class CardInterface {
    /**
     * Class for holding interface configuration.
     */
    public static class InterfaceConfig {
        /**
         * 
         */
        public final String NAME;
        /**
         * 
         */
        public final byte PROTOCOL;
        /**
         * 
         */
        public final byte MAX_CHANNELS;
        
        public InterfaceConfig(String interfaceName, byte protocol, byte maximumChannels) {
            NAME = interfaceName;
            PROTOCOL = protocol;
            MAX_CHANNELS = maximumChannels;
        }
    }
    
    /**
     * 
     */
    public final String NAME;
    /**
     * 
     */
    public final byte PROTOCOL;
    /**
     * 
     */
    public final byte MAX_CHANNELS;
    
    /**
     * 
     */
    private CardChannel[] channels;

    /**
     * 
     * @param interfaceName
     * @param protocol
     * @param maximumChannels 
     */
    CardInterface(String interfaceName, byte protocol, byte maximumChannels) {
        NAME = interfaceName;
        PROTOCOL = protocol;
        if (maximumChannels < 1) {
            maximumChannels = 1;
        } else if (maximumChannels > SimulatorConfig.MAX_LOGICAL_CHANNELS) {
            maximumChannels = SimulatorConfig.MAX_LOGICAL_CHANNELS;
        }
        MAX_CHANNELS = maximumChannels;
        channels = new CardChannel[MAX_CHANNELS];
        for (int i = 0; i < MAX_CHANNELS; ++i) {
            channels[i] = new CardChannel();
        }
    }
    
    /**
     * 
     * @param applet
     * @param channelNumber 
     */
    void setDefaultApplet(AppletInstanceHolder applet, byte channelNumber) {
        if ((channelNumber >= 0) && (channelNumber < MAX_CHANNELS)) {
            channels[channelNumber].setDefaultApplet(applet);
        }
    }
    
    /**
     * 
     * @param channelNumber
     * @param sourceChannelNumber
     * @return 
     */
    AppletInstanceHolder getDefaultApplet(byte channelNumber, byte sourceChannelNumber) {
        if ((channelNumber >= 0) && (channelNumber < MAX_CHANNELS)) {
            if ((channelNumber == CardChannel.BASIC_CHANNEL) ||
                (sourceChannelNumber == CardChannel.BASIC_CHANNEL)) {
                return channels[channelNumber].getDefaultApplet();
            } else if ((sourceChannelNumber >= 0) && (sourceChannelNumber < MAX_CHANNELS)) {
                return channels[sourceChannelNumber].getCurrentSelectedApplet();
            }
        }
        
        return null;
    }

    /**
     * 
     * @param channelNumber
     * @return 
     */
    AppletInstanceHolder getCurrentSelectedApplet(byte channelNumber) {
        if ((channelNumber >= 0) && (channelNumber < MAX_CHANNELS)) {
            return channels[channelNumber].getCurrentSelectedApplet();
        }
        
        return null;
    }

    /**
     * 
     * @param channelNumber
     * @return 
     */
    AppletInstanceHolder getPreviousSelectedApplet(byte channelNumber) {
        if ((channelNumber >= 0) && (channelNumber < MAX_CHANNELS)) {
            return channels[channelNumber].getPreviousSelectedApplet();
        }
        
        return null;
    }
    
    /**
     * 
     * @param channelNumber 
     */
    byte open(byte channelNumber) {
        if ((channelNumber >= 0) && (channelNumber < MAX_CHANNELS)) {
            channels[channelNumber].open();
            return channelNumber;
        }
        
        return -1;
    }
    
    /**
     * 
     * @return channelNumber 
     */
    byte open() {
        for (byte channelNumber = 1; channelNumber < MAX_CHANNELS; ++channelNumber) {
            if (!channels[channelNumber].isOpen()) {
                channels[channelNumber].open();
                return channelNumber;
            }
        }
        
        return -1;
    }
    
    /**
     * 
     * @param applet
     * @param channelNumber 
     */
    void select(AppletInstanceHolder applet, byte channelNumber) {
        if ((channelNumber >= 0) && (channelNumber < MAX_CHANNELS)) {
            channels[channelNumber].select(applet);
        }
    }

    /**
     * 
     * @param channelNumber 
     */
    void close(byte channelNumber) {
        if ((channelNumber >= 0) && (channelNumber < MAX_CHANNELS)) {
            channels[channelNumber].close();
        }
    }

    /**
     * 
     * @param channelNumber
     * @return 
     */
    boolean isOpen(byte channelNumber) {
        if (channelNumber == 0) {
            return true;
        } else if ((channelNumber >= 0) && (channelNumber < MAX_CHANNELS)) {
            channels[channelNumber].isOpen();
        }
        
        return false;
    }
}
