package com.udacity.catpoint.mainapp.service;

import com.udacity.catpoint.imageservice.service.IImageService;
import com.udacity.catpoint.mainapp.data.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    @Mock
    SecurityRepository securityRepository;

    @Mock
    IImageService imageService;

    @InjectMocks
    private SecurityService securityService;

    private Set<Sensor> createSensors(boolean active, int quantity){
        Set<Sensor> sensors = new TreeSet<>();

        for (int i = 0; i < quantity; i++){
            Sensor sensor = new Sensor("Sensor-" + i, SensorType.values()[i%3]);
            sensor.setActive(active);

            sensors.add(sensor);
        }

        return sensors;
    }

    @ParameterizedTest // 1
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void sensorActivated_alarmArmed_alarmStatusPending(ArmingStatus armingStatus){
        Sensor sensor = new Sensor("Test 1", SensorType.WINDOW);

        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);

        verify(securityRepository, times(1)).setAlarmStatus(captor.capture());
        verify(securityRepository, times(1)).updateSensor(sensor);
        assertEquals(captor.getValue(), AlarmStatus.PENDING_ALARM);
    }

    @Test // 2
    void sensorActivated_AlarmAlreadyPending_alarmStatusAlarm(){
        Sensor sensor = new Sensor("Test 2", SensorType.DOOR);

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test // 3
    void allSensorsInactive_alarmPending_alarmStatusNoAlarm(){
        Set<Sensor> sensors = createSensors(false, 3);
        Sensor one = sensors.iterator().next();
        one.setActive(true);

        when(securityRepository.getSensors()).thenReturn(sensors);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(one, false);

        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, times(1)).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest // 4
    @ValueSource(booleans = {true, false})
    void sensorChange_alarmActive_notAffectedAlarmStatus(boolean active) {
        Sensor sensor = new Sensor("Test 4", SensorType.MOTION);
        sensor.setActive(active);

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        securityService.changeSensorActivationStatus(sensor, !active);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test // 5
    void sensorActivatedWhileAlreadyActive_alarmPending_alarmStatusAlarm(){
        Sensor sensor = new Sensor("Test 5", SensorType.WINDOW);

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, times(1)).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.ALARM);
    }

    @Test // 6
    void sensorDeactivateWhileAlreadyInactive_noChangeAlarmStatus(){
        Sensor sensor = new Sensor("Test 6", SensorType.DOOR);

        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any());
    }

    @Test // 7
    void imageContainCat_systemArmedHome_alarmStatusAlarm(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);

        securityService.processImage(mock(BufferedImage.class));

        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, times(1)).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.ALARM);
    }

    @Test // 8
    void imageContainNoCat_sensorsNotActive_alarmStatusNoAlarm(){
        Set<Sensor> sensors = createSensors(false, 6);

        when(securityRepository.getSensors()).thenReturn(sensors);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);

        securityService.processImage(mock(BufferedImage.class));

        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, times(1)).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.NO_ALARM);
    }

    @Test // 9
    void systemDisarmed_alarmStatusNoAlarm(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, times(1)).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest // 10
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void systemArmed_allSensorsInactive(ArmingStatus armingStatus){
        Set<Sensor> sensors = createSensors(true, 9);

        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.setArmingStatus(armingStatus);

        sensors.forEach(s -> assertEquals(s.getActive(), false));
    }

    @Test // 11
    void cameraShowCat_systemArmedHome_alarmStatusAlarm(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);

        securityService.processImage(mock(BufferedImage.class));

        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, times(1)).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.ALARM);
    }
}
