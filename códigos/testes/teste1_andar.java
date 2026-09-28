package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

/* stick esq anda pra frente/trás
stick dir gira o robô*/

// versao 2 -> um joystick controla o movimento pra frente e pra tras, e o outro joystick controla a curva do robo
@TeleOp(name = "Chassi Basico - POV Drive", group = "Treino")
public class ChassiPovTeleOp extends LinearOpMode {
    private ElapsedTime runtime = new ElapsedTime();
    private DcMotor motorEsquerdo;
    private DcMotor motorDireito;

    @Override
    public void runOpMode() {
        motorEsquerdo = hardwareMap.get(DcMotor.class, "motoresquerdo");
        motorDireito  = hardwareMap.get(DcMotor.class, "motordireito");
        motorEsquerdo.setDirection(DcMotor.Direction.REVERSE);
        motorDireito.setDirection(DcMotor.Direction.FORWARD);
        motorEsquerdo.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorDireito.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        telemetry.addData("Status", "Pronto para iniciar");
        telemetry.update();
        waitForStart();
        runtime.reset();
        while (opModeIsActive()) {
            double andar=-gamepad1.left_stick_y;
            double girar = gamepad1.right_stick_x;

            double potenciaEsquerda=andar+girar;
            double potenciaDireita=andar-girar;
            potenciaEsquerda=Range.clip(potenciaEsquerda, -1.0, 1.0);
            potenciaDireita=Range.clip(potenciaDireita, -1.0, 1.0);
            motorEsquerdo.setPower(potenciaEsquerda);
            motorDireito.setPower(potenciaDireita);
            
            telemetry.addData("Status", "Tempo de execução: " + runtime.toString());
            telemetry.addData("Andar / Girar", "%.2f / %.2f", andar, girar);
            telemetry.addData("Motores", "esquerdo (%.2f), direito (%.2f)", potenciaEsquerda, potenciaDireita);
            telemetry.update();
        }
    }
}