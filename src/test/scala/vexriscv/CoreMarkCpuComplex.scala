package coremark

import spinal.core._
import spinal.lib._
import spinal.lib.misc.HexTools
import spinal.lib.bus.amba3.apb._
import spinal.lib.bus.misc.SizeMapping
import spinal.lib.bus.simple._

import scala.collection.mutable._

import java.nio.file.{Paths,Files}

import vexriscv.plugin.{NONE, _}
import vexriscv.{VexRiscv, VexRiscvConfig, plugin}
import vexriscv.demo._

object PipelineOption extends Enumeration {
    type PipelineOption = Value
    val ExeMemWb        = Value(0)
    val ExeMem          = Value(1)
    val Exe             = Value(2)
}

object MultiplyOption extends Enumeration {
    type MultiplyOption = Value
    val None            = Value(0)
    val Iterative       = Value(1)
    val Simple          = Value(2)
}

object DivideOption extends Enumeration {
    type DivideOption = Value
    val None            = Value(0)
    val Iterative       = Value(1)
    val IterativeDhry   = Value(2)
}

object ShifterOption extends Enumeration {
    type ShifterOption = Value
    val Iterative       = Value(0)
    val BarrelExe       = Value(1)
    val BarrelMem       = Value(2)
}

object PredictionOption extends Enumeration {
    type PredictionOption = Value
    val None            = Value(0)
    val Static          = Value(1)
    val Dynamic         = Value(2)
    val DynamicTarget   = Value(3)
}

object OptimizationOption extends Enumeration {
    type OptimizationOption = Value
    val Os              = Value(0)
    val O2              = Value(1)
    val O3              = Value(2)
}

object GccVersionOption extends Enumeration {
    type GccVersionOption = Value
    val Ver7_2_0        = Value(0)
    val Ver8_2_0        = Value(1)
}

import PipelineOption.PipelineOption
import MultiplyOption.MultiplyOption
import DivideOption.DivideOption
import ShifterOption.ShifterOption
import PredictionOption.PredictionOption
import OptimizationOption.OptimizationOption
import GccVersionOption.GccVersionOption

