package example1.mothership.core.entities;

import example1.mothership.core.services.TemperatureService;
import javaslang.collection.HashMap;
import javaslang.collection.Map;
import lombok.val;
import org.junit.Test;

import static example1.mothership.core.MothershipDataSchema.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlateauTest {

    @Test
    public void should_work_on_normal_conditions() {

        val plateau = new Plateau(new PlateauId("inferno"), new PlateauDimension(6, 6));
        val mockService = mock(TemperatureService.class);
        when(mockService.currentTemperatureInCelsius()).thenReturn(99f);

        plateau.canLaunchRover(new RoverId("innocent"), new PlateauLocation(0,0), mockService);

    }

    @Test(expected = Exception.class)
    public void should_fail_if_too_high_tempeature() {

        val plateau = new Plateau(new PlateauId("inferno"), new PlateauDimension(6, 6));
        val mockService = mock(TemperatureService.class);
        when(mockService.currentTemperatureInCelsius()).thenReturn(200f);

        plateau.canLaunchRover(new RoverId("innocent"), new PlateauLocation(0,0), mockService);

    }

    @Test(expected = Exception.class)
    public void should_fail_on_position_conflict() {

        val roverPosition1 = new PlateauLocation(2, 3);
        val roverPosition2 = new PlateauLocation(3, 4);

        Map<String, PlateauLocation> map = HashMap.of(new RoverId("r1").getId(), roverPosition1,
                new RoverId("r2").getId(), roverPosition2);

        val plateau = new Plateau(new PlateauId("inferno"), new PlateauDimension(6, 6), map);

        val mockService = mock(TemperatureService.class);
        when(mockService.currentTemperatureInCelsius()).thenReturn(50f);

        plateau.canLaunchRover(new RoverId("innocent"), roverPosition1, mockService);

    }

    @Test(expected = Exception.class)
    public void should_fail_on_rover_conflict() {

        val roverPosition1 = new PlateauLocation(2, 3);
        Map<String, PlateauLocation> map = HashMap.of(new RoverId("r1").getId(), roverPosition1);

        val plateau = new Plateau(new PlateauId("inferno"), new PlateauDimension(6, 6), map);

        val mockService = mock(TemperatureService.class);
        when(mockService.currentTemperatureInCelsius()).thenReturn(50f);

        plateau.canLaunchRover(new RoverId("r1"), roverPosition1, mockService);

    }
}