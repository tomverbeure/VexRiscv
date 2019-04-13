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

        val spinalConfig = SpinalConfig(anonymSignalUniqueness = true)
        val rtl = spinalConfig.generateVerilog({
            new CoreMarkTop(config, synth = true).setDefinitionName(name)
        })

    }

    def runSim(config : CoreMarkCpuComplexConfig, name : String = "CoreMarkTop") = {

        SimConfig.
            //withWave.
            allOptimisation.
            compile(new CoreMarkTop(config, synth = false)).
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

        val synth = args.contains("--synth")

		val params = CoreMarkParameters().withArgs(args)
		val config = params.toCoreMarkCpuComplexConfig() 

		println(params.toShortStr())
		println(params.toLongStr)

        if (synth){
            runSynth(config, "CoreMarkTop")
        }
        else{
            runSim(config)
        }

    }

}