case class CoreMarkParameters(
                pipeline                  : PipelineOption      = PipelineOption.ExeMemWb,
                bypassExecute             : Boolean             = false,
                bypassMemory              : Boolean             = false,
                bypassWriteBack           : Boolean             = false,
                bypassWriteBackBuffer     : Boolean             = false,
                compressed                : Boolean             = false,
                branchEarly               : Boolean             = false,
                multiply                  : MultiplyOption      = MultiplyOption.None,
                divide                    : DivideOption        = DivideOption.None,
                shifter                   : ShifterOption       = ShifterOption.Iterative,
                prediction                : PredictionOption    = PredictionOption.None,
                mergeIBusDBus             : Boolean             = false,
                optimization              : OptimizationOption  = OptimizationOption.O2,
                gccVersion                : GccVersionOption    = GccVersionOption.Ver7_2_0
		)
{

    def withArgs(args : Seq[String]) = {
        var pipeline                = this.pipeline
        var bypassExecute           = this.bypassExecute
        var bypassMemory            = this.bypassMemory
        var bypassWriteBack         = this.bypassWriteBack
        var bypassWriteBackBuffer   = this.bypassWriteBackBuffer
        var compressed              = this.compressed
        var branchEarly             = this.branchEarly
        var multiply                = this.multiply
        var divide                  = this.divide
        var shifter                 = this.shifter
        var prediction              = this.prediction
        var mergeIBusDBus           = this.mergeIBusDBus
        var optimization            = this.optimization
        var gccVersion              = this.gccVersion

        for(arg <- args.toList){
            val opt_val = arg.split("=").map(_.trim)

            opt_val(0) match {
                case "--Pipe"       => pipeline                 = PipelineOption(opt_val(1).toInt)
                case "--BypE"       => bypassExecute            = (opt_val(1) == "1")
                case "--BypM"       => bypassMemory             = (opt_val(1) == "1")
                case "--BypW"       => bypassWriteBack          = (opt_val(1) == "1")
                case "--BypWB"      => bypassWriteBackBuffer    = (opt_val(1) == "1")
                case "--C"          => compressed               = (opt_val(1) == "1")
                case "--BrE"        => branchEarly              = (opt_val(1) == "1")
                case "--Mul"        => multiply                 = MultiplyOption(opt_val(1).toInt)
                case "--Div"        => divide                   = DivideOption(opt_val(1).toInt)
                case "--Shft"       => shifter                  = ShifterOption(opt_val(1).toInt)
                case "--BP"         => prediction               = PredictionOption(opt_val(1).toInt)
                case "--MergeIBDB"  => mergeIBusDBus            = (opt_val(1) == "1")
                case "--Opt"        => optimization             = OptimizationOption(opt_val(1).toInt)
                case "--Gcc"        => gccVersion               = GccVersionOption(opt_val(1).toInt)
                case "--synth"      =>
            }
        }

        this.copy(
            pipeline                = pipeline,
            bypassExecute           = bypassExecute,
            bypassMemory            = bypassMemory,
            bypassWriteBack         = bypassWriteBack,
            bypassWriteBackBuffer   = bypassWriteBackBuffer,
            compressed              = compressed,
            branchEarly             = branchEarly,
            multiply                = multiply,
            divide                  = divide,
            shifter                 = shifter,
            prediction              = prediction,
            mergeIBusDBus           = mergeIBusDBus,
            optimization            = optimization,
            gccVersion              = gccVersion
        )
    }

    def toCoreMarkCpuComplexConfig() = {

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

        val gccVersionStr = gccVersion match {
                                case GccVersionOption.Ver7_2_0      => "7.2.0"
                                case GccVersionOption.Ver8_2_0      => "8.2.0"
                            }
        val optimizationStr = optimization match {
                                case OptimizationOption.Os          => "Os"
                                case OptimizationOption.O2          => "O2"
                                case OptimizationOption.O3          => "O3"
                            }
        val compressedStr = if (compressed) "c" else ""
        val multiplyStr = if (multiply == MultiplyOption.None) "" else "m"

        val hexFilename = s"src/test/cpp/coremark/${gccVersionStr}/coremark_${optimizationStr}_rv32i${multiplyStr}${compressedStr}.hex"
        assert(Files.exists(Paths.get(hexFilename)), s"File doesn't exist: ${hexFilename}")

        val config = CoreMarkCpuComplexConfig(
            onChipRamHexFile            = hexFilename,
            coreFrequency               = 100 MHz,
            mergeIBusDBus               = mergeIBusDBus,
            iBusLatency                 = 1,
            dBusLatency                 = 1,
			memoryStage                 = pipeline match {
                                            case PipelineOption.ExeMemWb      => true
                                            case PipelineOption.ExeMem        => true
                                            case PipelineOption.Exe           => false
                                          },
			writeBackStage              = pipeline match {
                                            case PipelineOption.ExeMemWb      => true
                                            case PipelineOption.ExeMem        => false
                                            case PipelineOption.Exe           => false
                                          },
            apb3Config = Apb3Config(
                addressWidth = 20,
                dataWidth = 32
            ),
            cpuPlugins = ArrayBuffer(
                new IBusSimplePlugin(
                    resetVector             = 0x00000000l,
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
                    zeroBoot                = false,
                    readInExecute           = false
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
                    earlyBranch             = branchEarly,
                    catchAddressMisaligned  = false
                ),
                new CsrPlugin(ucycleCsrConfig),
                new YamlPlugin("cpu0.yaml")
            ).filter(_ != null)
        )

        config
    }

    def toShortStr(assignChar : Char = '=', separatorChar : Char = ' ') : String = {

        val options = ListBuffer[String]()

        options += s"Pipe${ assignChar }${ pipeline.toString }"
        options += s"BypE${ assignChar }${ bypassExecute.compare(false)}"
        options += s"BypM${ assignChar }${ bypassMemory.compare(false)}"
        options += s"BypW${ assignChar }${ bypassWriteBack.compare(false) }"
        options += s"BypWB${ assignChar }${ bypassWriteBackBuffer.compare(false) }"
        options += s"C${ assignChar }${ compressed.compare(false) }"
        options += s"BrE${ assignChar }${ branchEarly.compare(false) }"
        options += s"Mul${ assignChar }${ multiply.toString }"
        options += s"Div${ assignChar }${ divide.toString }"
        options += s"Shft${ assignChar }${ shifter.toString }"
        options += s"BP${ assignChar }${ prediction.toString }"
        options += s"MergeIBDB${ assignChar }${ mergeIBusDBus.compare(false) }"
        options += s"Opt${ assignChar }${ optimization.toString }"
        options += s"Gcc${ assignChar }${ gccVersion.toString }"

        val str = options.mkString(separatorChar.toString)

        str
    }

    def toLongStr : String = {
        val options = ListBuffer[String]()

        options += s"Pipeline            : ${ pipeline.toString }"
        options += s"BypassExecute       : ${ bypassExecute.compare(false)}"
        options += s"BypMemory           : ${ bypassMemory.compare(false)}"
        options += s"BypWriteBack        : ${ bypassWriteBack.compare(false) }"
        options += s"BypWwriteBackBuffer : ${ bypassWriteBackBuffer.compare(false) }"
        options += s"Compressed          : ${ compressed.compare(false) }"
        options += s"BranchEarly         : ${ branchEarly.compare(false) }"
        options += s"Multiply            : ${ multiply.toString }"
        options += s"Divide              : ${ divide.toString }"
        options += s"Shifter             : ${ shifter.toString }"
        options += s"BranchPrediction    : ${ prediction.toString }"
        options += s"MergeIBusDBus       : ${ mergeIBusDBus.compare(false) }"
        options += s"Optimization        : ${ optimization.toString }"
        options += s"GccVersion          : ${ gccVersion.toString }"

        val str = options.mkString("\n")

        str
    }

}


