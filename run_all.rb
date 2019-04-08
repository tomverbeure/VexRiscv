#! /usr/bin/env ruby

require 'open3'


configs = "--BypE=0 --BypM=0 --BypW=0 --BypWB=0 --BrE=0 --Pipe=0 --BP=0 --Opt=2 --Mul=1 --Div=1"

Open3.popen3("sbt \"test:runMain coremark.CoreMarkSim #{configs}\"") do |stdout, stderr, status, thread|

    while line = stderr.gets do
        puts line
    end

#    while line = stdout.gets do
#        puts line
#    end
#
#    puts stderr.read
#    puts stdout.read

end


