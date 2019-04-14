#! /usr/bin/env ruby
require 'pp'
require 'open3'
require 'fileutils'

def run_sim(option_str)

    log = []

    total_ticks = -1

    Open3.popen3("sbt \"test:runMain coremark.CoreMarkSim #{option_str}\"") do |stdout, stderr, status, thread|

        while line = stderr.gets do
            puts line
            log << line

            if line =~ /Total ticks\s*:\s*(\d+)/
                total_ticks = $1
            end
        end
    end

    total_ticks
end

def run_syn(option_str)

if 1
    FileUtils.rm_f("./CoreMarkTop.v")
    log = []
    Open3.popen3("sbt \"test:runMain coremark.CoreMarkSim --synth #{option_str}\"") do |stdout, stderr, status, thread|
        while line = stderr.gets do
            puts line
            log << line

            if line =~ /Total ticks\s*:\s*(\d+)/
                total_ticks = $1
            end
        end
    end

    log = []
    Open3.popen3("cd ./quartus && ./run_quartus.sh") do |stdout, stderr, status, thread|
        while line = stderr.gets do
            puts line
            log << line
        end
    end
end

    fit_file = File.open("./quartus/output_files/VexRiscv.fit.rpt")
    resource_line = nil
    fit_file.readlines.each do |l|
        if l.scrub =~ / \|VexRiscv:cpu\|/
            resource_line = l
        end
    end
    unless resource_line
        return nil
    end
    #puts resource_line

    fields = resource_line.split(";").collect{ |f| f.strip }
    result = {
        "ALMs"      => fields[3].to_i,
        "ALUTs"     => fields[7].to_i,
        "FFs"       => fields[8].to_f,
        "RAMs"      => fields[11].to_i,
    }

    sta_file = File.open("./quartus/output_files/VexRiscv.sta.rpt")
    mhz_line = nil
    sta_file.readlines.each do |l|
        if l.scrub =~ /MHz/
            mhz_line = l
        end
    end
    unless mhz_line
        return nil
    end
    fields = mhz_line.split(";").collect{ |f| f.strip }
    result["MHz"] = fields[1]

    pp result
    result

end


PIPE_EMW    = 0
PIPE_EM     = 1
PIPE_E      = 2

BP_NONE     = 0
BP_STATIC   = 1
BP_DYN      = 2
BP_DYN_TGT  = 3

OPT_Os      = 0
OPT_O2      = 1
OPT_O3      = 2

MUL_NONE    = 0
MUL_ITER    = 1
MUL_SIMPLE  = 2

DIV_NONE    = 0
DIV_ITER    = 1
DIV_DHRY    = 2

SHIFT_ITER      = 0
SHIFT_BAR_EXE   = 1
SHIFT_BAR_MEM   = 2

pipe5_medium_config = {
        "--Pipe"    => PIPE_EMW,
        "--BrE"     => 0,
        "--BP"      => BP_STATIC,
        "--Shft"    => SHIFT_BAR_EXE,
        "--Mul"     => MUL_NONE,
        "--Div"     => DIV_NONE,
    }
pipe5_medium_config_str = pipe5_medium_config.collect { |opt, val| "#{opt}=#{val}" }.join(" ")

pipe4_medium_config = {
        "--Pipe"    => PIPE_EM,
        "--BrE"     => 1,
        "--BP"      => BP_STATIC,
        "--Shft"    => SHIFT_BAR_EXE,
        "--Mul"     => MUL_NONE,
        "--Div"     => DIV_NONE,
    }
pipe4_medium_config_str = pipe4_medium_config.collect { |opt, val| "#{opt}=#{val}" }.join(" ")

pipe3_medium_config = {
        "--Pipe"    => PIPE_E,
        "--BrE"     => 1,
        "--BP"      => BP_STATIC,
        "--Shft"    => SHIFT_BAR_EXE,
        "--Mul"     => MUL_NONE,
        "--Div"     => DIV_NONE,
    }
pipe3_medium_config_str = pipe3_medium_config.collect { |opt, val| "#{opt}=#{val}" }.join(" ")


all_tests = [
    { "bypass_impact_tests" =>  [
        { "5-stage, no bypass"  => "--BypE=0 --BypM=0 --BypW=0 --BypWB=0 #{pipe5_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
        { "5-stage, bypassE"    => "--BypE=1 --BypM=0 --BypW=0 --BypWB=0 #{pipe5_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
        { "5-stage, bypassM"    => "--BypE=0 --BypM=1 --BypW=0 --BypWB=0 #{pipe5_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
        { "5-stage, bypassW"    => "--BypE=0 --BypM=0 --BypW=1 --BypWB=0 #{pipe5_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
        { "5-stage, bypassWB"   => "--BypE=0 --BypM=0 --BypW=0 --BypWB=1 #{pipe5_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
        { "5-stage, bypassAll"  => "--BypE=1 --BypM=1 --BypW=1 --BypWB=1 #{pipe5_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },

        { "4-stage, no bypass"  => "--BypE=0 --BypM=0 --BypW=0 --BypWB=0 #{pipe4_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
        { "4-stage, bypassE"    => "--BypE=1 --BypM=0 --BypW=0 --BypWB=0 #{pipe4_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
        { "4-stage, bypassM"    => "--BypE=0 --BypM=1 --BypW=0 --BypWB=0 #{pipe4_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
        { "4-stage, bypassWB"   => "--BypE=0 --BypM=0 --BypW=0 --BypWB=1 #{pipe4_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
        { "4-stage, bypassAll"  => "--BypE=1 --BypM=1 --BypW=1 --BypWB=1 #{pipe4_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },

        { "3-stage, no bypass"  => "--BypE=0 --BypM=0 --BypW=0 --BypWB=0 #{pipe3_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
        { "3-stage, bypassE"    => "--BypE=1 --BypM=0 --BypW=0 --BypWB=0 #{pipe3_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
        { "3-stage, bypassWB"   => "--BypE=0 --BypM=0 --BypW=0 --BypWB=1 #{pipe3_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
        { "3-stage, bypassAll"  => "--BypE=1 --BypM=1 --BypW=1 --BypWB=1 #{pipe3_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
    ] },
]

all_tests.each do |test_set|
    test_set_name = test_set.keys.first

    puts "============================================================"
    puts test_set_name
    puts "============================================================"


    test_set[test_set_name].each do |config, config_str|
        config_name = config.keys.first
        config_option_str = config[config_name]

        puts config_name

        result = run_syn(config_option_str)
        total_ticks = run_sim(config_option_str)
        puts "%%%%%% #{config_name} : #{total_ticks} : #{config_option_str}"
        result["total_ticks"] = total_ticks

        config[:result] = result
    end
end

pp all_tests

