package frc.robot.utilities;

import frc.robot.subsystems.messaging.MessagingSystem;
import frc.robot.subsystems.vision.Vision;
import java.util.TimerTask;

import org.littletonrobotics.junction.Logger;

public class LogSubsystemInputsTask extends TimerTask {
	private LogInputs loggingHelper = LogInputs.getInstance();
	private Loggable[] loggingTargets = {
		Vision.getInstance(),
		MessagingSystem.getInstance()
	};

	@Override
	public void run() {
		for (Loggable target : loggingTargets) {
			loggingHelper.setLoggingTarget(target);
			Logger.getInstance().processInputs(target.getTableName(), loggingHelper);
		}
	}
}
