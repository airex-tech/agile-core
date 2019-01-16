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
	protected static final Map<String, byte[]> commands = new HashMap<String, byte[]>();
	private static final byte[] TURN_ON_SENSOR = { 0X01 };
	private static final byte[] TURN_OFF_SENSOR = { 0X00 };
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
		sensors.put(TEMPERATURE,
				new SensorUuid("0000aa20-0000-1000-8000-00805f9b34fb", "0000aa21-0000-1000-8000-00805f9b34fb",
						"0000aa22-0000-1000-8000-00805f9b34fb", "0000aa23-0000-1000-8000-00805f9b34fb"));
		sensors.put(HUMIDITY,
				new SensorUuid("0000aa20-0000-1000-8000-00805f9b34fb", "0000aa21-0000-1000-8000-00805f9b34fb",
						"0000aa22-0000-1000-8000-00805f9b34fb", "0000aa23-0000-1000-8000-00805f9b34fb"));
	}

	static {
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
				deviceProtocol.Write(address, getEnableSensorProfile(componentName), TURN_ON_SENSOR);
				deviceProtocol.Subscribe(address, getReadValueProfile(componentName));
			}
		}
	}

	@Override
	public String DeviceRead(String sensorName) {
		if ((protocol.equals(BLUETOOTH_LOW_ENERGY)) && (deviceProtocol != null)) {
			if (isConnected()) {
				if (isSensorSupported(sensorName.trim())) {
					try {
						if (!hasOtherActiveSubscription(sensorName)) {
							// turn on sensor
							deviceProtocol.Write(address, getEnableSensorProfile(sensorName), TURN_ON_SENSOR);
						}
						/**
						 * The default read data period (frequency) of most of sensor tag sensors is
						 * 1000ms therefore the first data will be available to read after 1000ms for
						 * these we call Read method after 1 second
						 */
						Thread.sleep(1010);
						// read value
						byte[] readValue = deviceProtocol.Read(address, getReadValueProfile(sensorName));
						if (!hasOtherActiveSubscription(sensorName)) {
							deviceProtocol.Write(address, getTurnOffSensorProfile(sensorName), TURN_OFF_SENSOR);
						}
						return formatReading(sensorName, readValue);
					} catch (Exception e) {
						logger.debug("Error in reading value from Sensor {}", e);
						e.printStackTrace();
					}
				} else {
					logger.debug("Sensor not supported: {}", sensorName);
					return null;
				}
			} else {
				logger.debug("BLE Device not connected: {}", deviceName);
				return null;
			}
		} else {
			logger.debug("Protocol not supported:: {}", protocol);
			return null;
		}
		return null;
	}

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

	@Override
	public synchronized void Subscribe(String componentName) {
		logger.info("Subscribe to {}", componentName);
		if ((protocol.equals(BLUETOOTH_LOW_ENERGY)) && (deviceProtocol != null)) {
			if (isConnected()) {
				if (isSensorSupported(componentName.trim())) {
					try {
						if (!hasOtherActiveSubscription()) {
							addNewRecordSignalHandler();
						}
						if (!hasOtherActiveSubscription(componentName)) {
							deviceProtocol.Write(address, getEnableSensorProfile(componentName), TURN_ON_SENSOR);
							/*
							 * Setting the period on the Pressure sensor was not working. Since we are
							 * anyway using the default value, keep this disabled. TODO: verify pressure
							 * senosr. byte[] period = { 100 }; deviceProtocol.Write(address,
							 * getFrequencyProfile(componentName), period);
							 */
							deviceProtocol.Subscribe(address, getReadValueProfile(componentName));
						}
						subscribedComponents.put(componentName, subscribedComponents.get(componentName) + 1);
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
	}

	@Override
	public synchronized void Unsubscribe(String componentName) throws DBusException {
		logger.info("Unsubscribe from {}", componentName);
		if ((protocol.equals(BLUETOOTH_LOW_ENERGY)) && (deviceProtocol != null)) {
			if (isConnected()) {
				if (isSensorSupported(componentName.trim())) {
					try {
						subscribedComponents.put(componentName, subscribedComponents.get(componentName) - 1);
						if (!hasOtherActiveSubscription(componentName)) {
							// disable notification
							deviceProtocol.Unsubscribe(address, getReadValueProfile(componentName));
							// turn off sensor
							deviceProtocol.Write(address, getTurnOffSensorProfile(componentName), TURN_OFF_SENSOR);
						}
						if (!hasOtherActiveSubscription()) {
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
		if ((protocol.equals(BLUETOOTH_LOW_ENERGY)) && (deviceProtocol != null)) {
			if (isConnected()) {
				try {
					if(payload.equals("0")) {
						deviceProtocol.Write(address, getEnableSensorProfile(componentName), TURN_OFF_SENSOR);
					} else {
						deviceProtocol.Write(address, getEnableSensorProfile(componentName), TURN_ON_SENSOR);
					}
				} catch (Exception ex) {
					logger.error("Exception occured in Write: " + ex);
				}
			} else {
				throw new AgileNoResultException("BLE Device not connected: " + deviceName);
			}
		} else {
			throw new AgileNoResultException("Protocol not supported: " + protocol);
		}
	}

	@Override
	public List<String> Commands() {
		List<String> commandList = new ArrayList<>(commands.keySet());
		return commandList;
	}

	// =======================Utility methods===========================
	@Override
	protected boolean isSensorSupported(String sensorName) {
		return sensors.containsKey(sensorName);
	}

	private Map<String, String> getEnableSensorProfile(String sensorName) {
		Map<String, String> profile = new HashMap<String, String>();
		SensorUuid s = sensors.get(sensorName);
		if (s != null) {
			profile.put(GATT_SERVICE, s.serviceUuid);
			profile.put(GATT_CHARACTERSTICS, s.charConfigUuid);
		}
		return profile;
	}

	private Map<String, String> getReadValueProfile(String sensorName) {
		Map<String, String> profile = new HashMap<String, String>();
		SensorUuid s = sensors.get(sensorName);
		if (s != null) {
			profile.put(GATT_SERVICE, s.serviceUuid);
			profile.put(GATT_CHARACTERSTICS, s.charValueUuid);
		}
		return profile;
	}

	private Map<String, String> getTurnOffSensorProfile(String sensorName) {
		Map<String, String> profile = new HashMap<String, String>();
		SensorUuid s = sensors.get(sensorName);
		if (s != null) {
			profile.put(GATT_SERVICE, s.serviceUuid);
			profile.put(GATT_CHARACTERSTICS, s.charConfigUuid);
		}
		return profile;
	}

	private Map<String, String> getFrequencyProfile(String sensorName) {
		Map<String, String> profile = new HashMap<String, String>();
		SensorUuid s = sensors.get(sensorName);
		if (s != null) {
			profile.put(GATT_SERVICE, s.serviceUuid);
			profile.put(GATT_CHARACTERSTICS, s.charFreqUuid);
		}
		return profile;
	}

	/**
	 *
	 * The sensor service returns the data in an encoded format which can be found
	 * in the wiki(http://processors.wiki.ti.com/index.php/SensorTag_User_Guide#
	 * IR_Temperature_Sensor). Convert the raw sensor reading value format to human
	 * understandable value and print it.
	 * 
	 * @param sensorName
	 *            Name of the sensor to read value from
	 * @param readingValue
	 *            the raw value read from the sensor
	 * @return
	 */
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
	 * Gyroscope, Magnetometer, Barometer, IR temperature all store 16 bit two's
	 * complement values in the format LSB MSB.
	 *
	 * This function extracts these 16 bit two's complement values.
	 */
	private static Integer shortSignedAtOffset(byte[] value, int offset) {
		Integer lowerByte = Byte.toUnsignedInt(value[offset]);
		Integer upperByte = Byte.toUnsignedInt(value[offset + 1]); // Note:
		return (upperByte << 8) + lowerByte;
	}

	/**
	 * Converts temperature into degree Celsius
	 *
	 * @param raw
	 * @return
	 */
	private float convertCelsius(int raw) {
		return raw / 128f;
	}

	/**
	 * Formats humidity value according to SensorTag WIKI
	 * 
	 * @param raw
	 * @return
	 */
	private float convertHumidity(int raw) {
		return (((float) raw) / 65536) * 100;
	}

	private float convertPressure(int raw) {
		return raw / 100.0f;
	}

	private float convertOpticalRead(int raw) {
		int e = (raw & 0x0F000) >> 12; // Interim value in calculation
		int m = raw & 0x0FFF; // Interim value in calculation

		return (float) (m * (0.01 * Math.pow(2.0, e)));
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
