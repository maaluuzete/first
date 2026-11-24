package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

/**
 * A parte de cima (runOpMode) é só a lista de passos: andar, girar, lançar...
 * A parte de baixo tem as funções andar(), girar() e lancar() que fazem o trabalho
 * (encoder para a distância, IMU para o ângulo, telemetria o tempo todo).
 * largando ENCOSTADO na parede, de frente para a hive, alinhado com a célula alta.
 * Ângulos: 0 = de frente para a hive, positivo = esquerda, negativo = direita.
 * EMERGÊNCIA: botão B ou A do gamepad 1.
 */
@Autonomous(name = "Auto Simples", group = "Autonomo")
public class AutoSimples extends LinearOpMode {

    // hardware
    DcMotor motorEsquerdo;
    DcMotor motorDireito;
    DcMotor intake;
    DcMotorEx shooter;
    IMU imu;

    //ajustar chassi
    double TICKS_POR_VOLTA  = 537.7;
    double DIAMETRO_RODA_CM = 9.6;
    double TICKS_POR_CM = TICKS_POR_VOLTA / (DIAMETRO_RODA_CM * Math.PI);

    //ajustar o shooter
    double VEL_TIRO_1 = 1500;   // largada
    double VEL_TIRO_2 = 1500;   // outro lado da arena (mesma distância)
    double VEL_TIRO_3 = 1450;   // depois da flor (um pouco mais perto)
    double TEMPO_LANCAR_S = 2.2; // quanto tempo o intake alimenta o shooter (chute por enquanto)

    String passoAtual = "";     // aparece na telemetria

    @Override
    public void runOpMode(){
        motorEsquerdo =hardwareMap.get(DcMotor.class, "motoresquerdo");
        motorDireito=hardwareMap.get(DcMotor.class, "motordireito");
        intake=hardwareMap.get(DcMotor.class, "intake");
        shooter=hardwareMap.get(DcMotorEx.class, "shooter");
        imu=hardwareMap.get(IMU.class, "imu");

        motorEsquerdo.setDirection(DcMotorSimple.Direction.REVERSE);  // se andar pra trás, inverta os dois
        motorDireito.setDirection(DcMotorSimple.Direction.FORWARD);
        motorEsquerdo.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorDireito.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        shooter.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // hub no robo
        imu.initialize(new IMU.Parameters(new RevHubOrientationOnRobot(
                RevHubOrientationOnRobot.LogoFacingDirection.FORWARD,
                RevHubOrientationOnRobot.UsbFacingDirection.DOWN)));

        telemetry.addLine("Largue ENCOSTADO na parede, de frente pra hive.");
        telemetry.addLine("EMERGENCIA: B ou A no gamepad 1");
        telemetry.update();

        waitForStart();
        resetRuntime();     // zera o cronômetro do autônomo
        imu.resetYaw();     // o ângulo 0 é a direção da largada

        // primeiro tiro
        passoAtual = "Tiro 1";
        lancar(VEL_TIRO_1);

        // só tenta o ciclo completo se ainda sobra tempo (senão vai direto estacionar)
        if (getRuntime() < 6.5) {

            // vai no jardim
            passoAtual = "Indo ao jardim";
            andar(14.3);          // sai da parede para poder girar
            girar(-90);           // vira para o lado da flor da parede
            andar(72.4);          // desce passando longe da flor
            girar(-106);          // aponta pro canto do jardim
            andar(41.7);
            girar(-90);           // de frente pros pollens

            passoAtual = "Coletando jardim";
            intake.setPower(1.0);
            andarLento(15.5);
            sleep(800);           // espera os pollens entrarem
            intake.setPower(0);

            // corre pro outro lado e atira
            passoAtual = "Indo pro outro lado";
            girar(-102);
            andar(-56.7);         // ré: se afasta da parede
            girar(90);            // vira para o lado da loading zone
            andar(127.6);         // sobe até ficar de frente pra célula
            girar(0);             // de frente pra hive
            andar(-14.3);         // ré até a posição de tiro

            passoAtual = "Tiro 2";
            lancar(VEL_TIRO_2);

            if (getRuntime()<18){

                //ir na flor do outro lado
                passoAtual = "Indo pra flor";
                andar(10.6);      // sai da parede
                girar(47);
                andar(126.9);
                girar(90);        // de frente pra flor

                passoAtual = "Coletando flor";
                intake.setPower(1.0);
                andarLento(24.2);
                sleep(800);
                intake.setPower(0);

                passoAtual = "Tiro 3";
                girar(-77);       // mira na célula
                lancar(VEL_TIRO_3);

                // estacionar
                passoAtual = "Estacionando";
                shooter.setVelocity(0);
                girar(-147);
                andar(100.0);

            }else{
                // sem tempo para a flor: estaciona direto do 2º tiro
                passoAtual = "Estacionando";
                shooter.setVelocity(0);
                andar(10.6);
                girar(87);
                andar(62.0);
            }

        }else{
            // sem tempo para o ciclo: estaciona direto depois do 1º tiro
            passoAtual = "Estacionando";
            shooter.setVelocity(0);
            andar(10.6);
            girar(88);
            andar(117.2);
        }

        pararTudo();
        telemetry.addLine("FIM do autonomo");
        telemetry.addData("Tempo (s)", "%.1f", getRuntime());
        telemetry.update();
    }

