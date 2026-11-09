package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

@TeleOp(name = "Teste Movimentação", group = "Treino")
public class Chassi11TesteMovimentacao extends LinearOpMode{

    private ElapsedTime runtime = new ElapsedTime();
    private ElapsedTime tempoEspera = new ElapsedTime();

    private DcMotorEx motorEsquerdo;
    private DcMotorEx motorDireito;
    private IMU imu;
    static final int LADO = 1;
    static final double TICKS_POR_ROTACAO = 28.0;
    static final double REDUCAO_MOVIMENTACAO = 6.2;
    static final double DIAMETRO_RODA_CM = 9;
    static final double TICKS_POR_CM = (TICKS_POR_ROTACAO * REDUCAO_MOVIMENTACAO) / (Math.PI * DIAMETRO_RODA_CM);
    static final double POTENCIA_ANDAR = 0.5;
    static final double TEMPO_MAX_MOVIMENTO_SEG = 5.0;

    // giro pelo giroscopio
    static final double POTENCIA_GIRO = 0.4;
    static final double POTENCIA_MIN_GIRO = 0.15;
    static final double KP_GIRO = 0.02;
    static final double TOLERANCIA_GIRO_GRAUS = 2.0;
    static final double TEMPO_MAX_GIRO_SEG = 3.0;

    // trajeto (mesmos nomes do autonomo, pra copiar os valores direto)
    static final double DISTANCIA_ATE_TIRO_CM = 100;
    static final double ANGULO_TIRO_GRAUS = -45;

    // so do teste
    static final double DISTANCIA_TESTE_CM = 100; // distancia dos botoes A e B (confere com a trena)
    static final double TEMPO_SIMULA_TIRO_SEG = 5.0; // pausa que representa o tempo de atirar no trajeto completo

    @Override
    public void runOpMode() {
        motorEsquerdo = hardwareMap.get(DcMotorEx.class, "motoresquerdo");
        motorDireito = hardwareMap.get(DcMotorEx.class, "motordireito");

        motorEsquerdo.setDirection(DcMotor.Direction.REVERSE);
        motorDireito.setDirection(DcMotor.Direction.FORWARD);

        motorEsquerdo.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorDireito.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        motorEsquerdo.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorDireito.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorEsquerdo.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorDireito.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        imu = hardwareMap.get(IMU.class, "imu");
        imu.initialize(new IMU.Parameters(new RevHubOrientationOnRobot(
                RevHubOrientationOnRobot.LogoFacingDirection.UP,
                RevHubOrientationOnRobot.UsbFacingDirection.FORWARD)));

        telemetry.addData("Status", "Pronto. Aperte START");
        telemetry.update();

        waitForStart();
        imu.resetYaw();

        while (opModeIsActive()) {
            int esq0 = motorEsquerdo.getCurrentPosition();
            int dir0 = motorDireito.getCurrentPosition();
            String acao = "";

            if (gamepad1.a){
                acao = "andar " + DISTANCIA_TESTE_CM + " cm";
                andarCm(DISTANCIA_TESTE_CM, POTENCIA_ANDAR);
            }else if (gamepad1.b){
                acao = "andar -" + DISTANCIA_TESTE_CM + " cm";
                andarCm(-DISTANCIA_TESTE_CM, POTENCIA_ANDAR);
            }else if (gamepad1.x){
                acao = "girar para 90";
                girarParaAngulo(90, POTENCIA_GIRO);
            }else if (gamepad1.y){
                acao = "girar para -90";
                girarParaAngulo(-90, POTENCIA_GIRO);
            }else if (gamepad1.left_bumper){
                acao = "girar para 0";
                girarParaAngulo(0, POTENCIA_GIRO);
            }else if (gamepad1.dpad_up){
                acao = "girar para ANGULO_TIRO_GRAUS";
                girarParaAngulo(ANGULO_TIRO_GRAUS*LADO, POTENCIA_GIRO);
            }else if (gamepad1.right_bumper){
                acao = "trajeto completo";
                trajetoCompleto();
            }else if (gamepad1.back){
                imu.resetYaw();
                acao = "giroscopio zerado";
            }

            if (!acao.isEmpty()){
                sleep(500); // deixa o robo assentar antes de ler
                mostrarResultado(acao, esq0, dir0);
                while (opModeIsActive() && algumBotaoApertado()) {
                    idle();
                }
            }

            if (acao.isEmpty()){
                telemetry.addData("Aguardando", "A/B andar | X/Y/LB/DPAD girar | RB trajeto | BACK zera giro");
                telemetry.addData("Angulo atual", "%.1f graus", pegarHeading());
                telemetry.update();
            }
        }
    }

