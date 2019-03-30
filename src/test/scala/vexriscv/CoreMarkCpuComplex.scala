package coremark

import spinal.core._
import spinal.lib._
import spinal.lib.misc.HexTools
import spinal.lib.bus.amba3.apb._
import spinal.lib.bus.misc.SizeMapping
import spinal.lib.bus.simple._

import scala.collection.mutable.ArrayBuffer
import vexriscv.plugin.{NONE, _}
import vexriscv.{VexRiscv, VexRiscvConfig, plugin}
import vexriscv.demo._

case class CoreMarkCpuComplexConfig(
                  onChipRamHexFile  : String,
                  coreFrequency     : HertzNumber,
                  mergeIBusDBus     : Boolean,
                  iBusLatency       : Int,
                  dBusLatency       : Int,
                  apb3Config        : Apb3Config,
                  cpuPlugins        : ArrayBuffer[Plugin[VexRiscv]])
{
    require(iBusLatency >=1, "iBusLatency must be >= 1")
    require(dBusLatency >=1, "dBusLatency must be >= 1")
}

object CoreMarkCpuComplexConfig{

    val ucycleCsrConfig =  CsrPluginConfig(
        catchIllegalAccess  = false,
        mvendorid           = null,
        marchid             = null,
        mimpid              = null,
        mhartid             = null,
        misaExtensionsInit  = 66,
        misaAccess          = CsrAccess.NONE,
        mtvecAccess         = CsrAccess.NONE,
        mtvecInit           = 0x00000020l,
        mepcAccess          = CsrAccess.NONE,
        mscratchGen         = false,
        mcauseAccess        = CsrAccess.READ_ONLY,
        mbadaddrAccess      = CsrAccess.NONE,
        mcycleAccess        = CsrAccess.NONE,
        minstretAccess      = CsrAccess.NONE,
        ecallGen            = false,
        wfiGenAsWait        = false,
        ucycleAccess        = CsrAccess.READ_ONLY
    )

    object MultiplyOption extends Enumeration {
        val None            = Value(0)
        val Iterative       = Value(1)
        val Simple          = Value(2)
    }

    object DivideOption extends Enumeration {
        val None            = Value(0)
        val Iterative       = Value(1)
        val IterativeDhry   = Value(2)
    }

    object ShifterOption extends Enumeration {
        val Iterative       = Value(0)
        val BarrelExe       = Value(1)
        val BarrelMem       = Value(2)
    }

    object PredictionOption extends Enumeration {
        val None            = Value(0)
        val Static          = Value(1)
        val Dynamic         = Value(2)
        val DynamicTarget   = Value(3)
    }

    val configOptions = Array(
        // Option,                  starting bit,   nr of bits, max option
        ("BypassExecute",           0,              1,          1),
        ("BypassMemory",            1,              1,          1),
        ("BypassWriteBack",         2,              1,          1),
        ("BypassWriteBackBuffer",   3,              1,          1),
        ("Shifter",                 4,              2,          ShifterOption.values.size-1),
        ("BranchPredicition",       6,              2,          3),
        ("Multiply",                8,              2,          MultiplyOption.values.size-1),
        ("Prediction",             10,              2,          PredictionOption.values.size-1),
        ("Compressed",             11,              1,          1)
    )

