package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

@TeleOp(name = "Teste Tempo Trajeto", group = "Teste")
public class TesteTempoTrajeto extends LinearOpMode {

    // hardware
    DcMotor motorEsquerdo;
    DcMotor motorDireito;
    DcMotor intake;
    DcMotorEx shooter;
    IMU imu;

    double TICKS_POR_VOLTA  = 537.7;
    double DIAMETRO_RODA_CM = 9.6;
    double TICKS_POR_CM = TICKS_POR_VOLTA / (DIAMETRO_RODA_CM * Math.PI);

    //ajustar o shooter
    double VEL_TIRO_1 = 1500;   // largada
    double VEL_TIRO_2 = 1500;   // outro lado da arena (mesma distância)
    double VEL_TIRO_3 = 1450;   // depois da flor (um pouco mais perto)
    double TEMPO_LANCAR_S = 1.0; // quanto tempo o intake alimenta o shooter (chute por enquanto)

    String passoAtual = "";     // aparece na telemetria

    // tempos (cumulativos, em segundos desde o play)
    double ultimoMarco = 0;
    double tTiro1, tJardim, tOutroLado, tTiro2, tFlor, tTiro3, tTotal;

    @Override
    public void runOpMode(){
        motorEsquerdo =hardwareMap.get(DcMotor.class, "motoresquerdo");
        motorDireito=hardwareMap.get(DcMotor.class, "motordireito");
        intake=hardwareMap.get(DcMotor.class, "intake");
        shooter=hardwareMap.get(DcMotorEx.class, "shooter");
        imu=hardwareMap.get(IMU.class, "imu");

        motorEsquerdo.setDirection(DcMotorSimple.Direction.REVERSE);  // se andar pra trás, inverta os dois
        motorDireito.setDirection(DcMotorSimple.Direction.FORWARD);
        intake.setDirection(DcMotorSimple.Direction.REVERSE);
        motorEsquerdo.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorDireito.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        shooter.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // o andar termina mais rapido (15 ticks = ~0,8 cm de tolerancia). Se passar do ponto, volte para 10 ou 5
        ((DcMotorEx) motorEsquerdo).setTargetPositionTolerance(15);
        ((DcMotorEx) motorDireito).setTargetPositionTolerance(15);

        // hub no robo
        imu.initialize(new IMU.Parameters(new RevHubOrientationOnRobot(
                RevHubOrientationOnRobot.LogoFacingDirection.FORWARD,
                RevHubOrientationOnRobot.UsbFacingDirection.DOWN)));

        telemetry.log().setCapacity(12);   // o log mostra os tempos de cada etapa
        telemetry.addLine("TESTE DE TEMPO - trajeto completo");
        telemetry.addLine("Largue ENCOSTADO na parede, de frente pra hive.");
        telemetry.addLine("EMERGENCIA: B ou A no gamepad 1");
        telemetry.update();

        waitForStart();
        resetRuntime();     // zera o cronômetro do teste
        imu.resetYaw();     // o ângulo 0 é a direção da largada

        // primeiro tiro
        passoAtual = "Tiro 1";
        lancar(VEL_TIRO_1);
        tTiro1 = marcar("Tiro 1 acabou");

        // vai no jardim
        passoAtual = "Indo ao jardim";
        andar(20);          // sai da parede para poder girar
        girar(90);           // vira para o lado da flor da parede
        andar(72.4);          // desce passando longe da flor

        passoAtual = "Coletando jardim";
        intake.setPower(1.0);
        andarLento(15.5);
        // sleep(800);           // espera os pollens entrarem (agora o intake continua puxando durante o giro)

        // corre pro outro lado e atira
        passoAtual = "Indo pro outro lado";
        girar(0);             // o intake continua ligado durante o giro
        intake.setPower(0);
        tJardim = marcar("Jardim coletado");
        andar(270);
        girar(-90);            // vira para o lado da loading zone
        andar(120);         // sobe até ficar de frente pra célula
        girar(180);
        tOutroLado = marcar("Chegou no outro lado");

        passoAtual = "Tiro 2";
        lancar(VEL_TIRO_2);
        tTiro2 = marcar("Tiro 2 acabou");

        //ir na flor do outro lado
        passoAtual = "Indo pra flor";
        girar(180);
        andar(50);

        passoAtual = "Coletando flor";
        intake.setPower(1.0);
        sleep(800);
        intake.setPower(0);
        tFlor = marcar("Flor coletada");

        passoAtual = "Tiro 3";
        girar(180);       // mira na célula
        lancar(VEL_TIRO_3);
        tTiro3 = marcar("Tiro 3 acabou");

        // estacionar
        passoAtual = "Estacionando";
        shooter.setVelocity(0);
        girar(90, 6);     // tolerancia maior: no estacionamento uns graus a mais nao importam
        andar(80.0);
        tTotal = marcar("Estacionou (TOTAL)");

        pararTudo();

        // resultado fica na tela ate apertar STOP
        while (opModeIsActive()){
            telemetry.addLine("=== TEMPOS (s desde o play) ===");
            telemetry.addData("Tiro 1 acabou", "%.1f", tTiro1);
            telemetry.addData("Jardim coletado", "%.1f", tJardim);
            telemetry.addData("Chegou no outro lado", "%.1f", tOutroLado);
            telemetry.addData("Tiro 2 acabou", "%.1f", tTiro2);
            telemetry.addData("Flor coletada", "%.1f", tFlor);
            telemetry.addData("Tiro 3 acabou", "%.1f", tTiro3);
            telemetry.addData("TOTAL", "%.1f", tTotal);
            telemetry.addData("Cabe nos 30 s?", tTotal <= 29.5 ? "SIM" : "NAO (passou " + String.format("%.1f", tTotal - 29.5) + " s)");
            telemetry.addLine("=== LIMITES SUGERIDOS PRO getRuntime() ===");
            telemetry.addData("Ciclo completo (hoje 6.5)", "%.1f", 29.5 - (tTotal - tTiro1) - 1.5);
            telemetry.addData("Flor (hoje 18)", "%.1f", 29.5 - (tTotal - tTiro2) - 1.5);
            telemetry.update();
            idle();
        }
    }

    // anota o tempo e mostra no log: tempo total e quanto durou a etapa
    double marcar(String nome){
        double agora = getRuntime();
        telemetry.log().add(nome + ": " + String.format("%.1f", agora) + " s (etapa: " + String.format("%.1f", agora - ultimoMarco) + " s)");
        ultimoMarco = agora;
        return agora;
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
        girar(graus, 2.0);      // tolerância normal de 2 graus
    }

    // tolerancia maior = o giro termina mais rapido
    void girar(double graus, double tolerancia){
        double inicio = getRuntime();

        while (opModeIsActive() && !emergencia() && getRuntime() - inicio < 2.5){
            double agora = anguloAtual();                    // le o IMU uma vez so por volta
            double erro = diferencaAngulo(graus - agora);
            if (Math.abs(erro) <= tolerancia) break;         // chegou

            double p = Range.clip(0.02 * erro, -0.6, 0.6);
            if (Math.abs(p) < 0.2) p = Math.signum(p) * 0.2;   // força mínima p/ não travar

            motorEsquerdo.setPower(-p);
            motorDireito.setPower(p);

            telemetry.addData("Passo", passoAtual);
            telemetry.addData("Girando para", "%.0f", graus);
            telemetry.addData("Angulo atual", "%.0f", agora);
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

    // botão de emergência: para tudo e encerra o teste
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
