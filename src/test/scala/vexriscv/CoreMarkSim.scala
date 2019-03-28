package coremark

import spinal.sim._
import spinal.core._
import spinal.core.sim._

import scala.collection.mutable
import vexriscv.plugin._

object CoreMarkSim {

    def main(args: Array[String]): Unit = {

        val configOptions = CoreMarkCpuComplexConfig.configOptions

        printf("%s: %d, %d\n", configOptions(0)._1,configOptions(0)._2,configOptions(0)._3)

        def config = CoreMarkCpuComplexConfig.fast

        config.cpuPlugins(config.cpuPlugins.indexWhere(_.isInstanceOf[LightShifterPlugin])) = new FullBarrelShifterPlugin()

        val simSlowDown = false
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

            while(!done){
                clockDomain.waitSampling()

                if (dut.io.apb.PENABLE.toBoolean){
                    if (dut.io.apb.PADDR.toLong == 0x0){
                        printf("%c", (dut.io.apb.PWDATA.toLong & 0xff).toChar)
                    }
                    else{
                        done = true
                    }
                }
            }

            printf("Done!\n")
            simSuccess()
        }
    }
}
