package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

// stick esq = anda | stick dir = gira | LB segurado = modo lento (precisao)
// X liga/desliga shooter | Y liga/desliga intake | segura A = abre a portinhola e o intake empurra | solta = fecha
// Se USAR_CHECAGEM_RPM = true, o A so abre com o shooter na RPM certa (RB + A ignora a checagem)
@TeleOp(name = "Chassi 3 - TeleOp Simples", group = "Treino")
public class Chassi3TeleOpSimples extends LinearOpMode {
    private ElapsedTime runtime=new ElapsedTime();
    private ElapsedTime tempoPortinhola=new ElapsedTime();
    private DcMotor motorEsquerdo;
    private DcMotor motorDireito;
    private DcMotorEx motorShooter;
    private DcMotor motorIntake;
    private CRServo servoPortinhola1;
    private CRServo servoPortinhola2;
    private CRServo servoPortinhola3;

    static final boolean USAR_CHECAGEM_RPM = false;   // ligar depois de calibrar RPM_ALVO e a reducao
    static final double TICKS_POR_ROTACAO = 28.0;
    static final double REDUCAO_SHOOTER = 1.0;        // mesma do autonomo
    static final double RPM_ALVO_SHOOTER = 4000;
    static final double RPM_TOLERANCIA = 150;
    static final double POTENCIA_SHOOTER = 0.8;
    static final double POTENCIA_INTAKE = 1.0;
    static final double FATOR_LENTO = 0.5;
    static final double TEMPO_ABRIR_SEG = 0.4;
    static final double TEMPO_FECHAR_SEG = 0.4;
    static final double DURACAO_TELEOP_SEG = 130;     // 2 minutos e 10 segundos

    static final int FECHADA = 0;
    static final int ABRINDO = 1;
    static final int ABERTA = 2;
    static final int FECHANDO = 3;

    @Override
    public void runOpMode(){
        motorEsquerdo = hardwareMap.get(DcMotor.class, "motoresquerdo");
        motorDireito = hardwareMap.get(DcMotor.class, "motordireito");
        motorShooter = hardwareMap.get(DcMotorEx.class, "motorshooter");
        motorIntake = hardwareMap.get(DcMotor.class, "motorintake");
        servoPortinhola1 = hardwareMap.get(CRServo.class, "servoportinhola1");
        servoPortinhola2 = hardwareMap.get(CRServo.class, "servoportinhola2");
        servoPortinhola3 = hardwareMap.get(CRServo.class, "servoportinhola3");

        motorEsquerdo.setDirection(DcMotor.Direction.REVERSE);
        motorDireito.setDirection(DcMotor.Direction.FORWARD);
        motorEsquerdo.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorDireito.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorShooter.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        motorIntake.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        motorShooter.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorShooter.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorIntake.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        telemetry.addData("Status", "Pronto para iniciar");
        telemetry.update();
        waitForStart();
        runtime.reset();

        boolean shooterLigado = false;
        boolean botaoXAnterior = false;
        boolean intakeLigado = false;
        boolean botaoYAnterior = false;
        int estadoPortinhola = FECHADA;

        while (opModeIsActive()){
            // andar
            double fator = gamepad1.left_bumper ? FATOR_LENTO : 1.0;
            double andar = -gamepad1.left_stick_y*fator;
            double girar = gamepad1.right_stick_x*fator;
            motorEsquerdo.setPower(Range.clip(andar+girar, -1.0, 1.0));
            motorDireito.setPower(Range.clip(andar-girar, -1.0, 1.0));

            // shooter (x)
            if (gamepad1.x && !botaoXAnterior){
                shooterLigado=!shooterLigado;
            }
            botaoXAnterior=gamepad1.x;
            motorShooter.setPower(shooterLigado ? POTENCIA_SHOOTER : 0);
            double rpm = motorShooter.getVelocity()/(TICKS_POR_ROTACAO*REDUCAO_SHOOTER)*60.0;
            boolean shooterPronto = shooterLigado && Math.abs(rpm-RPM_ALVO_SHOOTER)<=RPM_TOLERANCIA;

            // intake (y)
            if (gamepad1.y && !botaoYAnterior){
                intakeLigado = !intakeLigado;
            }
            botaoYAnterior = gamepad1.y;

            // portinhola (a)
            boolean podeAtirar= !USAR_CHEGAGEM_RPM || shooterPronto || gamepad1.right_bumper;
            if (estadoPortinhola==FECHADA && gamepad1.a && podeAtirar){
                estadoPortinhola = ABRINDO;
                tempoPortinhola.reset();
                setPotenciaServos(1.0);
            }
            if (estadoPortinhola == ABRINDO && tempoPortinhola.seconds() >= TEMPO_ABRIR_SEG){
                estadoPortinhola = ABERTA;
                setPotenciaServos(0);
            }
            if (estadoPortinhola == ABERTA && !gamepad1.a){
                estadoPortinhola = FECHANDO;
                tempoPortinhola.reset();
                setPotenciaServos(-1.0);
            }
            if (estadoPortinhola == FECHANDO && tempoPortinhola.seconds() >= TEMPO_FECHAR_SEG){
                estadoPortinhola = FECHADA;
                setPotenciaServos(0);
            }
  
            // intake funciona com y ligado ou porta aberta
            double potenciaIntake = (intakeLigado || estadoPortinhola == ABERTA)?POTENCIA_INTAKE : 0;
            motorIntake.setPower(potenciaIntake);
            // telemetria

            double restante = DURACAO_TELEOP_SEG - runtime.seconds();
            String marco = restante <= 20 ? "FINAL (20 s)" : (restante <= 60 ? "FLOWER liberada (passou 1:00)" : "");
            telemetry.addData("Tempo restante", "%d:%02d %s", (int) Math.max(restante, 0)/60, (int) Math.max(restante, 0) % 60, marco);
            telemetry.addData("Shooter", "%s | %.0f rpm (alvo %.0f) pronto: %b", shooterLigado ? "LIGADO" : "desligado", rpm, RPM_ALVO_SHOOTER, shooterPronto);
            telemetry.addData("Intake", "%.1f", potenciaIntake);
            telemetry.addData("Portinhola", "estado %d", estadoPortinhola);
            telemetry.addData("Modo", fator < 1.0 ? "LENTO" : "normal");
            telemetry.update();
      
        }
        motorShooter.setPower(0);
        motorIntake.setPower(0);
        setPotenciaServos(0);
    }

    private void setPotenciaServos(double potencia) {
        servoPortinhola1.setPower(potencia);
        servoPortinhola2.setPower(potencia);
        servoPortinhola3.setPower(potencia);
    }
}
