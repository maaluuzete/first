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
- A parte de cima (runOpMode) é só a lista de passos: andar, girar, lançar...
- A parte de baixo tem as funções andar(), girar() e lancar() que fazem o trabalho
- (encoder para a distância, IMU para o ângulo, telemetria o tempo todo).
- largando ENCOSTADO na parede, de frente para a hive, alinhado com a célula alta.
- Ângulos: 0 = de frente para a hive, positivo = esquerda, negativo = direita.
 */
@Autonomous(name = "Auto Simples", group = "Autonomo")
public class AutoSimples extends LinearOpMode{

    // hardware
    DcMotor motorEsquerdo;
    DcMotor motorDireito;
    DcMotor intake;
    DcMotorEx shooter;
    IMU imu;

    //ajustar chassi
    double TICKS_POR_VOLTA  = 336;
    double DIAMETRO_RODA_CM = 9.6;
    static final double FATOR_CORRECAO = 100.0/118.0; 

    double TICKS_POR_CM =((TICKS_POR_VOLTA)/(DIAMETRO_RODA_CM * Math.PI)) * FATOR_CORRECAO;

    //ajustar o shooter
    double RPM_TIRO_1 = 2500.0;   
    double RPM_TIRO_2 = 2500.0;   
    double RPM_TIRO_3 = 2500.0;   
    double TEMPO_LANCAR_S = 2.3; // tempo que o intake alimenta o shooter

    String passoAtual = "";     // aparece na telemetria

    @Override
    public void runOpMode(){
        motorEsquerdo = hardwareMap.get(DcMotor.class, "motoresquerdo");
        motorDireito = hardwareMap.get(DcMotor.class, "motordireito");
        intake = hardwareMap.get(DcMotor.class, "intake");
        shooter = hardwareMap.get(DcMotorEx.class, "shooter");
        imu = hardwareMap.get(IMU.class, "imu");

        motorEsquerdo.setDirection(DcMotorSimple.Direction.REVERSE);  
        motorDireito.setDirection(DcMotorSimple.Direction.FORWARD);
        intake.setDirection(DcMotorSimple.Direction.REVERSE);
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
        lancarRPM(RPM_TIRO_1);

        // só tenta o ciclo completo se ainda sobra tempo (senão vai direto estacionar)
        if (getRuntime()<6.5){

            // vai no jardim
            passoAtual = "Indo ao jardim";

          /* Primeira Ideia: andar um pouco pra frente antes de virar pra poder pegar as flores
          a parte negativa, é que ele teria que ir de novo pra direção da parede
            andar(20);          // sai da parede para poder girar
            girar(90);           // vira para o lado da flor da parede
            andar(72.4);          // desce passando longe da flor
          */
            girar(90);
            andar(50);
          // começa a andar lentinho
            passoAtual = "Coletando jardim";
            intake.setPower(1.0);
            andarLento(15);
            sleep(800);           /* espera os pollens entrarem, o objetivo é sleepar o menos possivel pra economizar tempo,
              mas garantir que entre tudo */
            intake.setPower(0);

            // corre pro outro lado e atira
            passoAtual = "Indo pro outro lado";
            andar(-30); // ré pra poder começar a ir pro lugar
            girar(0);
            andarLongoLento(240);       
            girar(-90);            // vira para o lado da hive
            andar(60);         // sobe até ficar de frente pra célula
            girar(180);

            passoAtual = "Tiro 2";
            lancarRPM(RPM_TIRO_2);
// Esses if são pra evitar que, caso não sobrar tempo, o robô vá direto estacionar
            if (getRuntime()<18){

                passoAtual = "Indo pra flor";
              // da a voltinha
                girar(0);
                andar(20);
                
                passoAtual = "Coletando flor";
              // rampa ativada (coisa futura), eu tenho que testar depois
                intake.setPower(1.0);
                sleep(800);
                intake.setPower(0);
                andar(-20); //rézinha
                
                passoAtual = "Tiro 3";
                girar(180);       // mira na célula
                lancarRPM(RPM_TIRO_3);

                // estacionar
                passoAtual = "Estacionando";
                shooter.setVelocity(0);
                girar(90);
                andar(100.0);

            }else{
                // sem tempo para a flor: estaciona direto do 2º tiro
                passoAtual = "Estacionando 2°tiro sem tempo";
                shooter.setVelocity(0);
                andar(20.0);
                girar(90);
                andar(100.0);
            }

        }else{
            // sem tempo para o ciclo: estaciona direto depois do 1º tiro
            passoAtual = "Estacionando 1° tiro sem tempo";
            shooter.setVelocity(0);
            girar(90);
            andar(117.2);
            girar(90);
            andarLongoLento(270);
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

    void andarLongoLento(double cm) {
        andarComPotencia(cm, 0.5);
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
        double limiteSeg = Math.abs(cm) / velocidadeCmPorS + 3.0; // margem maior por causa da baixa velocidade
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

        while (opModeIsActive() && !emergencia() && getRuntime() - inicio < 2.5){
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

    // Função que converte RPM para Ticks por Segundo considerando a redução 12:1 do shooter
    double rpmParaTicks(double rpm) {
        return (rpm*TICKS_POR_VOLTA) / (60.0 * 12.0);
    }

    // Liga o shooter em RPM real, espera chegar na velocidade, empurra os pollens com o intake
    void lancarRPM(double rpmDesejado) {
        if (emergencia()) return;

        double ticksPorSegundoAlvo = rpmParaTicks(rpmDesejado);
        shooter.setVelocity(ticksPorSegundoAlvo);

        double inicio = getRuntime();
        while (opModeIsActive() && !emergencia() && getRuntime() - inicio < 2.5
                && Math.abs(shooter.getVelocity() - ticksPorSegundoAlvo) > 20) {
            
            double rpmAtual = ((shooter.getVelocity() * 60.0) / TICKS_POR_VOLTA) * 12.0;
            telemetry.addData("Passo", passoAtual);
            telemetry.addData("RPM Atual / Alvo", "%.0f / %.0f", rpmAtual, rpmDesejado);
            telemetry.update();
        }

        intake.setPower(1.0);     // empurra os pollens para o shooter

        double inicioAlimentar = getRuntime();
        while (opModeIsActive() && !emergencia() && getRuntime() - inicioAlimentar < TEMPO_LANCAR_S){
            double rpmAtual = ((shooter.getVelocity() * 60.0) / TICKS_POR_VOLTA) * 12.0;
            telemetry.addData("Passo", passoAtual);
            telemetry.addData("Alimentando (RPM)", "%.0f / %.0f", rpmAtual, rpmDesejado);
            telemetry.update();
        }
        intake.setPower(0);
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
