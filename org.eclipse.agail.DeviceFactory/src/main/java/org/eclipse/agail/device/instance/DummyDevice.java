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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

public class DummyDevice extends DeviceImp implements Device {
  protected Logger logger = LoggerFactory.getLogger(DummyDevice.class);
  
  protected static final Map<String, byte[]> commands = new HashMap<String, byte[]>();

  public static final String deviceTypeName = "Dummy";

  /**
   * DUMMY Protocol imp DBus interface id
   */
  private static final String DUMMY_PROTOCOL_ID = "org.eclipse.agail.protocol.Dummy";
  /**
   * DUMMY Protocol imp DBus interface path
   */
  private static final String DUMMY_PROTOCOL_PATH = "/org/eclipse/agail/protocol/Dummy";

  private static final String DUMMY_COMPONENT = "DummyData";

  private DeviceStatusType deviceStatus = DeviceStatusType.DISCONNECTED;

  private static final byte[] DUM_CMD_1 = { 0x01 };
  private static final byte[] DUM_CMD_2 = { 0x02 };
  
  {
    profile.add(new DeviceComponent(DUMMY_COMPONENT, "dum"));
    subscribedComponents.put(DUMMY_COMPONENT, 0);
  }
  {
	  commands.put("Dummy Command 1", DUM_CMD_1);
	  commands.put("Dummy Command 2", DUM_CMD_2);
  }

  public DummyDevice(DeviceOverview deviceOverview) throws DBusException {
    super(deviceOverview);
    this.protocol = DUMMY_PROTOCOL_ID;
    String devicePath = AGILE_DEVICE_BASE_BUS_PATH + "dummy" + deviceOverview.id.replace(":", "");
    dbusConnect(deviceAgileID, devicePath, this);
    deviceProtocol = (Protocol) connection.getRemoteObject(DUMMY_PROTOCOL_ID, DUMMY_PROTOCOL_PATH, Protocol.class);
    logger.debug("Exposed device {} {}", deviceAgileID, devicePath);
  }

  public static boolean Matches(DeviceOverview d) {
    return d.name.contains(deviceTypeName);
  }

  @Override
  protected String DeviceRead(String componentName) {
    if ((protocol.equals(DUMMY_PROTOCOL_ID)) && (deviceProtocol != null)) {
      if (isConnected()) {
        if (isSensorSupported(componentName.trim())) {
          try {
            byte[] readData = deviceProtocol.Read(DUMMY_COMPONENT, new HashMap<String, String>());
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
    if ((protocol.equals(DUMMY_PROTOCOL_ID)) && (deviceProtocol != null)) {
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
    if ((protocol.equals(DUMMY_PROTOCOL_ID)) && (deviceProtocol != null)) {
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
  
  protected boolean isConnected() {
    if (Status().getStatus().equals(DeviceStatusType.CONNECTED.toString()) || Status().getStatus().equals(DeviceStatusType.ON.toString())) {
      return true;
    }
    return false;
  }
  
  @Override
  protected boolean isSensorSupported(String sensorName) {
    return DUMMY_COMPONENT.equals(sensorName);
  }
  
  @Override
  protected String formatReading(String sensorName, byte[] readData) {
	  String s = new String(readData);
	  return s;
  }
  
  @Override
  protected String getComponentName(Map<String, String> profile) {
    return DUMMY_COMPONENT;
  }
  
  @Override
  public void Write(String componentName, String payload) {
	  if ((protocol.equals(DUMMY_PROTOCOL_ID)) && (deviceProtocol != null)) {
		  if (isConnected()) {
				if (isSensorSupported(componentName.trim())) {
					try {
						logger.debug("Device Write: Time to step into the the moon's atmosphere without mask. {}, {}", payload, payload.getBytes());
						deviceProtocol.Write(address, getProfile(), payload.getBytes());
					} catch (Exception ex) {
						ex.printStackTrace();
						logger.error("Exception occured in Write: " + ex);
					}
				} else {
			        throw new AgileNoResultException("Componet not supported:" + componentName);
			    }
			} else {
				throw new AgileNoResultException("Dummy Device not connected: " + deviceName);
			}
		} else {
			throw new AgileNoResultException("Protocol not supported: " + protocol);
		}
	}

  @Override
  public void Execute(String command) {
	  if ((protocol.equals(DUMMY_PROTOCOL_ID)) && (deviceProtocol != null)) {
			if (isConnected()) {
				try {
					deviceProtocol.Write(address, getProfile(), commands.get(command));
				} catch (Exception ex) {
					logger.error("Exception occured in Execute: " + ex);
				}
			} else {
				throw new AgileNoResultException("Dummy Device not connected: " + deviceName);
			}
		} else {
			throw new AgileNoResultException("Protocol not supported: " + protocol);
		}
  }

  @Override
  public List<String> Commands(){
	  List<String> commandList = new ArrayList<>(commands.keySet());
	  return commandList;
  }
  
  	private Map<String, String> getProfile() {
		Map<String, String> profile = new HashMap<String, String>();
		profile.put(DUMMY_COMPONENT, "dum");
		return profile;
	}
}
