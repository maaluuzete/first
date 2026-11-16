package org.firstinspires.ftc.teamcode;

import android.graphics.Color;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name = "Detector de Cor", group = "Sensor")
public class DetectorDeCor extends LinearOpMode{

    final int   LUZ_MINIMA = 50;    
    final float SATURACAO_MINIMA = 0.35f; 

    final float VERMELHO_ATE = 20;      
    final float VERMELHO_DESDE = 340;
    final float AMARELO_DE = 35;     
    final float AMARELO_ATE = 75;
    final float AZUL_DE = 190;   
    final float AZUL_ATE = 260;

    @Override
    public void runOpMode(){

        TCS3472Driver sensor = hardwareMap.get(TCS3472Driver.class, "sensor_cor");
        sensor.setGanho(1);

        telemetry.addLine("Pronto! Aperte START");
        telemetry.update();
        waitForStart();

        float[] hsv = new float[3];

        while (opModeIsActive()){
            int[] c = sensor.lerCores();
            int clear = c[0];
            int red = c[1];
            int green = c[2];
            int blue = c[3];

            // Calcula matiz e saturação
            int maior=Math.max(red, Math.max(green, blue));
            if (maior > 0){
                Color.RGBToHSV(red*255 / maior, green*255 / maior, blue*255 / maior, hsv);
            }else{
                hsv[0] = 0;
                hsv[1] = 0;
            }

            String cor = identificarCor(hsv[0], hsv[1], clear);
            
            // if (cor.equals("AMARELO")){
            //     // ligar motor ou mover servo aqui
            // }else if (cor.equals("VERMELHO")){
            //     // outra ação aqui
            // }

            // telemetria
            telemetry.addData("COR", cor);
            telemetry.addLine();
            telemetry.addData("Matiz", "%.0f graus", hsv[0]);
            telemetry.addData("Saturação", "%.0f%%", hsv[1] * 100);
            telemetry.addData("Luz total", clear);
            telemetry.addData("R / G / B", "%d / %d / %d", red, green, blue);
            telemetry.update();
        }
    }

    /** Decide qual é a cor a partir do matiz, saturação e luz total. */
    String identificarCor(float matiz, float saturacao, int clear) {

        if (clear < LUZ_MINIMA){
            return "NENHUMA";
        }
        if (saturacao < SATURACAO_MINIMA){
            return "INDEFINIDA";
        }
        if (matiz <= VERMELHO_ATE || matiz >= VERMELHO_DESDE){
            return "VERMELHO";
        }
        if (matiz >= AMARELO_DE && matiz <= AMARELO_ATE){
            return "AMARELO";
        }
        if (matiz >= AZUL_DE && matiz <= AZUL_ATE){
            return "AZUL";
        }
        return "INDEFINIDA";
    }
}
