package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

/* stick esq = anda | stick dir = gira | LB = liga/desliga o modo lento
 X liga/desliga o shooter | DPAD CIMA = RPM 1 (4800) | DPAD BAIXO = RPM 2 (4000)
 A = abre a portinhola (se o shooter ainda nao estiver na RPM, fica aguardando e abre sozinha quando chegar)
    (as bolinhas ja estao no intake e vao pro shooter ao abrir) | A de novo = fecha
 RB + A = abre a portinhola mesmo sem a RPM certa
 Y liga/desliga o intake manual (o intake NAO liga sozinho ao abrir a portinhola!!)
 B = guarda-chuva: vai 120 graus, apertando de novo, volta */
@TeleOp(name = "Chassi 4 - TeleOp (RPM + Guarda-chuva)", group = "Treino")
public class Chassi4TeleOp extends LinearOpMode {
    private ElapsedTime runtime = new ElapsedTime();
    private ElapsedTime tempoPortinhola = new ElapsedTime();
    private DcMotor motorEsquerdo;
    private DcMotor motorDireito;
    private DcMotorEx motorShooter;
    private DcMotor motorIntake;
    private Servo servoPortinhola;
    private Servo servoGuardaChuva;

    // potencia
    static final double POTENCIA_MAX_ANDAR = 0.8;  
    static final double POTENCIA_INTAKE = 0.8;     
    static final double FATOR_LENTO = 0.5;        

    // shooter
    static final double TICKS_POR_VOLTA = 28;         
    static final double RPM_SHOOTER_1 = 4800;
    static final double RPM_SHOOTER_2 = 4000;        
    static final double RPM_TOLERANCIA = 150;      

    // portinhola
    static final double POS_PORTINHOLA_FECHADA = 0.0; 
    static final double POS_PORTINHOLA_ABERTA = 0.5;
    static final boolean INTAKE_AUTOMATICO_AO_ABRIR = false; 
    static final double ATRASO_INTAKE_SEG = 0.3;   

    // guardachuva
    static final double ALCANCE_SERVO_GRAUS = 180; 
    static final double ANGULO_GUARDACHUVA_GRAUS = 120;
    static final double POS_GC_RECOLHIDO = 0.0;
    static final double POS_GC_ABERTO = Range.clip(ANGULO_GUARDACHUVA_GRAUS / ALCANCE_SERVO_GRAUS, 0.0, 1.0);

    static final double DURACAO_TELEOP_SEG=120;