    def constructConfig(configId : Long) : CoreMarkCpuComplexConfig = {

        var bypassExecute             = false
        var bypassMemory              = false
        var bypassWriteBack           = false
        var bypassWriteBackBuffer     = false
        var barrelShifter             = false
        var multiply                  = MultiplyOption.None
        var divide                    = DivideOption.None
        var shifter                   = ShifterOption.Iterative
        var prediction                = PredictionOption.None
        var compressed                = false

        for(option <- configOptions){
            val optionVal = ((configId >> option._2) & ((1<<(option._3))-1)).toInt

            if (optionVal > option._3){
                printf("Option %s out of bounds (%d)!", option._1, optionVal)
                System.exit(-1)
            }

            option._1 match {
                case "BypassExecute"          => bypassExecute          = (optionVal == 1)
                case "BypassMemory"           => bypassMemory           = (optionVal == 1)
                case "BypassWriteBack"        => bypassWriteBack        = (optionVal == 1)
                case "BypassWriteBackBuffer"  => bypassWriteBackBuffer  = (optionVal == 1)
                case "Multiply"               => multiply               = MultiplyOption(optionVal)
                case "Shifter"                => shifter                = ShifterOption(optionVal)
                case "Prediction"             => prediction             = PredictionOption(optionVal)
                case "Compressed"             => compressed             = (optionVal == 1)
                case _                        =>
            }

        }

        //println(multiply.toString)

        val config = CoreMarkCpuComplexConfig(
            onChipRamHexFile = "src/test/cpp/coremark/coremark_O2_rv32i.hex",
            coreFrequency = 100 MHz,
            mergeIBusDBus = false,
            iBusLatency = 1,
            dBusLatency = 1,
            apb3Config = Apb3Config(
                addressWidth = 20,
                dataWidth = 32
            ),
            cpuPlugins = ArrayBuffer(
                new IBusSimplePlugin(
                    resetVector = 0x00000000l,
                    cmdForkOnSecondStage    = true,
                    cmdForkPersistence      = false,
                    prediction              = prediction match {
                                                case PredictionOption.None          => NONE
                                                case PredictionOption.Static        => STATIC
                                                case PredictionOption.Dynamic       => DYNAMIC
                                                case PredictionOption.DynamicTarget => DYNAMIC_TARGET
                                              },
                    catchAccessFault        = false,
                    compressedGen           = compressed
                ),
                new DBusSimplePlugin(
                    catchAddressMisaligned  = false,
                    catchAccessFault        = false,
                    earlyInjection          = false
                ),
                new DecoderSimplePlugin(
                    catchIllegalInstruction = false
                ),
                new RegFilePlugin(
                    regFileReadyKind        = plugin.SYNC,
                    zeroBoot                = false
                ),
                new IntAluPlugin,
                new SrcPlugin(
                    separatedAddSub         = false,
                    executeInsertion        = false
                ),
                shifter match {
                    case ShifterOption.Iterative    => new LightShifterPlugin()
                    case ShifterOption.BarrelExe    => new FullBarrelShifterPlugin(earlyInjection = true)
                    case ShifterOption.BarrelMem    => new FullBarrelShifterPlugin(earlyInjection = false)
                },
                multiply match {
                    case MultiplyOption.None        => null
                    case MultiplyOption.Iterative   => new MulDivIterativePlugin(genMul = true, genDiv = false, mulUnrollFactor = 1)
                    case MultiplyOption.Simple      => new MulSimplePlugin()
                },
                divide match {
                    case DivideOption.None          => null
                    case DivideOption.Iterative     => new DivPlugin()
                    case DivideOption.IterativeDhry => new MulDivIterativePlugin(genMul = false, genDiv = true, divUnrollFactor = 1, dhrystoneOpt = true)
                },
                new HazardSimplePlugin(
                    bypassExecute           = bypassExecute,
                    bypassMemory            = bypassMemory,
                    bypassWriteBack         = bypassWriteBack,
                    bypassWriteBackBuffer   = bypassWriteBackBuffer
                ),
                new BranchPlugin(
                    earlyBranch             = false,
                    catchAddressMisaligned  = false
                ),
                new CsrPlugin(ucycleCsrConfig),
                new YamlPlugin("cpu0.yaml")
            ).filter(_ != null)
        )

        config
    }