    void andar(double cm) {
        andarComPotencia(cm, 0.8);
    }

    // anda devagar (40%) - usado para coletar pollens
    void andarLento(double cm) {
        andarComPotencia(cm, 0.4);
    }

    void andarComPotencia(double cm, double potencia){
        if (emergencia()) return;

        int alvo = (int) Math.round(cm * TICKS_POR_CM);

        motorEsquerdo.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorDireito.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorEsquerdo.setTargetPosition(alvo);
        motorDireito.setTargetPosition(alvo);
        motorEsquerdo.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motorDireito.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motorEsquerdo.setPower(potencia);
        motorDireito.setPower(potencia);

        // tempo máximo, para o robô não ficar preso se algo travar
        double velocidadeCmPorS = 69.4 * (potencia / 0.8);
        double limiteSeg = Math.abs(cm) / velocidadeCmPorS + 1.5;
        double inicio = getRuntime();

        while (opModeIsActive() && !emergencia()
                && (motorEsquerdo.isBusy() || motorDireito.isBusy())
                && getRuntime() - inicio < limiteSeg) {
            telemetry.addData("Passo", passoAtual);
            telemetry.addData("Andando (cm)", "%.1f", cm);
            telemetry.addData("Encoder E / D", "%d / %d",
                    motorEsquerdo.getCurrentPosition(), motorDireito.getCurrentPosition());
            telemetry.addData("Angulo atual", "%.0f", anguloAtual());
            telemetry.addData("Tempo (s)", "%.1f", getRuntime());
            telemetry.update();
        }

        motorEsquerdo.setPower(0);
        motorDireito.setPower(0);
        motorEsquerdo.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motorDireito.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }

    // Gira até o ângulo 'graus' (absoluto, pelo IMU). 0 = direção da largada
    void girar(double graus){
        double inicio = getRuntime();

        while (opModeIsActive() && !emergencia() && getRuntime() - inicio < 2.5) {
            double erro = diferencaAngulo(graus - anguloAtual());
            if (Math.abs(erro) <= 2.0) break;           // chegou (tolerância de 2 graus)

            double p = Range.clip(0.02 * erro, -0.6, 0.6);
            if (Math.abs(p) < 0.15) p = Math.signum(p) * 0.15;   // força mínima p/ não travar

            motorEsquerdo.setPower(-p);
            motorDireito.setPower(p);

            telemetry.addData("Passo", passoAtual);
            telemetry.addData("Girando para", "%.0f", graus);
            telemetry.addData("Angulo atual", "%.0f", anguloAtual());
            telemetry.addData("Tempo (s)", "%.1f", getRuntime());
            telemetry.update();
        }
        motorEsquerdo.setPower(0);
        motorDireito.setPower(0);
    }

    // Liga o shooter, espera chegar na velocidade, empurra os pollens com o intake
    void lancar(double velocidade) {
        if (emergencia()) return;

        shooter.setVelocity(velocidade);

        double inicio = getRuntime();
        while (opModeIsActive() && !emergencia() && getRuntime() - inicio < 2.5
                && Math.abs(shooter.getVelocity() - velocidade) > 60) {
            telemetry.addData("Passo", passoAtual);
            telemetry.addData("Shooter", "%.0f / %.0f", shooter.getVelocity(), velocidade);
            telemetry.update();
        }

        intake.setPower(1.0);     // empurra os pollens para o shooter

        // shooter cai um pouco a cada pollen que sai. Conte as quedas para saber quantos saíram.
        double inicioAlimentar = getRuntime();
        while (opModeIsActive() && !emergencia() && getRuntime() - inicioAlimentar < TEMPO_LANCAR_S){
            telemetry.addData("Passo", passoAtual);
            telemetry.addData("Alimentando - shooter", "%.0f / %.0f", shooter.getVelocity(), velocidade);
            telemetry.update();
        }
        intake.setPower(0);
        // o shooter continua girando para o próximo tiro
    }

    // angulo atual do robô em graus (0 = direção da largada, esquerda = positivo)
    double anguloAtual(){
        return imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES);
    }

    // ajusta a diferença de ângulo para ficar entre -180 e 180
    double diferencaAngulo(double graus){
        while (graus > 180) graus-=360;
        while (graus < -180) graus+=360;
        return graus;
    }

    // botão de emergência: para tudo e encerra o autônomo
    boolean emergencia(){
        if (gamepad1.b || gamepad1.a){
            pararTudo();
            requestOpModeStop();
            return true;
        }
        return false;
    }

    void pararTudo(){
        motorEsquerdo.setPower(0);
        motorDireito.setPower(0);
        intake.setPower(0);
        shooter.setVelocity(0);
    }
}
