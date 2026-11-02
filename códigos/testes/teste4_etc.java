package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

/* stick esq anda pra frente/trás
stick dir gira o robô*/
// fluxo de tiro: checa distancia na camera -> checa rpm do shooter -> abre portinhola -> liga intake
@TeleOp(name = "Chassi Basico - Drive", group = "Treino")
public class Chassi2TeleOp extends LinearOpMode {
    private ElapsedTime runtime = new ElapsedTime();
    private ElapsedTime tempoPortinhola = new ElapsedTime();
    private DcMotor motorEsquerdo;
    private DcMotor motorDireito;
    private DcMotorEx motorShooter;   // DcMotorEx pra conseguir ler a velocidade (getVelocity)
    private DcMotor motorIntake;
    private CRServo servoPortinhola1;
    private CRServo servoPortinhola2;
    private CRServo servoPortinhola3;
    private IMU imu;
    private AprilTagProcessor aprilTag;
    private VisionPortal visionPortal;

    static final double TICKS_POR_ROTACAO = 28.0;
    static final double RPM_ALVO_SHOOTER = 4000;     // rpm ideal pra lancar a bolinha
    static final double RPM_TOLERANCIA = 150;        // quanto pode variar e ainda estar "pronto"
    static final double DISTANCIA_MIN_CM = 60;       // distancia minima pra atirar
    static final double DISTANCIA_MAX_CM = 200;      // distancia maxima pra atirar
    static final double TEMPO_ABRIR_SEG = 0.4;       // quanto tempo os servos giram pra abrir
    static final double TEMPO_FECHAR_SEG = 0.4;      // quanto tempo os servos giram pra fechar

    // ids das tags da sua alianca -> vermelho = 30 a 37, azul = 38 a 45
    static final int ID_TAG_MIN = 30;
    static final int ID_TAG_MAX = 37;

    // estados da portinhola
    static final int FECHADA = 0;
    static final int ABRINDO = 1;
    static final int ABERTA = 2;
    static final int FECHANDO = 3;

    @Override
    public void runOpMode() {
        motorEsquerdo = hardwareMap.get(DcMotor.class, "motoresquerdo");
        motorDireito  = hardwareMap.get(DcMotor.class, "motordireito");
        motorShooter = hardwareMap.get(DcMotorEx.class, "motorshooter");
        motorIntake = hardwareMap.get(DcMotor.class, "motorintake");
        servoPortinhola1 = hardwareMap.get(CRServo.class, "servoportinhola1");
        servoPortinhola2 = hardwareMap.get(CRServo.class, "servoportinhola2");
        servoPortinhola3 = hardwareMap.get(CRServo.class, "servoportinhola3");
        motorEsquerdo.setDirection(DcMotor.Direction.REVERSE);
        motorDireito.setDirection(DcMotor.Direction.FORWARD);
        motorShooter.setDirection(DcMotor.Direction.FORWARD);
        motorIntake.setDirection(DcMotor.Direction.FORWARD);
        servoPortinhola1.setDirection(CRServo.Direction.FORWARD);
        servoPortinhola2.setDirection(CRServo.Direction.FORWARD);
        servoPortinhola3.setDirection(CRServo.Direction.FORWARD);
        motorEsquerdo.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorDireito.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorShooter.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        motorIntake.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);

        // encoders -> zera a contagem e liga o uso do encoder nos motores (intake nao tem encoder)
        motorEsquerdo.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorDireito.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorShooter.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorEsquerdo.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorDireito.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorShooter.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorIntake.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        // giroscopio -> IMU do control hub (ajustar as direcoes conforme a montagem do hub no robo)
        imu = hardwareMap.get(IMU.class, "imu");
        imu.initialize(new IMU.Parameters(new RevHubOrientationOnRobot(
                RevHubOrientationOnRobot.LogoFacingDirection.UP,
                RevHubOrientationOnRobot.UsbFacingDirection.FORWARD)));
        imu.resetYaw();

