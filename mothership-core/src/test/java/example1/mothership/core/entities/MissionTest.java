package example1.mothership.core.entities;

import example1.mothership.core.services.TemperatureService;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static example1.mothership.core.MothershipDataSchema.*;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Plateau.class)
public class MissionTest {

    @Test
    public void must_delegate_can_launch_to_plateau() {

        val missionId = new MissionId("deep space");
        val plateauMock = PowerMockito.mock(Plateau.class);
        val mission = new Mission(missionId, plateauMock);
        val service = Mockito.mock(TemperatureService.class);

        mission.canLaunchRover(new RoverId("r1"), new PlateauLocation(2, 2), service);

        verify(plateauMock).canLaunchRover(new RoverId("r1"), new PlateauLocation(2, 2), service);

    }


}