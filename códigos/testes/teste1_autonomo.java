package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

// fluxo: anda ate o ponto de tiro (shooter ja acelerando) -> vira pra hive -> atira so as bolinhas que viram a cell -> desliga o shooter -> volta pro canto
// anda por encoder, gira pelo giroscopio, usa a camera so pra conferir a distancia na telemetria
@Autonomous(name = "Chassi Basico - Autonomo", group = "Treino")
public class Chassi2Autonomo extends LinearOpMode {
    private ElapsedTime runtime = new ElapsedTime();
    private ElapsedTime tempoEspera = new ElapsedTime();
    private DcMotorEx motorEsquerdo;
    private DcMotorEx motorDireito;
    private DcMotorEx motorShooter;
    private DcMotor motorIntake;
    private CRServo servoPortinhola1;
    private CRServo servoPortinhola2;
    private CRServo servoPortinhola3;
    private IMU imu;
    private AprilTagProcessor aprilTag;
    private VisionPortal visionPortal;

    // lado do trajeto -> 1 = comeca no canto esquerdo (hive vermelha), -1 = espelha o trajeto (outro lado)
    static final int LADO = 1;

    // ids das tags da sua alianca -> vermelho = 30 a 37, azul = 38 a 45
    static final int ID_TAG_MIN = 30;
    static final int ID_TAG_MAX = 37;

    // movimentacao (HD Hex = 28 ticks no eixo do motor, multiplica pela reducao)
    static final double TICKS_POR_ROTACAO = 28.0;
    static final double REDUCAO_MOVIMENTACAO = 20.0;   // ex: 4:1 x 5:1
    static final double DIAMETRO_RODA_CM = 9.6;
    static final double TICKS_POR_CM = (TICKS_POR_ROTACAO * REDUCAO_MOVIMENTACAO) / (Math.PI * DIAMETRO_RODA_CM);
    static final double POTENCIA_ANDAR = 0.5;
    static final double TEMPO_MAX_MOVIMENTO_SEG = 5.0; // desiste do movimento se travar (robo preso)

    // giro pelo giroscopio
    static final double POTENCIA_GIRO = 0.4;           // potencia maxima do giro
    static final double POTENCIA_MIN_GIRO = 0.15;      // potencia minima pra vencer o atrito
    static final double KP_GIRO = 0.02;                // quanto mais perto do angulo, mais devagar
    static final double TOLERANCIA_GIRO_GRAUS = 2.0;   // erro aceito no angulo final
    static final double TEMPO_MAX_GIRO_SEG = 3.0;      // desiste do giro se nao chegar no angulo

    // shooter
    static final double REDUCAO_SHOOTER = 1.0;         // reducao real do motor do shooter
    static final double POTENCIA_SHOOTER = 0.8;
    static final double RPM_ALVO_SHOOTER = 4000;
    static final double RPM_TOLERANCIA = 150;
    static final double TEMPO_MAX_SHOOTER_SEG = 3.0;   // desiste de esperar o shooter depois disso

    // portinhola e intake
    static final double TEMPO_ABRIR_SEG = 0.4;
    static final double TEMPO_FECHAR_SEG = 0.4;
    static final double TEMPO_INTAKE_SEG = 0.8;        // quanto tempo o intake empurra UMA bolinha
    static final double TEMPO_ENTRE_TIROS_SEG = 0.3;   // pausa depois de cada bolinha

    // tiro
    static final int BOLINHAS_PRE_CARREGADAS = 4;      // quantas pollen o robo leva
    static final int BOLINHAS_PARA_VIRAR = 3;          // quantas bolinhas viram a cell (descobrir testando!)
    static final double TEMPO_LIMITE_TIRO_SEG = 22.0;  // depois disso para de atirar e volta pro canto (autonomo dura 30 s)

    // trajeto (medir na arena)
    static final double DISTANCIA_ATE_TIRO_CM = 100;   // quanto anda pra frente ate o ponto de tiro
    static final double ANGULO_TIRO_GRAUS = -45;       // quanto gira pra mirar na hive (negativo = direita, positivo = esquerda)

