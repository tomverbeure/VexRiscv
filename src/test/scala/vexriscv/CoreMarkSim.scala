package coremark

import spinal.sim._
import spinal.core._
import spinal.core.sim._

import scala.collection.mutable

object CoreMarkSim {
  def main(args: Array[String]): Unit = {
    def config = CoreMarkCpuComplexConfig.fast

    val simSlowDown = false
    SimConfig.
//        withWave.
        allOptimisation.
        compile(new CoreMarkTop(config)).
        doSimUntilVoid{dut =>

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