    private boolean algumBotaoApertado(){
        return gamepad1.a || gamepad1.b || gamepad1.x || gamepad1.y
                || gamepad1.left_bumper || gamepad1.right_bumper
                || gamepad1.dpad_up || gamepad1.back;
    }

    // mesmo trajeto do autonomo, mas sem shooter: anda, gira, espera, volta ao angulo 0 e da re
    private void trajetoCompleto() {
        runtime.reset();
        andarCm(DISTANCIA_ATE_TIRO_CM, POTENCIA_ANDAR);
        girarParaAngulo(ANGULO_TIRO_GRAUS * LADO, POTENCIA_GIRO);
        telemetry.addData("Simulando tiro", "%.1f s", TEMPO_SIMULA_TIRO_SEG);
        telemetry.update();
        sleep((long) (TEMPO_SIMULA_TIRO_SEG * 1000));
        girarParaAngulo(0, POTENCIA_GIRO);
        andarCm(-DISTANCIA_ATE_TIRO_CM, POTENCIA_ANDAR);
        telemetry.addData("Tempo do trajeto", "%.1f s", runtime.seconds());
    }

    private void mostrarResultado(String acao, int esq0, int dir0){
        int esq = motorEsquerdo.getCurrentPosition() - esq0;
        int dir = motorDireito.getCurrentPosition() - dir0;
        telemetry.addData("Acao", acao);
        telemetry.addData("Ticks esq / dir", "%d / %d", esq, dir);
        telemetry.addData("Distancia pelo encoder (cm)", "esq %.1f | dir %.1f", esq / TICKS_POR_CM, dir / TICKS_POR_CM);
        telemetry.addData("Angulo final", "%.1f graus", pegarHeading());
        telemetry.addData("Calibrar", "novo diametro = diametro atual x distancia real / distancia comandada");
        telemetry.addData("Diametro atual (cm)", "%.2f", DIAMETRO_RODA_CM);
        telemetry.update();
    }

    private void andarCm(double cm, double potencia){
        int ticks = (int) (cm*TICKS_POR_CM);
        motorEsquerdo.setTargetPosition(motorEsquerdo.getCurrentPosition() + ticks);
        motorDireito.setTargetPosition(motorDireito.getCurrentPosition() + ticks);
        motorEsquerdo.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motorDireito.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motorEsquerdo.setPower(Math.abs(potencia));
        motorDireito.setPower(Math.abs(potencia));
        tempoEspera.reset();
        while (opModeIsActive() && (motorEsquerdo.isBusy() || motorDireito.isBusy()) && tempoEspera.seconds() < TEMPO_MAX_MOVIMENTO_SEG) {
            telemetry.addData("Andando", "esq (%d), dir (%d)", motorEsquerdo.getCurrentPosition(), motorDireito.getCurrentPosition());
            telemetry.update();
        }
        motorEsquerdo.setPower(0);
        motorDireito.setPower(0);
        motorEsquerdo.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorDireito.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    private void girarParaAngulo(double alvoGraus, double potenciaMax){
        double alvo = normalizarAngulo(alvoGraus);
        tempoEspera.reset();
        while (opModeIsActive() && tempoEspera.seconds() < TEMPO_MAX_GIRO_SEG){
            double erro = normalizarAngulo(alvo - pegarHeading());
            if (Math.abs(erro) <= TOLERANCIA_GIRO_GRAUS) {
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

    private double pegarHeading() {
        return imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES);
    }

    private double normalizarAngulo(double angulo) {
        while (angulo > 180) angulo -= 360;
        while (angulo < -180) angulo += 360;
        return angulo;
    }
}