case class CoreMarkCpuComplexConfig(
                  onChipRamHexFile  	: String,
                  coreFrequency     	: HertzNumber,
                  mergeIBusDBus     	: Boolean,
                  iBusLatency       	: Int,
                  dBusLatency       	: Int,
                  apb3Config        	: Apb3Config,
				  memoryStage			: Boolean,
				  writeBackStage		: Boolean,
                  cpuPlugins        	: ArrayBuffer[Plugin[VexRiscv]])
{
    require(iBusLatency >=1, "iBusLatency must be >= 1")
    require(dBusLatency >=1, "dBusLatency must be >= 1")
}

case class CoreMarkCpuComplex(config : CoreMarkCpuComplexConfig, synth : Boolean = false) extends Component
{
    val io = new Bundle {
        val apb     = master(Apb3(config.apb3Config))
    }

    val pipelinedMemoryBusConfig = PipelinedMemoryBusConfig(
        addressWidth 	= 32,
        dataWidth 		= 32
    )

    val cpu = new VexRiscv(
        config = VexRiscvConfig(
			withMemoryStage 		= config.memoryStage,
			withWriteBackStage 		= config.writeBackStage,
            plugins 				= config.cpuPlugins
        )
    )

    val pipelineDBus    = false
    val pipelineMainBus = false

    // The CPU has 2 busses: iBus and dBus.
    // When mergeIBusDBus is true, when those 2 busses get merged first into the mainBus through the
    // MasterArbiter, which then goes to everything as the mainBus.
    // When mergIBusDBus is false, the iBus goes directly busB of the RAM and nothing else. Mainwhile dBus
    // get connected directly to mainBus, thus no MasterArbiter is needed. The RAM needs to be double-ported,
    // but that's often the case of FPGAs anyway, or a good approximation for systems that have individual
    // I-cache and D-cache.

    val mainBusArbiter = CoreMarkMasterArbiter(pipelinedMemoryBusConfig)

    val onChipRamSize = 32 KiB

    val ram = new CoreMarkPipelinedMemoryBusRam(
        dualBus                   = !config.mergeIBusDBus,
        onChipRamSize             = onChipRamSize,
        onChipRamHexFile          = config.onChipRamHexFile,
        pipelinedMemoryBusConfig  = pipelinedMemoryBusConfig,
        synth                     = synth
    )

    // Checkout plugins used to instanciate the CPU to connect them to the SoC
    for(plugin <- cpu.plugins) plugin match{
        case plugin : IBusSimplePlugin =>
            if (config.mergeIBusDBus)
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

case class CoreMarkPipelinedMemoryBusRam(dualBus : Boolean, onChipRamSize : BigInt, onChipRamHexFile : String, pipelinedMemoryBusConfig : PipelinedMemoryBusConfig, synth : Boolean = false) extends Component{
    val io = new Bundle{
        val busA  =              slave(PipelinedMemoryBus(pipelinedMemoryBusConfig))
        val busB  = if (dualBus) slave(PipelinedMemoryBus(pipelinedMemoryBusConfig)) else null
    }

    if (!synth){

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
    else{
		val ram = new dpram_8kx32_p1p1
		ram.io.address_a			:= (io.busA.cmd.address >> 2).resized
		ram.io.wren_a				:= False
		ram.io.byteena_a			:= 0
		ram.io.data_a				:= 0
		ram.io.q_a					<> io.busA.rsp.data

        io.busA.cmd.ready := True
		io.busA.rsp.valid := RegNext(io.busA.cmd.fire && !io.busA.cmd.write) init(False)

		ram.io.address_b			:= (io.busB.cmd.address >> 2).resized
		ram.io.wren_b				:= io.busB.cmd.valid & io.busB.cmd.write
		ram.io.byteena_b			:= io.busB.cmd.mask
		ram.io.data_b				:= io.busB.cmd.data
		ram.io.q_b					<> io.busB.rsp.data

        io.busB.cmd.ready := True
		io.busB.rsp.valid := RegNext(io.busB.cmd.fire && !io.busB.cmd.write) init(False)

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

case class CoreMarkTop(config : CoreMarkCpuComplexConfig, synth : Boolean = false) extends Component{
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

        val cpuComplex = new CoreMarkCpuComplex(config = config, synth = synth)
        cpuComplex.io.apb <> io.apb
    }
}

class dpram_8kx32_p1p1 extends BlackBox {

    val io = new Bundle {
        val clock           = in(Bool)
        val address_a       = in(UInt(14 bits))
        val wren_a          = in(Bool)
        val byteena_a       = in(Bits(4 bits))
        val data_a          = in(Bits(32 bits))
        val q_a             = out(Bits(32 bits))

        val address_b       = in(UInt(16 bits))
        val wren_b          = in(Bool)
        val byteena_b       = in(Bits(4 bits))
        val data_b          = in(Bits(32 bits))
        val q_b             = out(Bits(32 bits))
    }

    mapCurrentClockDomain(io.clock)
    noIoPrefix()

    addRTLPath("./primitives/cyclone2/dpram_8kx32_p1p1/dpram_8kx32_p1p1.v")
}

class dpram_8kx32_p2p1 extends BlackBox {

    val io = new Bundle {
        val clock           = in(Bool)
        val address_a       = in(UInt(14 bits))
        val wren_a          = in(Bool)
        val byteena_a       = in(Bits(4 bits))
        val data_a          = in(Bits(32 bits))
        val q_a             = out(Bits(32 bits))

        val address_b       = in(UInt(16 bits))
        val wren_b          = in(Bool)
        val byteena_b       = in(Bits(4 bits))
        val data_b          = in(Bits(32 bits))
        val q_b             = out(Bits(32 bits))
    }

    mapCurrentClockDomain(io.clock)
    noIoPrefix()

    addRTLPath("./primitives/cyclone2/dpram_8kx32_p2p1/dpram_8kx32_p2p1.v")
}

class dpram_8kx32_p1p2 extends BlackBox {

    val io = new Bundle {
        val clock           = in(Bool)
        val address_a       = in(UInt(14 bits))
        val wren_a          = in(Bool)
        val byteena_a       = in(Bits(4 bits))
        val data_a          = in(Bits(32 bits))
        val q_a             = out(Bits(32 bits))

        val address_b       = in(UInt(16 bits))
        val wren_b          = in(Bool)
        val byteena_b       = in(Bits(4 bits))
        val data_b          = in(Bits(32 bits))
        val q_b             = out(Bits(32 bits))
    }

    mapCurrentClockDomain(io.clock)
    noIoPrefix()

    addRTLPath("./primitives/cyclone2/dpram_8kx32_p2p1/dpram_8kx32_p1p2.v")
}


