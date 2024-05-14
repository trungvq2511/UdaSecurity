package com.udacity.catpoint.security.data;

import java.util.Set;

/**
 * Interface showing the methods our security repository will need to support
 */
public interface SecurityRepository {
    void addSensor(Sensor sensor);

    void removeSensor(Sensor sensor);

    void updateSensor(Sensor sensor);

    Set<Sensor> getSensors();

    AlarmStatus getAlarmStatus();

    void setAlarmStatus(AlarmStatus alarmStatus);

    ArmingStatus getArmingStatus();

    void setArmingStatus(ArmingStatus armingStatus);


}