    @Override
    public void runOpMode() {
        motorEsquerdo = hardwareMap.get(DcMotorEx.class, "motoresquerdo");
        motorDireito  = hardwareMap.get(DcMotorEx.class, "motordireito");
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

        // giroscopio -> mesma montagem do teleop
        imu = hardwareMap.get(IMU.class, "imu");
        imu.initialize(new IMU.Parameters(new RevHubOrientationOnRobot(
                RevHubOrientationOnRobot.LogoFacingDirection.UP,
                RevHubOrientationOnRobot.UsbFacingDirection.FORWARD)));

        // camera -> processador de AprilTag
        aprilTag = new AprilTagProcessor.Builder()
                .setOutputUnits(DistanceUnit.CM, AngleUnit.DEGREES)
                .build();
        visionPortal = new VisionPortal.Builder()
                .setCamera(hardwareMap.get(WebcamName.class, "Webcam 1"))
                .addProcessor(aprilTag)
                .build();

        telemetry.addData("Status", "Pronto para iniciar");
        telemetry.addData("Posicao inicial", "canto inferior esquerdo, de frente, com %d pollen", BOLINHAS_PRE_CARREGADAS);
        telemetry.update();
        waitForStart();
        runtime.reset();
        imu.resetYaw();   // o angulo 0 e a direcao em que o robo comecou (de frente no canto)

        int bolinhasParaAtirar = Math.min(BOLINHAS_PARA_VIRAR, BOLINHAS_PRE_CARREGADAS);

        // 1) liga o shooter ja no comeco e anda ate o ponto de tiro (ele acelera enquanto o robo anda)
        motorShooter.setPower(POTENCIA_SHOOTER);
        andarCm(DISTANCIA_ATE_TIRO_CM, POTENCIA_ANDAR);

        // 2) vira pra hive
        girarParaAngulo(ANGULO_TIRO_GRAUS * LADO, POTENCIA_GIRO);

        // 3) atira so as bolinhas necessarias pra virar a colmeia
        int disparos = atirarSerie(bolinhasParaAtirar);

        // 4) desliga o shooter, volta pro angulo inicial e volta de re pro canto
        motorShooter.setPower(0);
        girarParaAngulo(0, POTENCIA_GIRO);
        andarCm(-DISTANCIA_ATE_TIRO_CM, POTENCIA_ANDAR);

        telemetry.addData("Status", "Terminou em %.1f s", runtime.seconds());
        telemetry.addData("Bolinhas lancadas", "%d de %d", disparos, bolinhasParaAtirar);
        telemetry.update();
        visionPortal.close();
    }