    def default = CoreMarkCpuComplexConfig(
        onChipRamHexFile = "src/test/cpp/coremark/coremark_O2_rv32i.hex",
        coreFrequency = 100 MHz,
        mergeIBusDBus = false,
        iBusLatency = 1,
        dBusLatency = 1,
        apb3Config = Apb3Config(
            addressWidth = 20,
            dataWidth = 32
        ),
        cpuPlugins = ArrayBuffer(
            new IBusSimplePlugin(
                resetVector = 0x00000000l,
                cmdForkOnSecondStage    = true,
                cmdForkPersistence      = false,
                prediction              = NONE,
                catchAccessFault        = false,
                compressedGen           = false
            ),
            new DBusSimplePlugin(
                catchAddressMisaligned  = false,
                catchAccessFault        = false,
                earlyInjection          = false
            ),
            new DecoderSimplePlugin(
                catchIllegalInstruction = false
            ),
            new RegFilePlugin(
                regFileReadyKind        = plugin.SYNC,
                zeroBoot                = false
            ),
            new IntAluPlugin,
            new SrcPlugin(
                separatedAddSub         = false,
                executeInsertion        = false
            ),
            new LightShifterPlugin,
            new MulSimplePlugin,
            new DivPlugin,
            new HazardSimplePlugin(
                bypassExecute           = false,
                bypassMemory            = false,
                bypassWriteBack         = false,
                bypassWriteBackBuffer   = false
            ),
            new BranchPlugin(
                earlyBranch             = false,
                catchAddressMisaligned  = false
            ),
            new CsrPlugin(ucycleCsrConfig),
            new YamlPlugin("cpu0.yaml")
        )
    )

  def fast = {
      val config = default

      // Replace HazardSimplePlugin to get datapath bypass
      config.cpuPlugins(config.cpuPlugins.indexWhere(_.isInstanceOf[HazardSimplePlugin])) = new HazardSimplePlugin(
          bypassExecute           = true,
          bypassMemory            = true,
          bypassWriteBack         = true,
          bypassWriteBackBuffer   = true
    )
//    config.cpuPlugins(config.cpuPlugins.indexWhere(_.isInstanceOf[LightShifterPlugin])) = new FullBarrelShifterPlugin()

    config
  }
}

case class CoreMarkCpuComplex(config : CoreMarkCpuComplexConfig) extends Component
{
    import config._

    val io = new Bundle {
        val apb     = master(Apb3(config.apb3Config))
    }

    val pipelinedMemoryBusConfig = PipelinedMemoryBusConfig(
        addressWidth = 32,
        dataWidth = 32
    )

    val cpu = new VexRiscv(
        config = VexRiscvConfig(
            plugins = cpuPlugins
        )
    )

    val pipelineDBus    = true
    val pipelineMainBus = false

    // The CPU has 2 busses: iBus and dBus.
    // When mergeIBusDBus is true, when those 2 busses get merged first into the mainBus through the
    // MasterArbiter, which then goes to everything as the mainBus.
    // When mergIBusDBus is false, the iBus goes directly busB of the RAM and nothing else. Mainwhile dBus
    // get connected directly to mainBus, thus no MasterArbiter is needed. The RAM needs to be double-ported,
    // but that's often the case of FPGAs anyway, or a good approximation for systems that have individual
    // I-cache and D-cache.

    val mainBusArbiter = CoreMarkMasterArbiter(pipelinedMemoryBusConfig)

    val onChipRamSize = 64 KiB

    val ram = new CoreMarkPipelinedMemoryBusRam(
        dualBus                   = !mergeIBusDBus,
        onChipRamSize             = onChipRamSize,
        onChipRamHexFile          = onChipRamHexFile,
        pipelinedMemoryBusConfig  = pipelinedMemoryBusConfig
    )

    // Checkout plugins used to instanciate the CPU to connect them to the SoC
    for(plugin <- cpu.plugins) plugin match{
        case plugin : IBusSimplePlugin =>
            if (mergeIBusDBus)
                mainBusArbiter.io.iBus <> plugin.iBus
            else {
                mainBusArbiter.io.iBus.cmd.valid    := False
                mainBusArbiter.io.iBus.cmd.pc       := 0

                ram.io.busB <> plugin.iBus.toPipelinedMemoryBus()
            }

        case plugin : DBusSimplePlugin => {
            if(!pipelineDBus)
                mainBusArbiter.io.dBus <> plugin.dBus
            else {
                mainBusArbiter.io.dBus.cmd << plugin.dBus.cmd.halfPipe()
                mainBusArbiter.io.dBus.rsp <> plugin.dBus.rsp
            }
        }
        case plugin : CsrPlugin        => {
            plugin.externalInterrupt    := False
            plugin.timerInterrupt       := False
        }
        case _ =>
    }

    val mainBusMapping = ArrayBuffer[(PipelinedMemoryBus,SizeMapping)]()

    mainBusMapping += ram.io.busA -> (0x00000000l, onChipRamSize)

    val apbBridge = new PipelinedMemoryBusToApbBridge(
        apb3Config = Apb3Config(
            addressWidth = 20,
            dataWidth = 32
        ),
        pipelineBridge            = true,
        pipelinedMemoryBusConfig  = pipelinedMemoryBusConfig
    )

    mainBusMapping += apbBridge.io.pipelinedMemoryBus -> (0x80000000l, 1 MB)

    io.apb <> apbBridge.io.apb

    val mainBusDecoder = new Area {
        val logic = new CoreMarkPipelinedMemoryBusDecoder(
            master          = mainBusArbiter.io.masterBus,
            specification   = mainBusMapping,
            pipelineMaster  = pipelineMainBus
        )
    }

}

