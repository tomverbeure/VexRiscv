package coremark

import spinal.sim._
import spinal.core._
import spinal.core.sim._

import scala.collection.mutable

object CoreMarkSim {
  def main(args: Array[String]): Unit = {
    def config = CoreMarkCpuComplexConfig.default

    val simSlowDown = false
    SimConfig.
        withWave.
//        allOptimisation.
        compile(new CoreMarkTop(config)).
        doSimUntilVoid{dut =>

        val mainClkPeriod = (1e12/dut.config.coreFrequency.toDouble).toLong

        val clockDomain = ClockDomain(dut.io.mainClk, dut.io.asyncReset)
        clockDomain.forkStimulus(mainClkPeriod)
    }
  }
}
