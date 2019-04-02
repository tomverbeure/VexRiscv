package coremark

import spinal.sim._
import spinal.core._
import spinal.core.sim._
import spinal.lib.eda.altera.QuartusFlow

import java.io.File
import org.apache.commons.io.FileUtils
import scala.collection.mutable._

import vexriscv.plugin._

object CoreMarkSim {

    def runSynth(config : CoreMarkCpuComplexConfig, name : String = "CoreMarkTop") = {

        val rtl = SpinalVerilog({
            new CoreMarkTop(config).setDefinitionName(name)
        })

//        FileUtils.copyFileToDirectory(new File(name + ".v_toplevel_system_cpuComplex_ram_ram_symbol0.bin"), new File("quartus"))
//        FileUtils.copyFileToDirectory(new File(name + ".v_toplevel_system_cpuComplex_ram_ram_symbol1.bin"), new File("quartus"))
//        FileUtils.copyFileToDirectory(new File(name + ".v_toplevel_system_cpuComplex_ram_ram_symbol2.bin"), new File("quartus"))
//        FileUtils.copyFileToDirectory(new File(name + ".v_toplevel_system_cpuComplex_ram_ram_symbol3.bin"), new File("quartus"))

        QuartusFlow(
            quartusPath = "/home/tom/altera/13.0sp1/quartus/bin/",
            workspacePath = "quartus",
            toplevelPath = name + ".v",
            family = "Cyclone II",
            device ="EP2C35F672C6"
        )
    }

    def runSim(config : CoreMarkCpuComplexConfig, name : String = "CoreMarkTop") = {

        SimConfig.
            //withWave.
            allOptimisation.
            compile(new CoreMarkTop(config)).
            doSimUntilVoid
        {dut =>

            val mainClkPeriod = (1e12/dut.config.coreFrequency.toDouble).toLong

            val clockDomain = ClockDomain(dut.io.mainClk, dut.io.asyncReset)
            clockDomain.forkStimulus(mainClkPeriod)

            dut.io.apb.PREADY #= true
            clockDomain.waitSampling()

            var done = false
            var ticks = -1;

            while(!done){
                clockDomain.waitSampling()

                if (dut.io.apb.PENABLE.toBoolean){
                    dut.io.apb.PADDR.toLong match {
                        case 0x0 => printf("%c", (dut.io.apb.PWDATA.toLong & 0xff).toChar)
                        case 0x4 => done = true
                        case 0x8 => ticks = dut.io.apb.PWDATA.toLong.toInt
                    }
                }
            }

            printf("Duration: %d\n", ticks);
            printf("Done!\n")
            simSuccess()
        }

    }

    def main(args: Array[String]): Unit = {

        var validConfigIds = new ArrayBuffer[Long]()

        for(configId <- 0 to 0){
            var config = CoreMarkCpuComplexConfig.constructConfig(configId)
            if (config != null){
                validConfigIds += configId

                if (true){
                    val shortConfigStr = CoreMarkCpuComplexConfig.shortConfigStr(configId)
                    runSynth(config, "CoreMarkTop_" + shortConfigStr)
                }
                else{
                    runSim(config)
                }
            }
        }
    }
}
