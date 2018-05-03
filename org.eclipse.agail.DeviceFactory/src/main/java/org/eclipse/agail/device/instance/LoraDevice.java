/*******************************************************************************
 * Copyright (C) 2017 Create-Net / FBK.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Create-Net / FBK - initial API and implementation
 ******************************************************************************/
package org.eclipse.agail.device.instance;

import java.util.HashMap;
import java.util.Map;

import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.agail.Device;
import org.eclipse.agail.Protocol;
import org.eclipse.agail.device.base.DeviceImp;
import org.eclipse.agail.exception.AgileNoResultException;
import org.eclipse.agail.object.DeviceComponent;
import org.eclipse.agail.object.DeviceOverview;
import org.eclipse.agail.object.DeviceStatusType;
import org.eclipse.agail.object.StatusType;
import java.util.List;

public class LoraDevice extends DeviceImp implements Device {
  protected Logger logger = LoggerFactory.getLogger(LoraDevice.class);

  public static final String deviceTypeName = "LoRa";

  /**
   * LoRa Protocol imp DBus interface id
   */
  private static final String LORA_PROTOCOL_ID = "org.eclipse.agail.protocol.LoRa";
  /**
   * LoRa Protocol imp DBus interface path
   */
  private static final String LORA_PROTOCOL_PATH = "/org/eclipse/agail/protocol/LoRa";

  private static final String LORA_COMPONENT = "LoraData";

  private DeviceStatusType deviceStatus = DeviceStatusType.DISCONNECTED;

  {
    profile.add(new DeviceComponent(LORA_COMPONENT, "lora"));
    subscribedComponents.put(LORA_COMPONENT, 0);
  }

  public LoraDevice(DeviceOverview deviceOverview) throws DBusException {
    super(deviceOverview);
    this.protocol = LORA_PROTOCOL_ID;
    String devicePath = AGILE_DEVICE_BASE_BUS_PATH + "lora" + deviceOverview.id; 
    dbusConnect(deviceAgileID, devicePath, this);
    deviceProtocol = (Protocol) connection.getRemoteObject(LORA_PROTOCOL_ID, LORA_PROTOCOL_PATH, Protocol.class);
    logger.debug("Exposed device {} {}", deviceAgileID, devicePath);
  }

  public static boolean Matches(DeviceOverview d) {
    return d.name.contains(deviceTypeName);
  }

  @Override
  protected String DeviceRead(String componentName) {
    if ((protocol.equals(LORA_PROTOCOL_ID)) && (deviceProtocol != null)) {
      if (isConnected()) {
        if (isSensorSupported(componentName.trim())) {
          try {
            byte[] readData = deviceProtocol.Read(LORA_COMPONENT, new HashMap<String, String>());
            return formatReading(componentName, readData);
          } catch (DBusException e) {
            e.printStackTrace();
          }
        } else {
          throw new AgileNoResultException("Componet not supported:" + componentName);
        }
      } else {
        throw new AgileNoResultException("Device not connected: " + deviceName);
      }
    } else {
      throw new AgileNoResultException("Protocol not supported: " + protocol);
    }
    throw new AgileNoResultException("Unable to read " + componentName);
  }

  @Override
  public void Subscribe(String componentName) {
    if ((protocol.equals(LORA_PROTOCOL_ID)) && (deviceProtocol != null)) {
      if (isConnected()) {
        if (isSensorSupported(componentName.trim())) {
          try {
            if (!hasOtherActiveSubscription()) {
              addNewRecordSignalHandler();
            }
            if (!hasOtherActiveSubscription(componentName)) {
              deviceProtocol.Subscribe(address, new HashMap<String, String>());
            }
            subscribedComponents.put(componentName, subscribedComponents.get(componentName) + 1);
          } catch (Exception e) {
            e.printStackTrace();
          }
        } else {
          throw new AgileNoResultException("Component not supported:" + componentName);
        }
      } else {
        throw new AgileNoResultException("Device not connected: " + deviceName);
      }
    } else {
      throw new AgileNoResultException("Protocol not supported: " + protocol);
    }
    }

  @Override
  public synchronized void Unsubscribe(String componentName) throws DBusException {
    if ((protocol.equals(LORA_PROTOCOL_ID)) && (deviceProtocol != null)) {
      if (isConnected()) {
        if (isSensorSupported(componentName.trim())) {
          try {
            subscribedComponents.put(componentName, subscribedComponents.get(componentName) - 1);
            if (!hasOtherActiveSubscription(componentName)) {
              deviceProtocol.Unsubscribe(address, new HashMap<String, String>());
             }
            if (!hasOtherActiveSubscription()) {
              removeNewRecordSignalHandler();
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        } else {
          throw new AgileNoResultException("Component not supported:" + componentName);
        }
      } else {
        throw new AgileNoResultException("Device not connected: " + deviceName);
      }
    } else {
      throw new AgileNoResultException("Protocol not supported: " + protocol);
    }
   }

  @Override
  public void Connect() throws DBusException {
    deviceStatus = DeviceStatusType.CONNECTED;
    logger.info("Device connected {}", deviceID);
  }

  @Override
  public void Disconnect() throws DBusException {
    deviceStatus = DeviceStatusType.DISCONNECTED;
    logger.info("Device disconnected {}", deviceID);
  }

  @Override
  public StatusType Status() {
    return new StatusType(deviceStatus.toString());
  }
  
//  @Override
//  public void Execute(String command, Map<String, Variant> args) {
//    if(command.equalsIgnoreCase(DeviceStatusType.ON.toString())){
//      deviceStatus = DeviceStatusType.ON;
//    }else if(command.equalsIgnoreCase(DeviceStatusType.OFF.toString())){
//      deviceStatus = DeviceStatusType.OFF;
//    }
//  }
//  
  protected boolean isConnected() {
    if (Status().getStatus().equals(DeviceStatusType.CONNECTED.toString()) || Status().getStatus().equals(DeviceStatusType.ON.toString())) {
      return true;
    }
    return false;
  }
  
  @Override
  protected boolean isSensorSupported(String sensorName) {
    return LORA_COMPONENT.equals(sensorName);
  }
  
  @Override
  protected String formatReading(String sensorName, byte[] readData) {
     int result = (readData[0] & 0xFF); 
     return String.valueOf(result);
  }
  
  @Override
  protected String getComponentName(Map<String, String> profile) {
    return LORA_COMPONENT;
  }
  
  @Override
  public void Write(String componentName, String payload) {	
    logger.debug("Write function for LoRA not implemented yet");
	}

  @Override
  public void Execute(String command) {
    logger.debug("Device. Execute not implemented");
	}

  @Override
  public List<String> Commands(){
    logger.debug("Device. Commands not implemented");
    return null;
      }
}