case class CoreMarkMasterArbiter(
                  pipelinedMemoryBusConfig  : PipelinedMemoryBusConfig) extends Component
{
    val io = new Bundle{
        val iBus        = slave(IBusSimpleBus(false))
        val dBus        = slave(DBusSimpleBus())
        val masterBus   = master(PipelinedMemoryBus(pipelinedMemoryBusConfig))
    }

    io.masterBus.cmd.valid      := io.iBus.cmd.valid || io.dBus.cmd.valid
    io.masterBus.cmd.write      := io.dBus.cmd.valid && io.dBus.cmd.wr
    io.masterBus.cmd.address    := io.dBus.cmd.valid ? io.dBus.cmd.address | io.iBus.cmd.pc
    io.masterBus.cmd.data       := io.dBus.cmd.data
    io.masterBus.cmd.mask       := io.dBus.cmd.size.mux(
            0 -> B"0001",
            1 -> B"0011",
            default -> B"1111"
        ) |<< io.dBus.cmd.address(1 downto 0)

    io.iBus.cmd.ready := io.masterBus.cmd.ready && !io.dBus.cmd.valid
    io.dBus.cmd.ready := io.masterBus.cmd.ready

    val rspPending  = RegInit(False) clearWhen(io.masterBus.rsp.valid)
    val rspTarget   = RegInit(False)
    when(io.masterBus.cmd.fire && !io.masterBus.cmd.write){
        rspTarget  := io.dBus.cmd.valid
        rspPending := True
    }

    when(rspPending && !io.masterBus.rsp.valid){
        io.iBus.cmd.ready := False
        io.dBus.cmd.ready := False
        io.masterBus.cmd.valid := False
    }

    io.iBus.rsp.valid := io.masterBus.rsp.valid && !rspTarget
    io.iBus.rsp.inst  := io.masterBus.rsp.data
    io.iBus.rsp.error := False

    io.dBus.rsp.ready := io.masterBus.rsp.valid && rspTarget
    io.dBus.rsp.data  := io.masterBus.rsp.data
    io.dBus.rsp.error := False
}

case class CoreMarkPipelinedMemoryBusRam(dualBus : Boolean, onChipRamSize : BigInt, onChipRamHexFile : String, pipelinedMemoryBusConfig : PipelinedMemoryBusConfig) extends Component{
    val io = new Bundle{
        val busA  =              slave(PipelinedMemoryBus(pipelinedMemoryBusConfig))
        val busB  = if (dualBus) slave(PipelinedMemoryBus(pipelinedMemoryBusConfig)) else null
    }

    val ram = Mem(Bits(32 bits), onChipRamSize / 4)

    if(onChipRamHexFile != null){
        HexTools.initRam(ram, onChipRamHexFile, 0x00000000l)
    }

    io.busA.rsp.valid   := RegNext(io.busA.cmd.fire && !io.busA.cmd.write) init(False)
    io.busA.rsp.data    := ram.readWriteSync(
        address   = (io.busA.cmd.address >> 2).resized,
        data      = io.busA.cmd.data,
        enable    = io.busA.cmd.valid,
        write     = io.busA.cmd.write,
        mask      = io.busA.cmd.mask
    )
    io.busA.cmd.ready := True

    if (dualBus) {
        io.busB.rsp.valid   := RegNext(io.busB.cmd.fire && !io.busB.cmd.write) init(False)
        io.busB.rsp.data    := ram.readWriteSync(
            address   = (io.busB.cmd.address >> 2).resized,
            data      = io.busB.cmd.data,
            enable    = io.busB.cmd.valid,
            write     = io.busB.cmd.write,
            mask      = io.busB.cmd.mask
        )
        io.busB.cmd.ready := True
    }

}

