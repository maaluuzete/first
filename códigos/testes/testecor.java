package org.firstinspires.ftc.teamcode;

import android.graphics.Color;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.NormalizedColorSensor;
import com.qualcomm.robotcore.hardware.NormalizedRGBA;
import com.qualcomm.robotcore.hardware.SwitchableLight;

@TeleOp(name = "Teste Sensor de Cor", group = "Teste")
public class TesteSensorCor extends LinearOpMode{

    NormalizedColorSensor sensorCor;
    final float SATURACAO_MINIMA = 0.45f; 
    final float VERMELHO_MAX = 25;    
    final float VERMELHO_MIN = 340;  
    final float AMARELO_MIN = 35;     
    final float AMARELO_MAX = 75;
    @Override
    public void runOpMode(){
        sensorCor=hardwareMap.get(NormalizedColorSensor.class, "sensor_cor");
        if (sensorCor instanceof SwitchableLight){
            ((SwitchableLight) sensorCor).enableLight(true);
        }

        sensorCor.setGain(2); 

        telemetry.addLine("Pronto! Aperte START");
        telemetry.update();
        waitForStart();

        float[] hsv =new float[3];

        while (opModeIsActive()){

            NormalizedRGBA cores = sensorCor.getNormalizedColors();
            Color.colorToHSV(cores.toColor(), hsv);

            float matiz=hsv[0];  
            float saturacao=hsv[1]; 

            String corDetectada;

            if(saturacao < SATURACAO_MINIMA){
                corDetectada = "NENHUMA";
            }else if (matiz <= VERMELHO_MAX || matiz>=VERMELHO_MIN){
                corDetectada = "VERMELHA";
            }else if(matiz >= AMARELO_MIN && matiz<=AMARELO_MAX){
                corDetectada = "AMARELA";
            }else{
                corDetectada = "NENHUMA";
            }

            telemetry.addData("Cor detectada", corDetectada);
            telemetry.addLine();
            telemetry.addData("Vermelho", "%.3f", cores.red);
            telemetry.addData("Verde", "%.3f", cores.green);
            telemetry.addData("Azul", "%.3f", cores.blue);
            telemetry.addData("Matiz (hue)", "%.1f", matiz);
            telemetry.addData("Saturação", "%.2f", saturacao);
            telemetry.update();
        }
    }
}
