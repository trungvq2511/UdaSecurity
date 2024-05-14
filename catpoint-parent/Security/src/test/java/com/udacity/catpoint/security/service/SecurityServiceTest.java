package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.mockito.Mockito.*;

/**
 * Unit test for simple App.
 */
@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    private final Integer random = new Random().nextInt();
    private Sensor sensor;
    private SecurityService securityService;
    @Mock
    private StatusListener statusListener;
    @Mock
    private ImageService imageService;
    @Mock
    private SecurityRepository securityRepository;

    private Sensor getNewSensor() {
        return new Sensor("Sensor " + random, SensorType.DOOR);
    }

    private Set<Sensor> createManySensors() {
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(new Sensor("DOOR", SensorType.DOOR));
        sensors.add(new Sensor("WINDOW", SensorType.WINDOW));
        sensors.add(new Sensor("MOTION", SensorType.MOTION));
        return sensors;
    }

    @BeforeEach
    public void init() {
        securityService = new SecurityService(securityRepository, imageService);
        sensor = getNewSensor();
        Sensor sensor2 = new Sensor();
        securityService.addSensor(sensor2);
        securityService.removeSensor(sensor2);
    }

    @Test //Test 1.
    public void alarmArmed_sensorActivated_changeAlarmStatusPending() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test //Test 2.
    public void alarmArmed_sensorActivatedAndSystemPending_changeAlarmStatusAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test //Test 3
    public void alarmPending_allSensorInactive_changeAlarmStatusNoAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, true);
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }


    @ParameterizedTest //Test 4
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM", "ALARM"})
    public void alarmActive_changeSensorState_notAffectAlarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @ParameterizedTest //Test 5
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM", "ALARM"})
    public void sensorActivatedWhileActive_alarmPending_changeAlarmStatusAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(any(AlarmStatus.class));
    }

    // Test 6
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM", "ALARM"})
    void sensorDeactivated_sensorInactive_noChangesToAlarmState(AlarmStatus status) {
        when(securityRepository.getAlarmStatus()).thenReturn(status);

        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);

        Assertions.assertEquals(status, securityRepository.getAlarmStatus());
    }

    @Test //Test 7
    public void catIdentified_systemArmedHome_changeToAlarmStatus() {
        securityService.addStatusListener(statusListener);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        securityService.processImage(mock(BufferedImage.class));

        securityService.getAlarmStatus();
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
        securityService.removeStatusListener(statusListener);
    }

    @Test //Test 8
    public void catNotIdentified_sensorsInactive_changeAlarmStatusNoAlarm() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);

        sensor.setActive(false);
        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test //Test 9
    public void systemDisarmed_changeAlarmStatusNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest //Test 10
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "ARMED_HOME"})
    public void systemArmed_resetSensorsInactive(ArmingStatus status) {
        Set<Sensor> manySensors = createManySensors();
        when(securityRepository.getSensors()).thenReturn(manySensors);

        securityService.setArmingStatus(status);
        securityRepository.getSensors().forEach(sensor -> {
            Assertions.assertFalse(sensor.getActive());
        });
    }

    @Test //Test 11
    public void systemArmedHome_catIdentified_changeStatusToAlarm() {
        Set<Sensor> manySensors = createManySensors();

        when(securityRepository.getSensors()).thenReturn(manySensors);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));

        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    public void sensorActivated_armingStatusDisarm_notAffectAlarmState() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.changeSensorActivationStatus(sensor, true);
    }
}