        // camera -> processador de AprilTag pra medir a distancia ate o alvo
        aprilTag = new AprilTagProcessor.Builder()
                .setOutputUnits(DistanceUnit.CM, AngleUnit.DEGREES)
                .build();
        visionPortal = new VisionPortal.Builder()
                .setCamera(hardwareMap.get(WebcamName.class, "Webcam 1"))
                .addProcessor(aprilTag)
                .build();

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
        double potenciaServoAbrir = 1.0;
        double potenciaServoFechar = -1.0;
        int estadoPortinhola = FECHADA;
        while (opModeIsActive()) {
            double andar=-gamepad1.left_stick_y;
            double girar = gamepad1.right_stick_x;

            double potenciaEsquerda=andar+girar;
            double potenciaDireita=andar-girar;
            potenciaEsquerda=Range.clip(potenciaEsquerda, -1.0, 1.0);
            potenciaDireita=Range.clip(potenciaDireita, -1.0, 1.0);
            motorEsquerdo.setPower(potenciaEsquerda);
            motorDireito.setPower(potenciaDireita);

            // giroscopio -> pega o angulo (heading) do robo, so pra mostrar na telemetria
            YawPitchRollAngles angulos = imu.getRobotYawPitchRollAngles();
            double heading = angulos.getYaw(AngleUnit.DEGREES);

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

            // encoder do shooter -> converte ticks por segundo em rpm e confere se ta na velocidade de lancamento
            double rpmShooter = motorShooter.getVelocity() / TICKS_POR_ROTACAO * 60.0;
            boolean shooterPronto = shooterLigado && Math.abs(rpmShooter - RPM_ALVO_SHOOTER) <= RPM_TOLERANCIA;

            // camera -> distancia ate o alvo em cm (-1 se nao estiver vendo nenhuma tag da alianca)
            double distanciaCm = pegarDistancia();
            boolean distanciaOk = distanciaCm >= DISTANCIA_MIN_CM && distanciaCm <= DISTANCIA_MAX_CM;

            // intake -> botao Y liga/desliga, sempre na potencia fixa
            if (gamepad1.y && !botaoYAnterior){
                intakeLigado = !intakeLigado;
            }
            botaoYAnterior = gamepad1.y;

            // portinhola -> segura A pra atirar, solta pra fechar
            // so abre se a distancia estiver ok e o shooter na rpm certa (segurar o RP ignora a distancia)
            boolean podeAtirar = shooterPronto && (distanciaOk || gamepad1.right_bumper);
            if (estadoPortinhola == FECHADA && gamepad1.a && podeAtirar){
                estadoPortinhola = ABRINDO;
                tempoPortinhola.reset();
                setPotenciaServos(potenciaServoAbrir);
            }
            if (estadoPortinhola == ABRINDO && tempoPortinhola.seconds() >= TEMPO_ABRIR_SEG){
                estadoPortinhola = ABERTA;
                setPotenciaServos(0);
            }
            if (estadoPortinhola == ABERTA && !gamepad1.a){
                estadoPortinhola = FECHANDO;
                tempoPortinhola.reset();
                setPotenciaServos(potenciaServoFechar);
            }
            if (estadoPortinhola == FECHANDO && tempoPortinhola.seconds() >= TEMPO_FECHAR_SEG){
                estadoPortinhola = FECHADA;
                setPotenciaServos(0);
            }

            // intake -> liga se o Y estiver ligado ou se a portinhola ja abriu (ultimo passo do fluxo)
            double potenciaIntake = 0;
            if (intakeLigado || estadoPortinhola == ABERTA){
                potenciaIntake = potenciaFixaIntake;
            }
            motorIntake.setPower(potenciaIntake);

            telemetry.addData("Status", "Tempo de execução: " + runtime.toString());
            telemetry.addData("Andar / Girar", "%.2f / %.2f", andar, girar);
            telemetry.addData("Motores", "esquerdo (%.2f), direito (%.2f)", potenciaEsquerda, potenciaDireita);
            telemetry.addData("Shooter / Intake", "%.2f / %.2f", potenciaShooter, potenciaIntake);
            telemetry.addData("RPM Shooter", "%.0f (alvo %.0f) pronto: %b", rpmShooter, RPM_ALVO_SHOOTER, shooterPronto);
            telemetry.addData("Distancia (cm)", "%.1f ok: %b", distanciaCm, distanciaOk);
            telemetry.addData("Portinhola", "estado %d", estadoPortinhola);
            telemetry.addData("Heading", "%.1f graus", heading);
            telemetry.addData("Encoders", "esq (%d), dir (%d), shooter (%d)", motorEsquerdo.getCurrentPosition(), motorDireito.getCurrentPosition(), motorShooter.getCurrentPosition());
            telemetry.update();
        }
        visionPortal.close();
    }

    // liga os 3 servos da portinhola na mesma potencia
    private void setPotenciaServos(double potencia){
        servoPortinhola1.setPower(potencia);
        servoPortinhola2.setPower(potencia);
        servoPortinhola3.setPower(potencia);
    }

    // pega a distancia ate a primeira tag da SUA alianca que a camera estiver vendo, retorna -1 se nao ver nenhuma
    private double pegarDistancia(){
        for (AprilTagDetection deteccao : aprilTag.getDetections()){
            if (deteccao.ftcPose != null && deteccao.id >= ID_TAG_MIN && deteccao.id <= ID_TAG_MAX){
                return deteccao.ftcPose.range;
            }
        }
        return -1;
    }
}