    // anda pra frente (cm positivo) ou pra tras (cm negativo) usando o encoder
    private void andarCm(double cm, double potencia){
        int ticks = (int) (cm * TICKS_POR_CM);
        motorEsquerdo.setTargetPosition(motorEsquerdo.getCurrentPosition() + ticks);
        motorDireito.setTargetPosition(motorDireito.getCurrentPosition() + ticks);
        motorEsquerdo.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motorDireito.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motorEsquerdo.setPower(Math.abs(potencia));
        motorDireito.setPower(Math.abs(potencia));
        tempoEspera.reset();
        while (opModeIsActive() && (motorEsquerdo.isBusy() || motorDireito.isBusy()) && tempoEspera.seconds() < TEMPO_MAX_MOVIMENTO_SEG){
            telemetry.addData("Andando", "esq (%d), dir (%d)", motorEsquerdo.getCurrentPosition(), motorDireito.getCurrentPosition());
            telemetry.update();
        }
        motorEsquerdo.setPower(0);
        motorDireito.setPower(0);
        motorEsquerdo.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorDireito.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    // gira o robo ate um angulo do giroscopio (0 = como o robo comecou, positivo = anti-horario, negativo = horario)
    private void girarParaAngulo(double alvoGraus, double potenciaMax){
        double alvo = normalizarAngulo(alvoGraus);
        tempoEspera.reset();
        while (opModeIsActive() && tempoEspera.seconds() < TEMPO_MAX_GIRO_SEG){
            double erro = normalizarAngulo(alvo - pegarHeading());
            if (Math.abs(erro) <= TOLERANCIA_GIRO_GRAUS){
                break;
            }
            double potencia = erro * KP_GIRO;
            potencia = Range.clip(Math.abs(potencia), POTENCIA_MIN_GIRO, potenciaMax) * Math.signum(erro);
            motorEsquerdo.setPower(-potencia);
            motorDireito.setPower(potencia);
            telemetry.addData("Girando", "erro %.1f graus", erro);
            telemetry.update();
        }
        motorEsquerdo.setPower(0);
        motorDireito.setPower(0);
    }

    // atira a quantidade de bolinhas pedida e devolve quantas realmente foram lancadas
    // a portinhola abre uma vez, cada bolinha espera o shooter recuperar a rpm e o intake empurra uma por vez
    private int atirarSerie(int quantidade){
        int disparos = 0;
        setPotenciaServos(1.0);
        sleep((long) (TEMPO_ABRIR_SEG * 1000));
        setPotenciaServos(0);
        while (opModeIsActive() && disparos < quantidade && runtime.seconds() < TEMPO_LIMITE_TIRO_SEG){
            boolean pronto = esperarShooterPronto();
            motorIntake.setPower(1.0);
            sleep((long) (TEMPO_INTAKE_SEG * 1000));
            motorIntake.setPower(0);
            disparos++;
            telemetry.addData("Tiro", "%d de %d (shooter pronto: %b)", disparos, quantidade, pronto);
            telemetry.update();
            sleep((long) (TEMPO_ENTRE_TIROS_SEG * 1000));
        }
        setPotenciaServos(-1.0);
        sleep((long) (TEMPO_FECHAR_SEG * 1000));
        setPotenciaServos(0);
        return disparos;
    }

    // espera o shooter chegar na rpm alvo, devolve false se desistiu pelo tempo maximo
    private boolean esperarShooterPronto(){
        tempoEspera.reset();
        while (opModeIsActive() && tempoEspera.seconds() < TEMPO_MAX_SHOOTER_SEG){
            double rpm = pegarRpmShooter();
            telemetry.addData("Shooter", "%.0f rpm (alvo %.0f)", rpm, RPM_ALVO_SHOOTER);
            telemetry.addData("Distancia tag (cm)", "%.1f", pegarDistancia());
            telemetry.update();
            if (Math.abs(rpm - RPM_ALVO_SHOOTER) <= RPM_TOLERANCIA){
                return true;
            }
        }
        return false;
    }

    // liga os 3 servos da portinhola na mesma potencia
    private void setPotenciaServos(double potencia){
        servoPortinhola1.setPower(potencia);
        servoPortinhola2.setPower(potencia);
        servoPortinhola3.setPower(potencia);
    }

    // converte ticks por segundo em rpm do shooter
    private double pegarRpmShooter(){
        return motorShooter.getVelocity() / (TICKS_POR_ROTACAO * REDUCAO_SHOOTER) * 60.0;
    }

    // angulo atual do robo em graus
    private double pegarHeading(){
        return imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES);
    }

    // deixa o angulo sempre entre -180 e 180
    private double normalizarAngulo(double angulo){
        while (angulo > 180) angulo -= 360;
        while (angulo < -180) angulo += 360;
        return angulo;
    }

    private double pegarDistancia(){
        for (AprilTagDetection deteccao : aprilTag.getDetections()){
            if (deteccao.ftcPose != null && deteccao.id >= ID_TAG_MIN && deteccao.id <= ID_TAG_MAX){
                return deteccao.ftcPose.range;
            }
        }
        return -1;
    }
}
