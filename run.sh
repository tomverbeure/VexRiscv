#sbt "test:runMain vexriscv.MuraxSim"

#sbt "runMain vexriscv.demo.GenFull"

#sbt "runMain vexriscv.demo.GenSmallest"

#sbt "runMain vexriscv.demo.GenFullNoMmuNoCacheSimpleMul"

sbt "test:runMain coremark.CoreMarkSim --BypE=0 --BypM=0 --BypW=0 --BypWB=0 --BrE=0 --Pipe=0 --BP=0 --Opt=1 --Mul=1 --Div=1"
