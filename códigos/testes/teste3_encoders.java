package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

/* stick esq anda pra frente/trás
stick dir gira o robô*/

// versao 2 -> um joystick controla o movimento pra frente e pra tras, e o outro joystick controla a curva do robo
@TeleOp(name = "Chassi Basico - Drive", group = "Treino")
public class Chassi2TeleOp extends LinearOpMode {
    private ElapsedTime runtime = new ElapsedTime();
    private DcMotor motorEsquerdo;
    private DcMotor motorDireito;
    private DcMotor motorShooter;
    private DcMotor motorIntake;
    private Servo servoAlimentador;

    @Override
    public void runOpMode() {
        motorEsquerdo = hardwareMap.get(DcMotor.class, "motoresquerdo");
        motorDireito  = hardwareMap.get(DcMotor.class, "motordireito");
        motorShooter = hardwareMap.get(DcMotor.class, "motorshooter");
        motorIntake = hardwareMap.get(DcMotor.class, "motorintake");
        servoAlimentador = hardwareMap.get(Servo.class, "servoalimentador");
        motorEsquerdo.setDirection(DcMotor.Direction.REVERSE);
        motorDireito.setDirection(DcMotor.Direction.FORWARD);
        motorShooter.setDirection(DcMotor.Direction.FORWARD);
        motorIntake.setDirection(DcMotor.Direction.FORWARD);
        servoAlimentador.setDirection(Servo.Direction.FORWARD);
        motorEsquerdo.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorDireito.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorShooter.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        motorIntake.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);

        // encoders -> zera a contagem e liga o uso do encoder nos 4 motores, proprio da biblioteca
        motorEsquerdo.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorDireito.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorShooter.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorIntake.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorEsquerdo.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorDireito.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorShooter.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorIntake.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        telemetry.addData("Status", "Pronto para iniciar");
        telemetry.update();
        waitForStart();
        runtime.reset();
        boolean shooterLigado = false;
        boolean botaoXAnterior = false;
        boolean intakeLigado = false;
        boolean botaoYAnterior = false;
        double potenciaFixaShooter = 0.8;
        double potenciaFixaIntake = 1.0;
        double posicaoRepouso = 0.0;
        double posicaoEmpurrar = 1;
        servoAlimentador.setPosition(posicaoRepouso);
        while (opModeIsActive()) {
            double andar=-gamepad1.left_stick_y;
            double girar = gamepad1.right_stick_x;

            double potenciaEsquerda=andar+girar;
            double potenciaDireita=andar-girar;
            potenciaEsquerda=Range.clip(potenciaEsquerda, -1.0, 1.0);
            potenciaDireita=Range.clip(potenciaDireita, -1.0, 1.0);
            motorEsquerdo.setPower(potenciaEsquerda);
            motorDireito.setPower(potenciaDireita);

            // shooter -> botao X liga/desliga, sempre na potencia fixa
            if (gamepad1.x && !botaoXAnterior){
                shooterLigado = !shooterLigado;
            }
            botaoXAnterior = gamepad1.x;
            double potenciaShooter = 0;
            if (shooterLigado){
                potenciaShooter = potenciaFixaShooter;
            }
            motorShooter.setPower(potenciaShooter);

            // intake -> botao Y liga/desliga, sempre na potencia fixa
            if (gamepad1.y && !botaoYAnterior){
                intakeLigado = !intakeLigado;
            }
            botaoYAnterior = gamepad1.y;
            double potenciaIntake = 0;
            if (intakeLigado){
                potenciaIntake = potenciaFixaIntake;
            }
            motorIntake.setPower(potenciaIntake);

            // servo -> segura A pra empurrar a bolinha, solta pra voltar
            double posicaoServo = posicaoRepouso;
            if (gamepad1.a){
                posicaoServo = posicaoEmpurrar;
            }
            servoAlimentador.setPosition(posicaoServo);
            
            telemetry.addData("Status", "Tempo de execução: " + runtime.toString());
            telemetry.addData("Andar / Girar", "%.2f / %.2f", andar, girar);
            telemetry.addData("Motores", "esquerdo (%.2f), direito (%.2f)", potenciaEsquerda, potenciaDireita);
            telemetry.addData("Shooter / Intake", "%.2f / %.2f", potenciaShooter, potenciaIntake);
            telemetry.addData("Servo", "%.2f", posicaoServo);
            telemetry.addData("Encoders", "esq (%d), dir (%d), shooter (%d), intake (%d)", motorEsquerdo.getCurrentPosition(), motorDireito.getCurrentPosition(),motorShooter.getCurrentPosition(), motorIntake.getCurrentPosition());
            telemetry.update();
        }
    }
}
