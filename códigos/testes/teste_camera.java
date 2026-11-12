package org.firstinspires.ftc.teamcode;

import android.util.Size;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagLibrary;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import java.util.List;

// para ver a imagem, camera stream 
@TeleOp(name = "Teste Camera Tags", group = "Teste")
public class TesteCameraTags extends LinearOpMode {

    static final double TAG_CM = 8.25;

    @Override
    public void runOpMode(){
        AprilTagLibrary.Builder biblioteca=new AprilTagLibrary.Builder();
        for(int id=30; id<=45; id++){
            biblioteca.addTag(id, "BIOBUZZ " + id, TAG_CM, DistanceUnit.CM);
        }

        AprilTagProcessor aprilTag = new AprilTagProcessor.Builder()
                .setTagLibrary(biblioteca.build())
                .setTagFamily(AprilTagProcessor.TagFamily.TAG_36h11)
                .setOutputUnits(DistanceUnit.CM, AngleUnit.DEGREES)
                .build();
        aprilTag.setDecimation(2);   // testar 1 a 3

        VisionPortal portal = new VisionPortal.Builder()
                .setCamera(hardwareMap.get(WebcamName.class, "Webcam 1"))
                .setCameraResolution(new Size(640, 480))
                .addProcessor(aprilTag)
                .build();

        telemetry.addData("Status", "Pronto. Aperte START e aponte a camera pras tags");
        telemetry.update();
        waitForStart();

        while (opModeIsActive()){
            List<AprilTagDetection> deteccoes=aprilTag.getDetections();
            telemetry.addData("Tags vistas", deteccoes.size());
            telemetry.addData("FPS da camera", "%.0f", portal.getFps());
            for (AprilTagDetection d : deteccoes){
                if(d.ftcPose != null){
                    telemetry.addData("Tag " + d.id, "dist %.1f cm | direcao %.1f | yaw %.1f | elevacao %.1f",
                            d.ftcPose.range, d.ftcPose.bearing, d.ftcPose.yaw, d.ftcPose.elevation);
                }else{
                    telemetry.addData("Tag " + d.id, "vista, mas SEM pose (fora da biblioteca ou camera sem calibracao)");
                }
            }
            telemetry.update();
            sleep(50);
        }
        portal.close();
    }
}