    @Override
    public void runOpMode() {
        motorEsquerdo = hardwareMap.get(DcMotor.class, "motoresquerdo");
        motorDireito = hardwareMap.get(DcMotor.class, "motordireito");
        motorShooter = hardwareMap.get(DcMotorEx.class, "motorshooter");
        motorIntake = hardwareMap.get(DcMotor.class, "motorintake");
        servoPortinhola = hardwareMap.get(Servo.class, "servoportinhola");
        servoGuardaChuva = hardwareMap.get(Servo.class, "servoguardachuva");

        motorEsquerdo.setDirection(DcMotor.Direction.REVERSE);
        motorDireito.setDirection(DcMotor.Direction.FORWARD);
        motorEsquerdo.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorDireito.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorShooter.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        motorIntake.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
      
        motorEsquerdo.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motorDireito.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motorIntake.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motorShooter.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorShooter.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // servos comecam nas posicoes de repouso
        servoPortinhola.setPosition(POS_PORTINHOLA_FECHADA);
        servoGuardaChuva.setPosition(POS_GC_RECOLHIDO);

        telemetry.addData("Status", "Pronto para iniciar");
        telemetry.update();
        waitForStart();
        runtime.reset();

        boolean shooterLigado = false;
        boolean botaoXAnterior = false;
        boolean intakeLigado = false;
        boolean botaoYAnterior = false;
        boolean guardaChuvaAberto = false;
        boolean botaoBAnterior = false;
        boolean portinholaAberta = false;
        boolean portinholaPedida = false;
        boolean botaoAAnterior = false;
        boolean modoLento = false;
        boolean botaoLBAnterior = false;
        double rpmAlvo = RPM_SHOOTER_1;
        int perfilRpm = 1;

        while (opModeIsActive()) {
            // locomocao
            if (gamepad1.left_bumper && !botaoLBAnterior){
                modoLento = !modoLento;
            }
            botaoLBAnterior = gamepad1.left_bumper;
            double fator = modoLento?FATOR_LENTO : 1.0;
            double andar = -gamepad1.left_stick_y;
            double girar = gamepad1.right_stick_x;
            double esquerda = andar+girar;
            double direita = andar-girar;
            double maior = Math.max(1.0, Math.max(Math.abs(esquerda), Math.abs(direita)));
            motorEsquerdo.setPower(esquerda/maior*POTENCIA_MAX_ANDAR*fator);
            motorDireito.setPower(direita/maior*POTENCIA_MAX_ANDAR*fator);

            // shooter
            if (gamepad1.dpad_up){
                rpmAlvo=RPM_SHOOTER_1;
                perfilRpm = 1;
            }
            if (gamepad1.dpad_down){
                rpmAlvo=RPM_SHOOTER_2;
                perfilRpm = 2;
            }
            if (gamepad1.x && !botaoXAnterior){
                shooterLigado = !shooterLigado;
            }
            botaoXAnterior = gamepad1.x;

            double ticksPorSegundoAlvo = shooterLigado ? rpmAlvo * TICKS_POR_VOLTA/60.0 : 0;
            motorShooter.setVelocity(ticksPorSegundoAlvo);
            double rpmReal = motorShooter.getVelocity() * 60.0 / TICKS_POR_VOLTA;
            boolean shooterPronto = shooterLigado && Math.abs(rpmReal - rpmAlvo) <= RPM_TOLERANCIA;

            // intakemanual
            if (gamepad1.y && !botaoYAnterior){
                intakeLigado = !intakeLigado;
            }
            botaoYAnterior = gamepad1.y;

            // portinhola
            if (gamepad1.a && !botaoAAnterior){
                portinholaPedida = !portinholaPedida;
            }
            botaoAAnterior = gamepad1.a;

            if (portinholaPedida && !portinholaAberta && (shooterPronto || gamepad1.right_bumper)){
                portinholaAberta = true;
                servoPortinhola.setPosition(POS_PORTINHOLA_ABERTA);
                tempoPortinhola.reset();
            }
            if (!portinholaPedida && portinholaAberta){
                portinholaAberta = false;
                servoPortinhola.setPosition(POS_PORTINHOLA_FECHADA);
            }

            // intake
            boolean intakeAutomatico=INTAKE_AUTOMATICO_AO_ABRIR && portinholaAberta
                    && tempoPortinhola.seconds() >= ATRASO_INTAKE_SEG;
            double potenciaIntake = (intakeLigado || intakeAutomatico) ? POTENCIA_INTAKE : 0;
            motorIntake.setPower(potenciaIntake);

            // guardachuva
            if(gamepad1.b && !botaoBAnterior){
                guardaChuvaAberto=!guardaChuvaAberto;
                servoGuardaChuva.setPosition(guardaChuvaAberto ? POS_GC_ABERTO : POS_GC_RECOLHIDO);
            }
            botaoBAnterior = gamepad1.b;

            // telemetria
            double restante = Math.max(DURACAO_TELEOP_SEG - runtime.seconds(), 0);
            telemetry.addData("Tempo restante", "%d:%02d", (int) restante / 60, (int) restante % 60);
            telemetry.addData("Shooter", "%s | perfil %d", shooterLigado ? "LIGADO" : "desligado", perfilRpm);
            telemetry.addData("RPM", "real %.0f | alvo %.0f | pronto: %b", rpmReal, rpmAlvo, shooterPronto);
            telemetry.addData("Portinhola", portinholaAberta ? "ABERTA" : (portinholaPedida ? "aguardando RPM..." : "fechada"));
            telemetry.addData("Intake", "%.1f", potenciaIntake);
            telemetry.addData("Guarda-chuva", guardaChuvaAberto ? "ABERTO (%.0f graus)" : "recolhido", ANGULO_GUARDACHUVA_GRAUS);
            telemetry.addData("Modo", fator < 1.0 ? "LENTO" : "normal");
            telemetry.update();
        }
        motorShooter.setVelocity(0);
        motorIntake.setPower(0);
        motorEsquerdo.setPower(0);
        motorDireito.setPower(0);
    }
}
