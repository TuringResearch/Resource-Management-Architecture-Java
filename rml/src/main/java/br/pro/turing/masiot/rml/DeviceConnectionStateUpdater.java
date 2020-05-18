package br.pro.turing.masiot.rml;

import br.pro.turing.masiot.core.model.ConnectionState;
import br.pro.turing.masiot.core.model.Device;
import br.pro.turing.masiot.core.model.Resource;
import br.pro.turing.masiot.core.service.ServiceManager;
import br.pro.turing.masiot.core.utils.LoggerUtils;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class DeviceConnectionStateUpdater implements Runnable {

    /** Logger. */
    private static final Logger LOGGER = LoggerUtils.initLogger(DeviceConnectionStateUpdater.class.getClassLoader()
                    .getResourceAsStream("br/pro/turing/masiot/rml/rml.logging.properties"),
            DeviceConnectionStateUpdater.class.getSimpleName());

    private Map<String, LocalDateTime> deviceCurrentUpdateMap;

    private Map<String, Device> deviceMap;

    public DeviceConnectionStateUpdater() {
        this.deviceCurrentUpdateMap = new HashMap<>();
        this.deviceMap = new HashMap<>();
    }

    public void init() {
        final List<Device> devices = ServiceManager.getInstance().deviceService.findAll();
        for (Device device : devices) {
            this.deviceMap.put(device.getDeviceName(), device);
            this.deviceCurrentUpdateMap.put(device.getDeviceName(), null);
            device.setConnectionState(ConnectionState.OFFLINE.getState());
        }
        ServiceManager.getInstance().deviceService.saveAll(devices);
    }

    @Override
    public void run() {
        while (true) {
            final long t1 = System.currentTimeMillis();

            List<Device> devicesChanged = new ArrayList<>();
            this.deviceCurrentUpdateMap.forEach((deviceName, localDateTime) -> {
                if (localDateTime != null) {
                    Device device = this.deviceMap.get(deviceName);
                    if (localDateTime.until(LocalDateTime.now(), ChronoUnit.MILLIS) > device.getCycleDelayInMillis()
                            && device.getConnectionState().equals(ConnectionState.ONLINE.getState())) {
                        device.setConnectionState(ConnectionState.OFFLINE.getState());
                        devicesChanged.add(device);
                    } else if (localDateTime.until(LocalDateTime.now(), ChronoUnit.MILLIS) <= device
                            .getCycleDelayInMillis() && device.getConnectionState().equals(
                            ConnectionState.OFFLINE.getState())) {
                        device.setConnectionState(ConnectionState.ONLINE.getState());
                        devicesChanged.add(device);
                    }
                }
            });
            if (!devicesChanged.isEmpty()) {
                ServiceManager.getInstance().deviceService.saveAll(devicesChanged);
            }

            final long deltaTime = System.currentTimeMillis() - t1;
            try {
                Thread.sleep(deltaTime < 5000 ? 5000 - deltaTime : 0);
            } catch (InterruptedException e) {
                LOGGER.severe(e.getMessage());
            }
        }
    }

    public void pingDevice(Device device) {
        if (!this.deviceMap.containsKey(device.getDeviceName())) {
            this.deviceMap.put(device.getDeviceName(), device);
            this.deviceCurrentUpdateMap.put(device.getDeviceName(), LocalDateTime.now());
        } else {
            this.deviceCurrentUpdateMap.replace(device.getDeviceName(), LocalDateTime.now());
        }
    }

    public void pingDeviceByDeviceName(String deviceName) {
        final Device device = this.deviceMap.get(deviceName);
        if (device != null) {
            pingDevice(device);
        }
    }
}