package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

/* stick esq anda pra frente/trás
stick dir gira o robô*/

// versao 2 -> um joystick controla o movimento pra frente e pra tras, e o outro joystick controla a curva do robo
@TeleOp(name = "Chassi Basico - POV Drive", group = "Treino")
public class ChassiPovTeleOp extends LinearOpMode{
    private ElapsedTime runtime = new ElapsedTime();
    private DcMotor motorEsquerdo;
    private DcMotor motorDireito;
    private DcMotor motorShooter;
    private DcMotor motorIntake;
    private CRServo servoFeeder;

    @Override
    public void runOpMode(){
        motorEsquerdo =hardwareMap.get(DcMotor.class, "motoresquerdo");
        motorDireito = hardwareMap.get(DcMotor.class, "motordireito");
      
        motorShooter= hardwareMap.get(DcMotor.class, "shooter");
        motorIntake = hardwareMap.get(DcMotor.class, "intake");
        servoFeeder = hardwareMap.get(CRServo.class, "fedeer");
      
        motorEsquerdo.setDirection(DcMotor.Direction.REVERSE);
        motorDireito.setDirection(DcMotor.Direction.FORWARD);
      
        motorShooter.setDirection(DcMotor.Direction.FORWARD);
        motorIntake.setDirection(DcMotor.Direction.FORWARD);
        servoFeeder.setDirection(CRServo.Direction.FORWARD);
      
        motorEsquerdo.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorDireito.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
      
        motorShooter.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        motorIntake.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        telemetry.addData("Status", "Pronto para iniciar");
        telemetry.update();
        waitForStart();
        runtime.reset();
        boolean shooterLigado=false;
        boolean botaoXAnterior=false;
        double potenciaFixaShooter=0.8;
        double potenciaFixaIntake=1.0;
        while(opModeIsActive()){
            double andar=-gamepad1.left_stick_y;
            double girar = gamepad1.right_stick_x;

            double potenciaEsquerda=andar+girar;
            double potenciaDireita=andar-girar;
            potenciaEsquerda=Range.clip(potenciaEsquerda, -1.0, 1.0);
            potenciaDireita=Range.clip(potenciaDireita, -1.0, 1.0);
            motorEsquerdo.setPower(potenciaEsquerda);
            motorDireito.setPower(potenciaDireita);

            // shooter -> botao x liga/desliga, sempre na potencia fixa
            if (gamepad1.x && !botaoXAnterior){
                shooterLigado=!shooterLigado;
            }
            botaoXAnterior = gamepad1.x;
            double potenciaShooter = 0;
            if(shooterLigado){
                potenciaShooter=potenciaFixaShooter;
            }
            motorShooter.setPower(potenciaShooter);

            // intake -> segura y pra puxar as bolinhas, solta pra parar
            double potenciaIntake = 0;
            if(gamepad1.y){
                potenciaIntake=potenciaFixaIntake;
            }
            motorIntake.setPower(potenciaIntake);

            // feeder -> a empurra a bolinha pro shooter, b gira ao contrario
            double potenciaServo = 0;
            if(gamepad1.a){
                potenciaServo=1.0;
            }else if(gamepad1.b){
                potenciaServo=-1.0;
            }
            servoFeeder.setPower(potenciaServo);
            
            telemetry.addData("Status", "Tempo de execução: " + runtime.toString());
            telemetry.addData("Andar / Girar", "%.2f / %.2f", andar, girar);
            telemetry.addData("Motores", "esquerdo (%.2f), direito (%.2f)", potenciaEsquerda, potenciaDireita);
            telemetry.addData("Shooter / Intake", "%.2f / %.2f", potenciaShooter, potenciaIntake);
            telemetry.addData("Feeder", "%.2f", potenciaServo);
            telemetry.update();
        }
    }
}
