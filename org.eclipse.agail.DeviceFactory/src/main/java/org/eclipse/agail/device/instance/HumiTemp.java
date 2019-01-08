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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.agail.Device;
import org.eclipse.agail.device.base.AgileBLEDevice;
import org.eclipse.agail.device.base.SensorUuid;
import org.eclipse.agail.exception.AgileNoResultException;
import org.eclipse.agail.object.DeviceComponent;
import org.eclipse.agail.object.DeviceDefinition;
import org.eclipse.agail.object.DeviceOverview;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HumiTemp extends AgileBLEDevice implements Device {
	protected Logger logger = LoggerFactory.getLogger(HumiTemp.class);
	protected static final Map<String, SensorUuid> sensors = new HashMap<String, SensorUuid>();
	private static final String TEMPERATURE = "Temperature";
	private static final String HUMIDITY = "Humidity";

	{
		subscribedComponents.put(TEMPERATURE, 0);
		subscribedComponents.put(HUMIDITY, 0);
	}

	{
		profile.add(new DeviceComponent(TEMPERATURE, "Degree celsius (°C)"));
		profile.add(new DeviceComponent(HUMIDITY, "Relative humidity (%RH)"));
	}
	
	static {
		sensors.put(TEMPERATURE, new SensorUuid("0000aa20-0000-1000-8000-00805f9b34fb", "0000aa21-0000-1000-8000-00805f9b34fb", "", ""));
		sensors.put(HUMIDITY, new SensorUuid("0000aa20-0000-1000-8000-00805f9b34fb", "0000aa21-0000-1000-8000-00805f9b34fb", "", ""));
	}

	public static boolean Matches(DeviceOverview d) {
		return d.name.contains("HumiTemp Sensor Tag");
	}

	public static String deviceTypeName = "HumiTemp Sensor Tag";

	public HumiTemp(DeviceOverview deviceOverview) throws DBusException {
		super(deviceOverview);
	}

	/**
	 * 
	 * @param devicedefinition
	 * @throws DBusException
	 */
	public HumiTemp(DeviceDefinition devicedefinition) throws DBusException {
		super(devicedefinition);
	}

	@Override
	public void Connect() throws DBusException {
		super.Connect();
		for (String componentName : subscribedComponents.keySet()) {
			if (subscribedComponents.get(componentName) > 0) {
				logger.info("Resubscribing to {}", componentName);
				deviceProtocol.Subscribe(address, getReadValueProfile(componentName));
			}
		}
	}
	
	@Override
	public String DeviceRead(String componentName) {
		logger.info("HumiTemp DeviceRead: "+ componentName);
		if ((protocol.equals(BLUETOOTH_LOW_ENERGY)) && (deviceProtocol != null)) {
			if (isConnected()) {
				if (isSensorSupported(componentName.trim())) {
					try {
						byte[] result = deviceProtocol.Read(address, getReadValueProfile(componentName));
						return formatReading(componentName, result);
					} catch (DBusException e) {
						e.printStackTrace();
					}
				} else {
					throw new AgileNoResultException("Sensor not supported:" + componentName);
				}
			} else {
				throw new AgileNoResultException("BLE Device not connected: " + deviceName);
			}
		} else {
			throw new AgileNoResultException("Protocol not supported: " + protocol);
		}
		throw new AgileNoResultException("Unable to read "+componentName);
	}

	/*
	 * TODO - enable for humitemp as this is how it seems to work best generally
	public String NotificationRead(String componentName) {
		if ((protocol.equals(BLUETOOTH_LOW_ENERGY)) && (deviceProtocol != null)) {
			if (isConnected()) {
				if (isSensorSupported(componentName.trim())) {
					try {
						deviceProtocol.Write(address, getEnableSensorProfile(componentName), TURN_ON_SENSOR);
						byte[] period = { 100 };
						deviceProtocol.Write(address, getFrequencyProfile(componentName), period);
						byte[] result = deviceProtocol.NotificationRead(address, getReadValueProfile(componentName));
						return formatReading(componentName, result);
					} catch (DBusException e) {
						e.printStackTrace();
					}
				} else {
					throw new AgileNoResultException("Sensor not supported:" + componentName);
				}
			} else {
				throw new AgileNoResultException("BLE Device not connected: " + deviceName);
			}
		} else {
			throw new AgileNoResultException("Protocol not supported: " + protocol);
		}
		throw new AgileNoResultException("Unable to read " + componentName);
	}
	*/

	@Override
	public synchronized void Subscribe(String componentName) {
		if ((protocol.equals(BLUETOOTH_LOW_ENERGY)) && (deviceProtocol != null)) {
			if (isConnected()) {
				if (isSensorSupported(componentName.trim())) {
					try {
						if (!hasOtherActiveSubscription(componentName)) {
							deviceProtocol.Subscribe(address, getReadValueProfile(componentName));
							addNewRecordSignalHandler();
						}
						logger.info("HumiTemp Subscribe");
						subscribedComponents.put(componentName, subscribedComponents.get(componentName) + 1);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					throw new AgileNoResultException("Sensor not supported:" + componentName);
				}
			} else {
				throw new AgileNoResultException("BLE Device not connected: " + deviceName);
			}
		} else {
			throw new AgileNoResultException("Protocol not supported: " + protocol);
		}
	}

	@Override
	public synchronized void Unsubscribe(String componentName) throws DBusException {
		if ((protocol.equals(BLUETOOTH_LOW_ENERGY)) && (deviceProtocol != null)) {
			if (isConnected()) {
				if (isSensorSupported(componentName.trim())) {
					try {
						subscribedComponents.put(componentName, subscribedComponents.get(componentName) - 1);
						if (!hasOtherActiveSubscription(componentName)) {
							deviceProtocol.Unsubscribe(address, getReadValueProfile(componentName));
							removeNewRecordSignalHandler();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					throw new AgileNoResultException("Sensor not supported:" + componentName);
				}
			} else {
				throw new AgileNoResultException("BLE Device not connected: " + deviceName);
			}
		} else {
			throw new AgileNoResultException("Protocol not supported: " + protocol);
		}
	}

	@Override
	public void Write(String componentName, String payload) {
		logger.debug("Device. Write not implemented");
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

	// =======================Utility methods===========================
	@Override
	protected boolean isSensorSupported(String sensorName) {
		return sensors.containsKey(sensorName);
	}

	private Map<String, String> getReadValueProfile(String sensorName) {
		Map<String, String> profile = new HashMap<String, String>();
		SensorUuid s = sensors.get(sensorName);
		if (s != null) {
			profile.put(GATT_SERVICE, s.serviceUuid);
			profile.put(GATT_CHARACTERSTICS, s.charValueUuid);
			logger.info("HumiTemp Gatt Service: "+s.serviceUuid);
			logger.info("HumiTemp Gatt Characteristic: "+s.charValueUuid);
		}
		return profile;
	}

	@Override
	protected String formatReading(String componentName, byte[] readData) {
		String value = "";
		switch (componentName) {
			case TEMPERATURE:
				value = Double.toString(Math.pow(-1, readData[0] + 2) * (readData[1]  + readData[2] / 10.0));
				return value;
			case HUMIDITY:	
				value = Double.toString(readData[4] + readData[5] / 10.0);
				return value;
		}
		return "0";
	}

	/**
	 * Given the profile of the component returns the name of the sensor
	 * 
	 * @param uuid
	 * @return
	 */
	@Override
	protected String getComponentName(Map<String, String> profile) {
		String serviceUUID = profile.get(GATT_SERVICE);
		String charValueUuid = profile.get(GATT_CHARACTERSTICS);
		for (Entry<String, SensorUuid> su : sensors.entrySet()) {
			if (su.getValue().serviceUuid.equals(serviceUUID) && su.getValue().charValueUuid.equals(charValueUuid)) {
				return su.getKey();
			}
		}
		return null;
	}

	private byte[] getBytes(String payload) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		output.write(Byte.valueOf(payload));

		byte[] bytes = output.toByteArray();

		return bytes;
	}
}