class CoreMarkPipelinedMemoryBusDecoder(
            master : PipelinedMemoryBus,
            val specification : Seq[(PipelinedMemoryBus,SizeMapping)], pipelineMaster : Boolean
      ) extends Area
{
    val masterPipelined = PipelinedMemoryBus(master.config)
    if(!pipelineMaster) {
        masterPipelined.cmd << master.cmd
        masterPipelined.rsp >> master.rsp
    } else {
        masterPipelined.cmd <-< master.cmd
        masterPipelined.rsp >> master.rsp
    }

    val slaveBuses = specification.map(_._1)
    val memorySpaces = specification.map(_._2)

    val hits = for((slaveBus, memorySpace) <- specification) yield {
        val hit = memorySpace.hit(masterPipelined.cmd.address)
        slaveBus.cmd.valid   := masterPipelined.cmd.valid && hit
        slaveBus.cmd.payload := masterPipelined.cmd.payload.resized
        hit
    }
    val noHit = !hits.orR
    masterPipelined.cmd.ready := (hits,slaveBuses).zipped.map(_ && _.cmd.ready).orR || noHit

    val rspPending  = RegInit(False) clearWhen(masterPipelined.rsp.valid) setWhen(masterPipelined.cmd.fire && !masterPipelined.cmd.write)
    val rspNoHit    = RegNext(False) init(False) setWhen(noHit)
    val rspSourceId = RegNextWhen(OHToUInt(hits), masterPipelined.cmd.fire)
    masterPipelined.rsp.valid   := slaveBuses.map(_.rsp.valid).orR || (rspPending && rspNoHit)
    masterPipelined.rsp.payload := slaveBuses.map(_.rsp.payload).read(rspSourceId)

    when(rspPending && !masterPipelined.rsp.valid) { //Only one pending read request is allowed
        masterPipelined.cmd.ready := False
        slaveBuses.foreach(_.cmd.valid := False)
    }
}

case class CoreMarkTop(config : CoreMarkCpuComplexConfig) extends Component{
    import config._

    val io = new Bundle {
        val asyncReset  = in Bool
        val mainClk     = in Bool

        val apb         = master(Apb3(config.apb3Config))
    }

    val resetCtrlClockDomain = ClockDomain(
        clock = io.mainClk,
        config = ClockDomainConfig( resetKind = BOOT)
    )

    val resetCtrl = new ClockingArea(resetCtrlClockDomain) {
        val mainClkResetUnbuffered  = False

        //Implement an counter to keep the reset axiResetOrder high 64 cycles
        // Also this counter will automatically do a reset when the system boot.
        val systemClkResetCounter = Reg(UInt(6 bits)) init(0)
        when(systemClkResetCounter =/= U(systemClkResetCounter.range -> true)){
            systemClkResetCounter := systemClkResetCounter + 1
            mainClkResetUnbuffered := True
        }
        when(BufferCC(io.asyncReset)){
            systemClkResetCounter := 0
        }

        //Create all reset used later in the design
        val systemReset  = RegNext(mainClkResetUnbuffered)
    }


    val systemClockDomain = ClockDomain(
        clock = io.mainClk,
        reset = resetCtrl.systemReset,
        frequency = FixedFrequency(coreFrequency)
    )

    val system = new ClockingArea(systemClockDomain) {

        val cpuComplex = new CoreMarkCpuComplex(config)
        cpuComplex.io.apb <> io.apb
    }
}
