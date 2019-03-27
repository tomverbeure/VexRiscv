#sbt "test:runMain vexriscv.MuraxSim"

#sbt "runMain vexriscv.demo.GenFull"

#sbt "runMain vexriscv.demo.GenSmallest"

#sbt "runMain vexriscv.demo.GenFullNoMmuNoCacheSimpleMul"

sbt "test:runMain coremark.CoreMarkSim"
