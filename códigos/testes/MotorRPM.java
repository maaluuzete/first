package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;

@TeleOp(name = "Motor 2300 RPM", group = "Teste")
public class MotorRPM extends LinearOpMode{
    final double RPM_DESEJADA = 2300;
    final double TICKS_POR_VOLTA = 28;   

    @Override
    public void runOpMode(){
        DcMotorEx motor = hardwareMap.get(DcMotorEx.class, "motor");

        motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER); 
        double ticksPorSegundo = RPM_DESEJADA*TICKS_POR_VOLTA/60.0;

        telemetry.addLine("Pronto! Aperte START");
        telemetry.update();
        waitForStart();

        motor.setVelocity(ticksPorSegundo);

        while (opModeIsActive()){

            double rpmReal = motor.getVelocity()*60.0/TICKS_POR_VOLTA;

            telemetry.addData("RPM desejada", "%.0f", RPM_DESEJADA);
            telemetry.addData("RPM real", "%.0f", rpmReal);
            telemetry.addData("Ticks/s desejado", "%.0f", ticksPorSegundo);
            telemetry.addData("Ticks/s real", "%.0f", motor.getVelocity());
            telemetry.update();
        }

        motor.setVelocity(0);
    }
}
