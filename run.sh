#sbt "test:runMain vexriscv.MuraxSim"

#sbt "runMain vexriscv.demo.GenFull"

#sbt "runMain vexriscv.demo.GenSmallest"

#sbt "runMain vexriscv.demo.GenFullNoMmuNoCacheSimpleMul"

sbt "test:runMain coremark.CoreMarkSim --BypE=1 --BypM=1 --BypW=1 --BypWB=1 --BrE=0 --Pipe=0 --BP=1 --Opt=1 --Mul=1 --Div=1 --Gcc=1"
