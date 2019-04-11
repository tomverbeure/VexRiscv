
all: 
	sbt "test:runMain coremark.CoreMarkSim --BypE=1 --BypM=1 --BypW=1 --BypWB=1 --BrE=0 --Pipe=0 --BP=0 --Opt=1 --Mul=1 --Div=1 --Gcc=1"

waves: 
	gtkwave simWorkspace/CoreMarkTop/test.vcd
