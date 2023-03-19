package es;

import java.io.FileNotFoundException;
import java.util.List;

import net.sf.clipsrules.jni.CLIPSException;
import net.sf.clipsrules.jni.Environment;
import net.sf.clipsrules.jni.PrimitiveValue;
import net.sf.clipsrules.jni.UserFunction;

public class CLIPSUtil {

	public static Environment initCLIPS(Robot robot) {
		Environment clips = new Environment();
		
		clips.addUserFunction("cleanCarpet", new UserFunction() {
			
			@Override
			public PrimitiveValue evaluate(List<PrimitiveValue> arguments) {
				robot.cleanCarpet();
				return null;
			}
		});
		
		clips.addUserFunction("cleanFloor", new UserFunction() {
			
			@Override
			public PrimitiveValue evaluate(List<PrimitiveValue> arguments) {
				robot.cleanFloor();
				return null;
			}
		});

		clips.addUserFunction("charge", new UserFunction() {

			@Override
			public PrimitiveValue evaluate(List<PrimitiveValue> arguments) {

				robot.charge();

				return null;
			}
		});

		clips.addUserFunction("handleTrap", new UserFunction() {

			@Override
			public PrimitiveValue evaluate(List<PrimitiveValue> arguments) {
				robot.handleTrap();
				return null;
			}
		});

		clips.addUserFunction("lt90", new UserFunction() {

			@Override
			public PrimitiveValue evaluate(List<PrimitiveValue> arguments) {
				robot.lt90();
				return null;
			}
		});

		clips.addUserFunction("rt90", new UserFunction() {

			@Override
			public PrimitiveValue evaluate(List<PrimitiveValue> arguments) {
				robot.rt90();
				return null;
			}
		});

		clips.addUserFunction("xlog", new UserFunction() {

			@Override
			public PrimitiveValue evaluate(List<PrimitiveValue> arguments) {
				for (var arg : arguments)
					System.out.println(arg);
				return null;
			}
		});

		clips.addUserFunction("fwd", new UserFunction() {

			@Override
			public PrimitiveValue evaluate(List<PrimitiveValue> arguments) {
				robot.fwd();
				return null;
			}
		});

		try {
			clips.loadFromResource("/es/resources/robot.clp");
			clips.reset();
		} catch (CLIPSException | FileNotFoundException e) {
			e.printStackTrace();
		}

		return clips;
	}

}
